package com.khanh.fooddelivery.catalog_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.catalog_service.dto.request.OptionTemplateBatchCopyRequest;
import com.khanh.fooddelivery.catalog_service.dto.response.OptionGroupResponse;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.entity.OptionGroup;
import com.khanh.fooddelivery.catalog_service.entity.OptionTemplate;
import com.khanh.fooddelivery.catalog_service.entity.OptionTemplateValue;
import com.khanh.fooddelivery.catalog_service.entity.OptionValue;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import com.khanh.fooddelivery.catalog_service.enums.CatalogItemType;
import com.khanh.fooddelivery.catalog_service.enums.OptionSelectionType;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.mapper.OptionGroupMapper;
import com.khanh.fooddelivery.catalog_service.outbox.OutboxEventService;
import com.khanh.fooddelivery.catalog_service.repository.CatalogItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.OptionGroupRepository;
import com.khanh.fooddelivery.catalog_service.repository.OptionTemplateRepository;
import com.khanh.fooddelivery.catalog_service.repository.OptionTemplateValueRepository;
import com.khanh.fooddelivery.catalog_service.repository.OptionValueRepository;
import com.khanh.fooddelivery.catalog_service.service.CatalogAuthorizationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OptionTemplateServiceImplTests {
    private static final UUID RESTAURANT_ID = UUID.randomUUID();
    private static final UUID TEMPLATE_ID = UUID.randomUUID();

    @Mock private OptionTemplateRepository templateRepository;
    @Mock private OptionTemplateValueRepository templateValueRepository;
    @Mock private CatalogItemRepository itemRepository;
    @Mock private OptionGroupRepository groupRepository;
    @Mock private OptionValueRepository valueRepository;
    @Mock private OptionGroupMapper groupMapper;
    @Mock private CatalogAuthorizationService authorizationService;
    @Mock private OutboxEventService outboxEventService;
    @InjectMocks private OptionTemplateServiceImpl service;

    @Test
    void copyToItemCreatesIndependentGroupAndValues() {
        OptionTemplate template = new OptionTemplate();
        template.setId(TEMPLATE_ID);
        template.setRestaurantId(RESTAURANT_ID);
        template.setName("Size");
        template.setSelectionType(OptionSelectionType.SINGLE);
        template.setMinimumSelections(0);
        template.setMaximumSelections(1);
        template.setStatus(CatalogStatus.ACTIVE);

        CatalogItem item = new CatalogItem();
        item.setId(UUID.randomUUID());
        item.setRestaurantId(RESTAURANT_ID);
        item.setName("Milk tea");
        item.setItemType(CatalogItemType.DRINK);

        OptionTemplateValue first = templateValue(template, "S", BigDecimal.ZERO);
        OptionTemplateValue second = templateValue(template, "M", BigDecimal.valueOf(5000));
        when(templateRepository.findByIdAndRestaurantId(TEMPLATE_ID, RESTAURANT_ID))
                .thenReturn(Optional.of(template));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(templateValueRepository.findAllByTemplateIdInOrderBySortOrderAsc(List.of(TEMPLATE_ID)))
                .thenReturn(List.of(first, second));
        when(groupRepository.save(any(OptionGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AtomicReference<List<OptionValue>> savedValues = new AtomicReference<>();
        doAnswer(invocation -> {
            savedValues.set(invocation.getArgument(0));
            return invocation.getArgument(0);
        }).when(valueRepository).saveAll(any());
        when(groupMapper.toResponse(any(OptionGroup.class))).thenReturn(null);

        service.copyToItem(RESTAURANT_ID, TEMPLATE_ID, item.getId());

        ArgumentCaptor<OptionGroup> groupCaptor = ArgumentCaptor.forClass(OptionGroup.class);
        verify(groupRepository).save(groupCaptor.capture());
        OptionGroup copiedGroup = groupCaptor.getValue();
        assertThat(copiedGroup.getId()).isNull();
        assertThat(copiedGroup.getSourceTemplateId()).isEqualTo(TEMPLATE_ID);
        assertThat(copiedGroup.getItem()).isSameAs(item);

        assertThat(savedValues.get()).hasSize(2).allSatisfy(value -> {
            assertThat(value.getId()).isNull();
            assertThat(value.getOptionGroup()).isSameAs(copiedGroup);
        });
    }

    @Test
    void copyToItemBatchCreatesIndependentGroupsForEveryActiveTemplate() {
        UUID toppingTemplateId = UUID.randomUUID();
        OptionTemplate size = template(TEMPLATE_ID, "Size");
        OptionTemplate topping = template(toppingTemplateId, "Topping");
        CatalogItem item = new CatalogItem();
        item.setId(UUID.randomUUID());
        item.setRestaurantId(RESTAURANT_ID);
        item.setItemType(CatalogItemType.DRINK);
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(templateRepository.findByIdAndRestaurantId(TEMPLATE_ID, RESTAURANT_ID)).thenReturn(Optional.of(size));
        when(templateRepository.findByIdAndRestaurantId(toppingTemplateId, RESTAURANT_ID)).thenReturn(Optional.of(topping));
        when(templateValueRepository.findAllByTemplateIdInOrderBySortOrderAsc(List.of(TEMPLATE_ID, toppingTemplateId)))
                .thenReturn(List.of(templateValue(size, "M", BigDecimal.valueOf(5000)), templateValue(topping, "Pearl", BigDecimal.valueOf(5000))));
        when(groupRepository.save(any(OptionGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(valueRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(groupMapper.toResponse(any(OptionGroup.class))).thenReturn(null);

        service.copyToItemBatch(RESTAURANT_ID, item.getId(), new OptionTemplateBatchCopyRequest(List.of(TEMPLATE_ID, toppingTemplateId)));

        verify(groupRepository, times(2)).save(any(OptionGroup.class));
        verify(valueRepository, times(2)).saveAll(any());
    }

    @Test
    void copyToItemBatchRejectsInactiveTemplateBeforeSavingAnyGroup() {
        OptionTemplate inactive = template(TEMPLATE_ID, "Size");
        inactive.setStatus(CatalogStatus.INACTIVE);
        CatalogItem item = new CatalogItem();
        item.setId(UUID.randomUUID());
        item.setRestaurantId(RESTAURANT_ID);
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(templateRepository.findByIdAndRestaurantId(TEMPLATE_ID, RESTAURANT_ID)).thenReturn(Optional.of(inactive));

        AppException error = assertThrows(AppException.class, () -> service.copyToItemBatch(
                RESTAURANT_ID, item.getId(), new OptionTemplateBatchCopyRequest(List.of(TEMPLATE_ID))));

        assertThat(error.getErrorCode()).isEqualTo(ErrorCode.OPTION_TEMPLATE_INACTIVE);
        verify(groupRepository, never()).save(any());
    }

    @Test
    void copyToItemBatchRejectsAnExistingActiveGroupBeforeCopying() {
        OptionTemplate template = template(TEMPLATE_ID, "Size");
        CatalogItem item = new CatalogItem();
        item.setId(UUID.randomUUID());
        item.setRestaurantId(RESTAURANT_ID);
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(templateRepository.findByIdAndRestaurantId(TEMPLATE_ID, RESTAURANT_ID)).thenReturn(Optional.of(template));
        when(groupRepository.existsByItemIdAndNameAndStatus(item.getId(), "Size", CatalogStatus.ACTIVE)).thenReturn(true);

        AppException error = assertThrows(AppException.class, () -> service.copyToItemBatch(
                RESTAURANT_ID, item.getId(), new OptionTemplateBatchCopyRequest(List.of(TEMPLATE_ID))));

        assertThat(error.getErrorCode()).isEqualTo(ErrorCode.DATA_CONFLICT);
        verify(groupRepository, never()).save(any());
    }

    private OptionTemplate template(UUID id, String name) {
        OptionTemplate template = new OptionTemplate();
        template.setId(id);
        template.setRestaurantId(RESTAURANT_ID);
        template.setName(name);
        template.setSelectionType(OptionSelectionType.SINGLE);
        template.setMinimumSelections(0);
        template.setMaximumSelections(1);
        template.setStatus(CatalogStatus.ACTIVE);
        return template;
    }

    private OptionTemplateValue templateValue(OptionTemplate template, String name, BigDecimal price) {
        OptionTemplateValue value = new OptionTemplateValue();
        value.setTemplate(template);
        value.setName(name);
        value.setAdditionalPrice(price);
        value.setIsAvailable(true);
        return value;
    }
}
