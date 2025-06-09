package group.rohlik.grocerymanager.service;

import group.rohlik.grocerymanager.dto.OrderTO;
import group.rohlik.grocerymanager.exception.OrderExpiredException;
import group.rohlik.grocerymanager.model.OrderStatus;

import java.util.UUID;

/**
 * Service interface for managing orders in the grocery manager application.
 *
 * @author Tomas Kramec
*/
public interface IOrderService {

    /**
     * Creates a new order in state {@link OrderStatus#PENDING}.
     * This method also reserves stock for the products in the order.
     *
     * @param orderTO the transfer object containing order details
     * @return the created Order
     */
    OrderTO createOrder(OrderTO orderTO);

    /**
     * Pays for an order identified by its unique code.
     * Only {@link OrderStatus#PENDING} orders can be paid.
     *
     * @param code the unique identifier of the order
     * @return the updated Order after payment
     */
    OrderTO payOrder(UUID code);

    /**
     * Cancels an order identified by its unique code.
     * Only {@link OrderStatus#PENDING} orders can be canceled.
     * This method also releases the stock of the products in the order.
     *
     * @param code the unique identifier of the order
     * @return the updated Order after cancellation
     */
    OrderTO cancelOrder(UUID code);

    /**
     * Retrieves an order by its unique code.
     *
     * @param code the unique identifier of the order
     * @return the Order transfer object
     * @throws OrderExpiredException if the order has expired
     */
    OrderTO getOrderByCode(UUID code);

    /**
     * Expires pending orders that have not been {@link OrderStatus#PAID} or {@link OrderStatus#CANCELED}
     * and are past their expiration time.
     * It retrieves orders that are eligible for expiration based on the configured thresholds,
     * and processes them in batches to release stock and update their status to {@link OrderStatus#EXPIRED}.
     */
    void expirePendingOrders();
}
