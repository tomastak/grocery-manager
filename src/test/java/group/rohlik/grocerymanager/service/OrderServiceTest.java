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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the OrderService class.
 * @author Tomas Kramec
 */
class OrderServiceTest {

    @Mock
    private IOrderRepository orderRepository;
    @Mock
    private ProductService productService;
    @Mock
    private IOrderMapper orderMapper;
    @Mock
    private IExpireOrderRepository expireOrderRepository;
    @Mock
    private ExpireOrderScheduleProperties expireOrderScheduleProperties;

    @InjectMocks
    private OrderService orderService;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(expireOrderScheduleProperties.getThreshold()).thenReturn(java.time.Duration.ofMinutes(30));
        when(expireOrderScheduleProperties.getBottomThreshold()).thenReturn(java.time.Duration.ofMinutes(10));
        when(expireOrderScheduleProperties.getBatchUpdateSize()).thenReturn(100);
        when(expireOrderScheduleProperties.getMaxSize()).thenReturn(1000);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void createOrder_success() {
        OrderItemTO itemTO = new OrderItemTO();
        itemTO.setProductCode("P1");
        itemTO.setQuantity(2);

        Product product = Product.builder().code("P1").pricePerUnit(BigDecimal.TEN).build();
        when(productService.reserveStock("P1", 2)).thenReturn(product);

        OrderItem orderItem = OrderItem.builder()
                .product(product)
                .quantity(2)
                .unitPrice(BigDecimal.TEN)
                .totalPrice(BigDecimal.valueOf(20))
                .build();

        Order order = Order.builder()
                .id(1L)
                .code(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .items(List.of(orderItem))
                .totalAmount(BigDecimal.valueOf(20))
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        when(orderRepository.saveAndFlush(any(Order.class))).thenReturn(order);
        when(orderMapper.toOrderTO(any(Order.class))).thenReturn(new OrderTO());

        OrderTO orderTO = new OrderTO();
        orderTO.setItems(List.of(itemTO));

        OrderTO result = orderService.createOrder(orderTO);

        assertThat(result).isNotNull();
        verify(productService).reserveStock("P1", 2);
        verify(orderRepository).saveAndFlush(any(Order.class));
        verify(orderMapper).toOrderTO(any(Order.class));
    }

    @Test
    void createOrder_nullOrder_throwsException() {
        assertThatThrownBy(() -> orderService.createOrder(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createOrder_emptyItems_throwsException() {
        OrderTO orderTO = new OrderTO();
        orderTO.setItems(Collections.emptyList());
        assertThatThrownBy(() -> orderService.createOrder(orderTO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void payOrder_success() {
        UUID code = UUID.randomUUID();
        Order order = Order.builder()
                .code(code)
                .status(OrderStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        when(orderRepository.findByCodeWithItems(code)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toOrderTO(any(Order.class))).thenReturn(new OrderTO());

        OrderTO result = orderService.payOrder(code);

        assertThat(result).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
    }

    @Test
    void payOrder_expired_releaseStockAndThrowsException() {
        UUID code = UUID.randomUUID();
        Order order = Order.builder()
                .code(code)
                .items(List.of(
                        OrderItem.builder().product(Product.builder().code("P1").build()).quantity(2).build()
                ))
                .status(OrderStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(orderRepository.findByCodeWithItems(code)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.payOrder(code))
                .isInstanceOf(OrderExpiredException.class);

        verify(productService).releaseStock("P1", 2);
    }

    @Test
    void payOrder_expired_throwsException() {
        UUID code = UUID.randomUUID();
        Order order = Order.builder()
                .code(code)
                .items(Collections.emptyList())
                .status(OrderStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(orderRepository.findByCodeWithItems(code)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.payOrder(code))
                .isInstanceOf(OrderExpiredException.class);
    }

    @Test
    void payOrder_invalidStatus_throwsException() {
        UUID code = UUID.randomUUID();
        Order order = Order.builder()
                .code(code)
                .status(OrderStatus.PAID)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        when(orderRepository.findByCodeWithItems(code)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.payOrder(code))
                .isInstanceOf(InvalidOrderStatusException.class);
    }

    @Test
    void cancelOrder_success() {
        UUID code = UUID.randomUUID();
        Product product = Product.builder().code("P1").build();
        OrderItem item = OrderItem.builder().product(product).quantity(2).build();
        Order order = Order.builder()
                .code(code)
                .status(OrderStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .items(List.of(item))
                .build();

        when(orderRepository.findByCodeWithItems(code)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toOrderTO(any(Order.class))).thenReturn(new OrderTO());

        OrderTO result = orderService.cancelOrder(code);

        assertThat(result).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        verify(productService).releaseStock("P1", 2);
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrder_expired_throwsException() {
        UUID code = UUID.randomUUID();
        Order order = Order.builder()
                .code(code)
                .status(OrderStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .items(List.of())
                .build();

        when(orderRepository.findByCodeWithItems(code)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(code))
                .isInstanceOf(OrderExpiredException.class);
    }

    @Test
    void cancelOrder_invalidStatus_throwsException() {
        UUID code = UUID.randomUUID();
        Order order = Order.builder()
                .code(code)
                .status(OrderStatus.PAID)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .items(List.of())
                .build();

        when(orderRepository.findByCodeWithItems(code)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(code))
                .isInstanceOf(InvalidOrderStatusException.class);
    }

    @Test
    void getOrderByCode_success() {
        UUID code = UUID.randomUUID();
        Order order = Order.builder().code(code).build();
        when(orderRepository.findByCodeWithItems(code)).thenReturn(Optional.of(order));
        when(orderMapper.toOrderTO(order)).thenReturn(new OrderTO());

        OrderTO result = orderService.getOrderByCode(code);

        assertThat(result).isNotNull();
        verify(orderRepository).findByCodeWithItems(code);
        verify(orderMapper).toOrderTO(order);
    }

    @Test
    void getOrderByCode_notFound_throwsException() {
        UUID code = UUID.randomUUID();
        when(orderRepository.findByCodeWithItems(code)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderByCode(code))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void expirePendingOrders_shouldExpireOrders() {
        List<Long> orderIds = List.of(1L, 2L, 3L);
        when(expireOrderRepository.getToExpire(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(orderIds);

        Order order1 = Order.builder().id(1L).status(OrderStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .items(List.of(OrderItem.builder().product(Product.builder().code("P1").build()).quantity(1).build()))
                .build();
        Order order2 = Order.builder().id(2L).status(OrderStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .items(List.of(OrderItem.builder().product(Product.builder().code("P2").build()).quantity(2).build()))
                .build();
        Order order3 = Order.builder().id(3L).status(OrderStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .items(List.of(OrderItem.builder().product(Product.builder().code("P3").build()).quantity(3).build()))
                .build();

        when(orderRepository.findByIdsWithItems(any())).thenReturn(List.of(order1, order2, order3));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.expirePendingOrders();

        assertThat(order1.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        verify(productService).releaseStock("P1", 1);
        verify(orderRepository).save(order1);
        assertThat(order2.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        verify(productService).releaseStock("P2", 2);
        verify(orderRepository).save(order2);
        assertThat(order3.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        verify(productService).releaseStock("P3", 3);
        verify(orderRepository).save(order3);
    }

}
