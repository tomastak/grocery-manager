package group.rohlik.grocerymanager.service;

import group.rohlik.grocerymanager.dto.OrderItemTO;
import group.rohlik.grocerymanager.dto.OrderTO;
import group.rohlik.grocerymanager.exception.InvalidOrderStatusException;
import group.rohlik.grocerymanager.exception.OrderExpiredException;
import group.rohlik.grocerymanager.exception.OrderNotFoundException;
import group.rohlik.grocerymanager.mapper.IOrderMapper;
import group.rohlik.grocerymanager.model.Order;
import group.rohlik.grocerymanager.model.OrderItem;
import group.rohlik.grocerymanager.model.OrderStatus;
import group.rohlik.grocerymanager.model.Product;
import group.rohlik.grocerymanager.property.ExpireOrderScheduleProperties;
import group.rohlik.grocerymanager.repository.IExpireOrderRepository;
import group.rohlik.grocerymanager.repository.IOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static group.rohlik.grocerymanager.util.BatchUtil.splitIntoBatches;

/**
 * @author Tomas Kramec
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderService implements IOrderService {

    private final IOrderRepository orderRepository;
    private final ProductService productService;
    private final IOrderMapper orderMapper;
    private final IExpireOrderRepository expireOrderRepository;
    private final ExpireOrderScheduleProperties expireOrderScheduleProperties;

    @Override
    public OrderTO createOrder(final OrderTO orderTO) {
        Assert.notNull(orderTO, "Order must not be null");
        Assert.notEmpty(orderTO.getItems(), "Order must contain at least one item");

        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemTO itemRequest : orderTO.getItems()) {
            try {
                var product = productService.reserveStock(itemRequest.getProductCode(), itemRequest.getQuantity());
                orderItems.add(createOrderItem(itemRequest, product));
            } catch (Exception ex) {
                log.error("Error while reserving {} units in stock for product {}: {}. Rolling back order creation.",
                        itemRequest.getQuantity(), itemRequest.getProductCode(), ex.getMessage());
                throw ex;
            }
        }
        var order = createOrder(orderItems);
//        eventPublisher.publishEvent(new OrderCreatedEvent(savedOrder.getId()));
        log.info("Created order with id: {}, code: {}, totalAmount: {}",
                order.getId(), order.getCode(), order.getTotalAmount());

        return orderMapper.toOrderTO(order);
    }

    @Transactional(noRollbackFor = OrderExpiredException.class)
    @Override
    public OrderTO payOrder(final UUID code) {
        Assert.notNull(code, "Order code must not be null");

        var order = getOrderByCodeInternal(code);
        checkOrderExpiration(order, "Cannot pay expired order");
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException("Order "+ code + " cannot be paid. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.PAID);
        var paidOrder = orderRepository.save(order);
//        eventPublisher.publishEvent(new OrderPaidEvent(paidOrder.getId()));
        log.info("Paid order with code: {}", paidOrder.getCode());

        return orderMapper.toOrderTO(paidOrder);
    }

    @Transactional(noRollbackFor = OrderExpiredException.class)
    @Override
    public OrderTO cancelOrder(final UUID code) {
        Assert.notNull(code, "Order code must not be null");

        var order = getOrderByCodeInternal(code);
        checkOrderExpiration(order, "Cannot cancel expired order");
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException("Order "+ code + " cannot be canceled. Current status: " + order.getStatus());
        }
        releaseStock(order);

        order.setStatus(OrderStatus.CANCELED);
        var savedOrder = orderRepository.save(order);
//        eventPublisher.publishEvent(new OrderCanceledEvent(order.getId()));
        log.info("Canceled order with code: {}", savedOrder.getCode());

        return orderMapper.toOrderTO(savedOrder);
    }

    @Transactional(readOnly = true)
    @Override
    public OrderTO getOrderByCode(final UUID code) {
        return orderMapper.toOrderTO(getOrderByCodeInternal(code));
    }

    @Override
    public void expirePendingOrders() {
        var systemDate = Instant.now().atZone(ZoneId.systemDefault());
        var expirationDateThreshold = systemDate.minus(expireOrderScheduleProperties.getThreshold())
                .truncatedTo(ChronoUnit.SECONDS);
        var expirationDateBottomThreshold = systemDate.minus(expireOrderScheduleProperties.getBottomThreshold())
                .truncatedTo(ChronoUnit.SECONDS);
        var orderStatuses = Set.of(OrderStatus.PENDING);

        var orders = expireOrderRepository.getToExpire(Date.from(expirationDateThreshold.toInstant()),
                Date.from(expirationDateBottomThreshold.toInstant()), orderStatuses,
                expireOrderScheduleProperties.getBatchUpdateSize(),
                expireOrderScheduleProperties.getMaxSize());
        if (!orders.isEmpty()) {
            log.info("Found {} orders to expire", orders.size());
            batchExpire(orders);
        }
    }

    /**
     * Retrieves an order by its unique code, including its items.
     * If the order is not found, an OrderNotFoundException is thrown.
     *
     * @param code the unique identifier of the order
     * @return the Order entity with its items preloaded
     * @throws OrderNotFoundException if the order with the given code does not exist
     */
    private Order getOrderByCodeInternal(final UUID code) {
        return orderRepository.findByCodeWithItems(code)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with code: " + code));
    }

    /**
     * Splits the provided list of order IDs into batches and processes each batch to expire orders.
     * It logs the total number of expired orders and the time taken for the operation.
     *
     * @param orderIds the list of order IDs to be processed
     */
    private void batchExpire(final List<Long> orderIds) {
        var startTime = Instant.now();
        var expiredTotalCount = new AtomicInteger();
        splitIntoBatches(orderIds, expireOrderScheduleProperties.getBatchUpdateSize())
                .forEach(batch -> {
                    var orders = orderRepository.findByIdsWithItems(batch);
                    log.info("Expiring {} orders in batch", orders.size());
                    orders.forEach(order -> {
                        if (shouldOrderBeExpired(order)) {
                            try {
                                expireOrder(order);
                                expiredTotalCount.incrementAndGet();
                            } catch (Exception e) {
                                log.error("Failed to expire order {}: {}. Continuing with next order.",
                                        order.getCode(), e.getMessage());
                            }
                        }
                    });
                });
        var duration = Duration.between(startTime, Instant.now()).toMillis();
        log.info("Total expired orders: {}. Total time: {} ms.", expiredTotalCount.get(), duration);
    }

    /**
     * Expires the given order by releasing its stock and updating its status to {@link OrderStatus#EXPIRED}.
     * It also saves the updated order to the repository.
     *
     * @param order the order to be expired
     */
    private void expireOrder(final Order order) {
        releaseStock(order);
        order.setStatus(OrderStatus.EXPIRED);
        orderRepository.save(order);
//        eventPublisher.publishEvent(new OrderExpiredEvent(order.getId()));
        log.info("Expired order with code: {}", order.getCode());
    }

    /**
     * Creates an OrderItem entity from the provided OrderItemTO and Product.
     * It calculates the total price based on the quantity and product's price per unit.
     *
     * @param itemTO the transfer object containing order item details
     * @param product the product associated with this order item
     * @return a new OrderItem entity
     */
    private OrderItem createOrderItem(final OrderItemTO itemTO, final Product product) {
        return OrderItem.builder()
                .product(product)
                .quantity(itemTO.getQuantity())
                .unitPrice(product.getPricePerUnit())
                .totalPrice(calculateTotalPrice(itemTO.getQuantity(), product.getPricePerUnit()))
                .build();
    }

    /**
     * Creates a new order with the given list of items and stores it in the repository.
     * It sets the order status to {@link OrderStatus#PENDING}, calculates the total amount,
     * and sets the expiration time based on the configured threshold.
     *
     * @param items the list of order items to be included in the order
     * @return the created Order entity
     */
    private Order createOrder(final List<OrderItem> items) {
        Assert.notEmpty(items, "Order must contain at least one item");

        var order = Order.builder()
                .status(OrderStatus.PENDING)
                .expiresAt(LocalDateTime.now().plus(expireOrderScheduleProperties.getThreshold()))
                .items(items)
                .totalAmount(calculateTotalAmount(items))
                .build();

        items.forEach(item -> item.setOrder(order));

        return orderRepository.saveAndFlush(order);
    }

    /**
     * Calculates the total amount for the order based on the total prices of its items.
     * It filters out any null or zero total prices before summing them up.
     *
     * @param items the list of order items with their total prices already calculated
     * @return the total amount for the order
     */
    private BigDecimal calculateTotalAmount(final List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getTotalPrice)
                .filter(Objects::nonNull)
                .filter(totalPrice -> totalPrice.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates the total price for a given quantity and unit price.
     * The result is rounded to two decimal places using HALF_UP rounding mode.
     *
     * @param quantity the quantity of order item
     * @param unitPrice the price per unit
     * @return the total price for the given quantity and unit price
     */
    private BigDecimal calculateTotalPrice(final Integer quantity, final BigDecimal unitPrice) {
        Assert.notNull(unitPrice, "Unit price must not be null");
        Assert.notNull(quantity, "Quantity must not be null");
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Checks if the order has expired or is about to expire. If it has expired, an exception is thrown.
     * If it is about to expire, the full order expiration is processed.
     * This method is used to ensure that operations on the order are only performed if it is still valid.
     *
     * @param order the order to check
     * @param errorMsg the error message to include in the exception if the order is expired
     * @throws OrderExpiredException if the order is expired
     */
    private void checkOrderExpiration(final Order order, final String errorMsg) throws OrderExpiredException {
        Assert.notNull(order, "Order must not be null");
        if (order.getStatus() == OrderStatus.EXPIRED) {
            throw new OrderExpiredException("Order " + order.getCode() + " has already expired. " + errorMsg);
        }
        if (shouldOrderBeExpired(order)) {
            expireOrder(order);
            throw new OrderExpiredException("Order " + order.getCode() + " has expired. " + errorMsg);
        }
    }

    /**
     * Checks if the order should be expired based on its status and expiration time.
     *
     * @param order the order to check
     * @return true if the order should be expired, false otherwise
     */
    private boolean shouldOrderBeExpired(final Order order) {
        Assert.notNull(order, "Order must not be null");
        Assert.notNull(order.getExpiresAt(), "Order expiration time must not be null");
        return order.getStatus() == OrderStatus.PENDING
                && order.getExpiresAt().isBefore(LocalDateTime.now());
    }

    /**
     * Releases stock for all the items in the order.
     *
     * @param order the order for which stock should be released
     */
    private void releaseStock(final Order order) {
        Assert.notNull(order, "Order must not be null");
        order.getItems().forEach(item -> {
            try {
                productService.releaseStock(item.getProduct().getCode(), item.getQuantity());
            } catch (Exception ex) {
                log.error("Error while releasing {} units to stock for product {}. " +
                                "Rolling back operation for order {}: {}",
                        item.getQuantity(), item.getProduct().getCode(), order.getCode(), ex.getMessage());
                throw ex;
            }
        });

    }
}
