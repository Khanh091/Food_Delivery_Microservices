package com.khanh.fooddelivery.order_service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.khanh.fooddelivery.order_service.entity.Order;
import com.khanh.fooddelivery.order_service.entity.OrderItem;
import com.khanh.fooddelivery.order_service.entity.OrderItemOption;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class OrderMapperTests {
    private final OrderMapper mapper = Mappers.getMapper(OrderMapper.class);

    @Test
    void maps_item_options_without_inventing_null_labels() {
        Order order = new Order();
        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setItemName("Cơm gà");
        item.setUnitPrice(BigDecimal.TEN);
        item.setLineTotal(BigDecimal.TEN);
        item.setQuantity(1);

        OrderItemOption option = new OrderItemOption();
        option.setId(UUID.randomUUID());
        option.setOptionGroupName(null);
        option.setOptionValueName("Thêm cơm");
        item.setOptions(List.of(option));
        order.setItems(List.of(item));

        var response = mapper.toResponse(order);

        assertThat(response.items()).singleElement().satisfies(mapped -> {
            assertThat(mapped.options()).singleElement().satisfies(mappedOption -> {
                assertThat(mappedOption.groupName()).isNull();
                assertThat(mappedOption.valueName()).isEqualTo("Thêm cơm");
            });
        });
    }
}
