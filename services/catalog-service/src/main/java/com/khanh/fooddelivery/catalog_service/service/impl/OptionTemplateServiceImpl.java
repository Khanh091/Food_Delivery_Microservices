package com.khanh.fooddelivery.catalog_service.service.impl;

import com.khanh.fooddelivery.catalog_service.dto.request.OptionTemplateUpsertRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.OptionTemplateBatchCopyRequest;
import com.khanh.fooddelivery.catalog_service.dto.request.OptionTemplateValueInput;
import com.khanh.fooddelivery.catalog_service.dto.response.OptionGroupResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.OptionTemplatePageResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.OptionTemplateResponse;
import com.khanh.fooddelivery.catalog_service.dto.response.OptionTemplateValueResponse;
import com.khanh.fooddelivery.catalog_service.entity.CatalogItem;
import com.khanh.fooddelivery.catalog_service.entity.OptionGroup;
import com.khanh.fooddelivery.catalog_service.entity.OptionTemplate;
import com.khanh.fooddelivery.catalog_service.entity.OptionTemplateValue;
import com.khanh.fooddelivery.catalog_service.entity.OptionValue;
import com.khanh.fooddelivery.catalog_service.enums.CatalogStatus;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;
import com.khanh.fooddelivery.catalog_service.mapper.OptionGroupMapper;
import com.khanh.fooddelivery.catalog_service.outbox.CatalogEventData;
import com.khanh.fooddelivery.catalog_service.outbox.CatalogEventType;
import com.khanh.fooddelivery.catalog_service.outbox.OutboxEventService;
import com.khanh.fooddelivery.catalog_service.repository.CatalogItemRepository;
import com.khanh.fooddelivery.catalog_service.repository.OptionGroupRepository;
import com.khanh.fooddelivery.catalog_service.repository.OptionTemplateRepository;
import com.khanh.fooddelivery.catalog_service.repository.OptionTemplateValueRepository;
import com.khanh.fooddelivery.catalog_service.repository.OptionValueRepository;
import com.khanh.fooddelivery.catalog_service.service.CatalogAuthorizationService;
import com.khanh.fooddelivery.catalog_service.service.OptionTemplateService;
import com.khanh.fooddelivery.catalog_service.validation.OptionSelectionRules;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OptionTemplateServiceImpl implements OptionTemplateService {
    private final OptionTemplateRepository templateRepository;
    private final OptionTemplateValueRepository templateValueRepository;
    private final CatalogItemRepository itemRepository;
    private final OptionGroupRepository groupRepository;
    private final OptionValueRepository valueRepository;
    private final OptionGroupMapper groupMapper;
    private final CatalogAuthorizationService authorizationService;
    private final OutboxEventService outboxEventService;

    @Override
    @Transactional(readOnly = true)
    public OptionTemplatePageResponse list(UUID restaurantId, String query, int page, int size) {
        authorize(restaurantId);
        Page<OptionTemplate> result = templateRepository.searchByRestaurantId(
                restaurantId, normalizeQuery(query), PageRequest.of(page, size));
        Map<UUID, List<OptionTemplateValue>> values = valuesByTemplate(result.getContent());
        return new OptionTemplatePageResponse(
                result.getContent().stream().map(template -> response(template, values.get(template.getId()))).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public OptionTemplateResponse get(UUID restaurantId, UUID templateId) {
        authorize(restaurantId);
        OptionTemplate template = requiredTemplate(restaurantId, templateId);
        return response(template, templateValueRepository.findAllByTemplateIdInOrderBySortOrderAsc(List.of(templateId)));
    }

    @Override
    public OptionTemplateResponse create(UUID restaurantId, OptionTemplateUpsertRequest request) {
        authorize(restaurantId);
        OptionTemplate template = new OptionTemplate();
        template.setRestaurantId(restaurantId);
        apply(request, template);
        template.setStatus(CatalogStatus.ACTIVE);
        OptionTemplate saved = templateRepository.save(template);
        List<OptionTemplateValue> values = replaceValues(saved, request.values());
        return response(saved, values);
    }

    @Override
    public OptionTemplateResponse update(UUID restaurantId, UUID templateId, OptionTemplateUpsertRequest request) {
        authorize(restaurantId);
        OptionTemplate template = requiredTemplate(restaurantId, templateId);
        apply(request, template);
        OptionTemplate saved = templateRepository.save(template);
        templateValueRepository.deleteByTemplateId(templateId);
        templateValueRepository.flush();
        List<OptionTemplateValue> values = replaceValues(saved, request.values());
        return response(saved, values);
    }

    @Override
    public OptionTemplateResponse activate(UUID restaurantId, UUID templateId) {
        return changeStatus(restaurantId, templateId, CatalogStatus.ACTIVE);
    }

    @Override
    public OptionTemplateResponse deactivate(UUID restaurantId, UUID templateId) {
        return changeStatus(restaurantId, templateId, CatalogStatus.INACTIVE);
    }

    @Override
    public OptionGroupResponse copyToItem(UUID restaurantId, UUID templateId, UUID itemId) {
        authorize(restaurantId);
        OptionTemplate template = requiredTemplate(restaurantId, templateId);
        if (template.getStatus() != CatalogStatus.ACTIVE) {
            throw new AppException(ErrorCode.OPTION_TEMPLATE_INACTIVE);
        }
        CatalogItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.CATALOG_ITEM_NOT_FOUND));
        if (!restaurantId.equals(item.getRestaurantId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        return copyTemplateToItem(template, item,
                templateValueRepository.findAllByTemplateIdInOrderBySortOrderAsc(List.of(templateId)));
    }

    @Override
    public List<OptionGroupResponse> copyToItemBatch(
            UUID restaurantId, UUID itemId, OptionTemplateBatchCopyRequest request) {
        authorize(restaurantId);
        List<UUID> templateIds = request.templateIds();
        if (new LinkedHashSet<>(templateIds).size() != templateIds.size()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Option template IDs must be unique");
        }
        CatalogItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.CATALOG_ITEM_NOT_FOUND));
        if (!restaurantId.equals(item.getRestaurantId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        List<OptionTemplate> templates = templateIds.stream()
                .map(templateId -> requiredTemplate(restaurantId, templateId))
                .toList();
        if (templates.stream().anyMatch(template -> template.getStatus() != CatalogStatus.ACTIVE)) {
            throw new AppException(ErrorCode.OPTION_TEMPLATE_INACTIVE);
        }
        Map<UUID, List<OptionTemplateValue>> values = valuesByTemplate(templates);
        return templates.stream()
                .map(template -> copyTemplateToItem(template, item, values.get(template.getId())))
                .toList();
    }

    private OptionGroupResponse copyTemplateToItem(
            OptionTemplate template, CatalogItem item, List<OptionTemplateValue> templateValues) {
        OptionGroup group = new OptionGroup();
        group.setItem(item);
        group.setSourceTemplateId(template.getId());
        group.setName(template.getName());
        group.setSelectionType(template.getSelectionType());
        group.setMinimumSelections(template.getMinimumSelections());
        group.setMaximumSelections(template.getMaximumSelections());
        group.setRequired(template.getMinimumSelections() > 0);
        group.setSortOrder(template.getSortOrder());
        group.setStatus(template.getStatus());
        OptionGroup savedGroup = groupRepository.save(group);
        List<OptionValue> copiedValues = templateValues
                .stream()
                .map(value -> copyValue(savedGroup, value))
                .toList();
        valueRepository.saveAll(copiedValues);
        enqueue(savedGroup, "COPIED_FROM_TEMPLATE");
        copiedValues.forEach(value -> enqueue(value, "COPIED_FROM_TEMPLATE"));
        return groupMapper.toResponse(savedGroup);
    }

    private OptionTemplateResponse changeStatus(UUID restaurantId, UUID templateId, CatalogStatus status) {
        authorize(restaurantId);
        OptionTemplate template = requiredTemplate(restaurantId, templateId);
        if (template.getStatus() != status) template.setStatus(status);
        OptionTemplate saved = templateRepository.save(template);
        return response(saved, templateValueRepository.findAllByTemplateIdInOrderBySortOrderAsc(List.of(templateId)));
    }

    private void apply(OptionTemplateUpsertRequest request, OptionTemplate template) {
        OptionSelectionRules.Normalized selection = OptionSelectionRules.normalize(
                request.selectionType(), request.minimumSelections(), request.maximumSelections());
        template.setName(request.name().trim());
        template.setSelectionType(request.selectionType());
        template.setMinimumSelections(selection.minimumSelections());
        template.setMaximumSelections(selection.maximumSelections());
        template.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    private List<OptionTemplateValue> replaceValues(OptionTemplate template, List<OptionTemplateValueInput> inputs) {
        return templateValueRepository.saveAll(inputs.stream().map(input -> {
            OptionTemplateValue value = new OptionTemplateValue();
            value.setTemplate(template);
            value.setName(input.name().trim());
            value.setAdditionalPrice(input.additionalPrice());
            value.setIsAvailable(input.isAvailable() == null || input.isAvailable());
            value.setSortOrder(input.sortOrder() == null ? 0 : input.sortOrder());
            return value;
        }).toList());
    }

    private OptionValue copyValue(OptionGroup group, OptionTemplateValue source) {
        OptionValue value = new OptionValue();
        value.setOptionGroup(group);
        value.setName(source.getName());
        value.setAdditionalPrice(source.getAdditionalPrice());
        value.setIsAvailable(source.getIsAvailable());
        value.setSortOrder(source.getSortOrder());
        return value;
    }

    private OptionTemplate requiredTemplate(UUID restaurantId, UUID templateId) {
        return templateRepository.findByIdAndRestaurantId(templateId, restaurantId)
                .orElseThrow(() -> new AppException(ErrorCode.OPTION_TEMPLATE_NOT_FOUND));
    }

    private Map<UUID, List<OptionTemplateValue>> valuesByTemplate(Collection<OptionTemplate> templates) {
        if (templates.isEmpty()) return Map.of();
        return templateValueRepository.findAllByTemplateIdInOrderBySortOrderAsc(
                        templates.stream().map(OptionTemplate::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(value -> value.getTemplate().getId()));
    }

    private OptionTemplateResponse response(OptionTemplate template, List<OptionTemplateValue> values) {
        List<OptionTemplateValueResponse> responses = (values == null ? List.<OptionTemplateValue>of() : values).stream()
                .map(value -> new OptionTemplateValueResponse(value.getId(), value.getName(), value.getAdditionalPrice(),
                        value.getIsAvailable(), value.getSortOrder()))
                .toList();
        return new OptionTemplateResponse(template.getId(), template.getRestaurantId(), template.getName(),
                template.getSelectionType(), template.getMinimumSelections(), template.getMaximumSelections(),
                template.getMinimumSelections() > 0, template.getSortOrder(), template.getStatus(), responses,
                template.getCreatedAt(), template.getUpdatedAt());
    }

    private String normalizeQuery(String query) { return query == null ? "" : query.trim(); }

    private void authorize(UUID restaurantId) { authorizationService.requireRestaurantCatalogAccess(restaurantId); }

    private void enqueue(OptionGroup group, String action) {
        outboxEventService.enqueue(CatalogEventType.OPTION_GROUP_CHANGED, "OPTION_GROUP", group.getId(),
                CatalogEventData.optionGroup(group, action));
    }

    private void enqueue(OptionValue value, String action) {
        outboxEventService.enqueue(CatalogEventType.OPTION_VALUE_CHANGED, "OPTION_VALUE", value.getId(),
                CatalogEventData.optionValue(value, action));
    }
}
