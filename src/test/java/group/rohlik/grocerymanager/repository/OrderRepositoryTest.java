package group.rohlik.grocerymanager.repository;

import group.rohlik.grocerymanager.Application;
import group.rohlik.grocerymanager.RunProfile;
import group.rohlik.grocerymanager.model.Order;
import group.rohlik.grocerymanager.model.OrderItem;
import group.rohlik.grocerymanager.model.OrderStatus;
import group.rohlik.grocerymanager.model.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
@ActiveProfiles(profiles = {RunProfile.TEST})
@Transactional
class OrderRepositoryTest {

    @Autowired
    private IOrderRepository orderRepository;
    @Autowired
    private IProductRepository productRepository;

//    @AfterEach
//    void tearDown() {
//        productRepository.deleteAll();
//    }

    private Order createOrder() {
        final Product product = new Product();
        product.setName("Test Product");
        product.setCode("P1");
        product.setArchived(false);
        product.setPricePerUnit(BigDecimal.TEN);
        product.setStockQuantity(100);
        productRepository.saveAndFlush(product);

        Order order = new Order();
        order.setCode(UUID.randomUUID());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("20.00"));
        order.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(product.getPricePerUnit());
        item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        order.setItems(List.of(item));
        order = orderRepository.saveAndFlush(order);
        return order;
    }

    @Test
    @DisplayName("findByIdsWithItems returns orders with items")
    void findByIdsWithItems_returnsOrdersWithItems() {
        final Order order = createOrder();

        final List<Order> found = orderRepository.findByIdsWithItems(List.of(order.getId()));

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getItems()).hasSize(1);
        assertThat(found.getFirst()).isEqualTo(order);
        assertThat(found.getFirst().getItems().getFirst()).isEqualTo(order.getItems().getFirst());
    }

    @Test
    @DisplayName("existsByProductCodeAndStatusIn returns true if order with product and status exists")
    void existsByProductCodeAndStatusIn_works() {
        createOrder();

        boolean exists = orderRepository.existsByProductCodeAndStatusIn("P1", List.of(OrderStatus.PENDING, OrderStatus.PAID));

        assertThat(exists).isTrue();
    }


    @Test
    @DisplayName("findByCodeWithItems returns order with items by code")
    void findByCodeWithItems_returnsOrder() {
        final Order order = createOrder();

        Optional<Order> found = orderRepository.findByCodeWithItems(order.getCode());

        assertThat(found).isPresent();
        assertThat(found.get().getItems()).hasSize(1);
        assertThat(found.get()).isEqualTo(order);
        assertThat(found.get().getItems().getFirst()).isEqualTo(order.getItems().getFirst());
    }

}

