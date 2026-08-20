package com.khanh.fooddelivery.order_service.service.impl;

import com.khanh.fooddelivery.order_service.client.CartServiceClient;
import com.khanh.fooddelivery.order_service.client.DeliveryServiceClient;
import com.khanh.fooddelivery.order_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.order_service.client.RemoteApiResponse;
import com.khanh.fooddelivery.order_service.dto.request.CheckoutPreviewRequest;
import com.khanh.fooddelivery.order_service.dto.request.CreateOrderRequest;
import com.khanh.fooddelivery.order_service.dto.request.RejectOrderRequest;
import com.khanh.fooddelivery.order_service.dto.response.CheckoutPreviewResponse;
import com.khanh.fooddelivery.order_service.dto.response.OrderResponse;
import com.khanh.fooddelivery.order_service.entity.Order;
import com.khanh.fooddelivery.order_service.entity.OrderItem;
import com.khanh.fooddelivery.order_service.entity.OrderItemOption;
import com.khanh.fooddelivery.order_service.enums.OrderStatus;
import com.khanh.fooddelivery.order_service.exception.AppException;
import com.khanh.fooddelivery.order_service.exception.ErrorCode;
import com.khanh.fooddelivery.order_service.repository.OrderRepository;
import com.khanh.fooddelivery.order_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.order_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.order_service.service.CheckoutPreviewService;
import com.khanh.fooddelivery.order_service.service.OrderLifecycle;
import com.khanh.fooddelivery.order_service.service.OrderService;
import feign.FeignException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orders; private final CheckoutPreviewService previews;
    private final CurrentUserProvider currentUser; private final CurrentBearerTokenProvider bearer;
    private final CartServiceClient carts; private final DeliveryServiceClient deliveries; private final RestaurantServiceClient restaurants;

    @Override public OrderResponse create(Jwt jwt, CreateOrderRequest request) {
        UUID customerId = currentUser.getCurrentUserId(jwt);
        CheckoutPreviewResponse preview = previews.preview(jwt, new CheckoutPreviewRequest(request.branchId(), request.cartVersion(), request.target()));
        if (!preview.canPlaceOrder() || preview.totalAmount() == null || preview.deliveryFee() == null) throw new AppException(ErrorCode.ORDER_NOT_PLACEABLE);
        Order order = snapshot(customerId, preview);
        orders.saveAndFlush(order);
        try { carts.clear(bearer.getBearerToken(), request.branchId()); }
        catch (FeignException exception) { throw new AppException(ErrorCode.CART_SERVICE_UNAVAILABLE); }
        return response(order);
    }
    @Override @Transactional(readOnly = true) public List<OrderResponse> mine(Jwt jwt) {
        return orders.findByCustomerIdOrderByCreatedAtDesc(currentUser.getCurrentUserId(jwt)).stream().map(this::response).toList();
    }
    @Override @Transactional(readOnly = true) public List<OrderResponse> restaurant(Jwt jwt, UUID restaurantId) {
        requireRestaurantAccess(restaurantId, null);
        return orders.findByRestaurantIdOrderByCreatedAtDesc(restaurantId).stream().map(this::response).toList();
    }
    @Override public OrderResponse accept(Jwt jwt, UUID orderId) {
        Order order = restaurantOrder(jwt, orderId); OrderLifecycle.accept(order); orders.saveAndFlush(order);
        try { deliveries.startMatching(bearer.getBearerToken(), new DeliveryServiceClient.DeliveryMatchingRequest(order.getId(), order.getRestaurantId(), order.getBranchId(), order.getCustomerId(), order.getRestaurantName(), order.getBranchName(), order.getAddressDisplayLabel(), order.getLatitude(), order.getLongitude())); }
        catch (FeignException exception) { throw new AppException(ErrorCode.DELIVERY_SERVICE_UNAVAILABLE); }
        return response(order);
    }
    @Override public OrderResponse reject(Jwt jwt, UUID orderId, RejectOrderRequest request) {
        Order order = restaurantOrder(jwt, orderId); OrderLifecycle.reject(order); order.setRejectionReason(request.reason() == null || request.reason().isBlank() ? null : request.reason().trim()); return response(order);
    }
    @Override public void deliveryAssigned(UUID orderId) { OrderLifecycle.preparing(require(orderId)); }
    @Override public void pickedUp(UUID orderId) { OrderLifecycle.delivering(require(orderId)); }
    @Override public void delivered(UUID orderId) { OrderLifecycle.completed(require(orderId)); }
    @Override public void matchingFailed(UUID orderId) { OrderLifecycle.cancelMatching(require(orderId)); }

    private Order restaurantOrder(Jwt jwt, UUID id) {
        Order order = require(id);
        requireRestaurantAccess(order.getRestaurantId(), order.getBranchId());
        return order;
    }
    private Order require(UUID id) { return orders.findWithItemsById(id).orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND)); }
    private void requireRestaurantAccess(UUID restaurantId, UUID branchId) {
        try {
            RemoteApiResponse<RestaurantServiceClient.OrderAuthorizationResponse> response = restaurants.orderAuthorization(bearer.getBearerToken(), restaurantId, branchId);
            if (response == null || !response.success() || response.data() == null || !response.data().authorized()) throw new AppException(ErrorCode.ORDER_ACCESS_DENIED);
        } catch (FeignException exception) { throw new AppException(ErrorCode.RESTAURANT_SERVICE_UNAVAILABLE); }
    }
    private Order snapshot(UUID customerId, CheckoutPreviewResponse p) {
        Order order = new Order(); order.setId(UUID.randomUUID()); order.setCustomerId(customerId); order.setRestaurantId(p.restaurant().restaurantId()); order.setBranchId(p.branch().branchId()); order.setOrderCode("FD" + order.getId().toString().replace("-", "").substring(0, 10).toUpperCase()); order.setRestaurantName(p.restaurant().restaurantName()); order.setBranchName(p.branch().branchName()); order.setStatus(OrderStatus.PENDING_RESTAURANT); order.setCurrency(p.currency()); order.setItemsSubtotal(p.itemsSubtotal()); order.setDeliveryFee(p.deliveryFee()); order.setDiscountAmount(p.discountAmount()); order.setTotalAmount(p.totalAmount()); order.setAddressDisplayLabel(p.address().displayLabel()); order.setRecipientName(p.address().recipientName()); order.setRecipientPhone(p.address().recipientPhone()); order.setAddressLine(p.address().addressLine()); order.setWard(p.address().ward()); order.setDistrict(p.address().district()); order.setCity(p.address().city()); order.setLatitude(p.address().latitude()); order.setLongitude(p.address().longitude());
        for (int i=0;i<p.items().size();i++) { CheckoutPreviewResponse.CheckoutItemResponse source=p.items().get(i); OrderItem item=new OrderItem(); item.setId(UUID.randomUUID()); item.setOrder(order); item.setCatalogItemId(source.catalogItemId()); item.setBranchItemId(source.branchItemId()); item.setItemName(source.name()); item.setImageUrl(source.imageUrl()); item.setUnitPrice(source.unitPrice()); item.setQuantity(source.quantity()); item.setLineTotal(source.lineTotal()); item.setNote(source.note()); item.setSortOrder(i); for (int j=0;j<source.selectedOptions().size();j++) { CheckoutPreviewResponse.SelectedOptionResponse opt=source.selectedOptions().get(j); OrderItemOption option=new OrderItemOption(); option.setId(UUID.randomUUID()); option.setOrderItem(item); option.setOptionGroupId(opt.optionGroupId()); option.setOptionValueId(opt.optionValueId()); option.setOptionGroupName(opt.groupName()); option.setOptionValueName(opt.valueName()); option.setAdditionalPrice(opt.additionalPrice()); option.setSortOrder(j); item.getOptions().add(option); } order.getItems().add(item); }
        return order;
    }
    private OrderResponse response(Order order) { return new OrderResponse(order.getId(), order.getOrderCode(), order.getRestaurantId(), order.getRestaurantName(), order.getBranchId(), order.getBranchName(), order.getStatus(), order.getCurrency(), order.getItemsSubtotal(), order.getDeliveryFee(), order.getDiscountAmount(), order.getTotalAmount(), order.getAddressDisplayLabel(), order.getRecipientName(), order.getRecipientPhone(), order.getAddressLine(), order.getRejectionReason(), order.getCreatedAt(), order.getItems().stream().map(i -> new OrderResponse.Item(i.getId(), i.getCatalogItemId(), i.getItemName(), i.getImageUrl(), i.getUnitPrice(), i.getQuantity(), i.getLineTotal(), i.getNote(), i.getOptions().stream().map(o -> new OrderResponse.Option(o.getOptionGroupId(), o.getOptionValueId(), o.getOptionGroupName(), o.getOptionValueName(), o.getAdditionalPrice())).toList())).toList()); }
}
