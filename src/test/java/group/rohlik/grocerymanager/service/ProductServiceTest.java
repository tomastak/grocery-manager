package group.rohlik.grocerymanager.service;

import group.rohlik.grocerymanager.dto.ProductTO;
import group.rohlik.grocerymanager.exception.InsufficientStockException;
import group.rohlik.grocerymanager.exception.ProductAlreadyExistsException;
import group.rohlik.grocerymanager.exception.ProductDeletionException;
import group.rohlik.grocerymanager.exception.ProductNotFoundException;
import group.rohlik.grocerymanager.mapper.IProductMapper;
import group.rohlik.grocerymanager.model.OrderStatus;
import group.rohlik.grocerymanager.model.Product;
import group.rohlik.grocerymanager.repository.IOrderRepository;
import group.rohlik.grocerymanager.repository.IProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the ProductService class.
 *
 * @author Tomas Kramec
 */
class ProductServiceTest {

    @Mock
    private IProductRepository productRepository;
    @Mock
    private IOrderRepository orderRepository;
    @Mock
    private IProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void getAllProducts_onlyActive_true() {
        List<Product> products = List.of(Product.builder().code("P1").build());
        List<ProductTO> productTOs = List.of(ProductTO.builder().code("P1").build());
        when(productRepository.findAllByArchived(eq(false), any(Sort.class))).thenReturn(products);
        when(productMapper.toProductTOs(products)).thenReturn(productTOs);

        List<ProductTO> result = productService.getAllProducts(true);

        assertThat(result).isEqualTo(productTOs);
        verify(productRepository).findAllByArchived(eq(false), any(Sort.class));
    }

    @Test
    void getAllProducts_onlyActive_false() {
        List<Product> products = List.of(Product.builder().code("P1").build());
        List<ProductTO> productTOs = List.of(ProductTO.builder().code("P1").build());
        when(productRepository.findAll(any(Sort.class))).thenReturn(products);
        when(productMapper.toProductTOs(products)).thenReturn(productTOs);

        List<ProductTO> result = productService.getAllProducts(false);

        assertThat(result).isEqualTo(productTOs);
        verify(productRepository).findAll(any(Sort.class));
    }

    @Test
    void getProductByCode_success() {
        Product product = Product.builder().code("P1").build();
        ProductTO productTO = ProductTO.builder().code("P1").build();
        when(productRepository.findByCode("P1")).thenReturn(Optional.of(product));
        when(productMapper.toProductTO(product)).thenReturn(productTO);

        ProductTO result = productService.getProductByCode("P1");

        assertThat(result).isEqualTo(productTO);
        verify(productRepository).findByCode("P1");
    }

    @Test
    void getProductByCode_notFound() {
        when(productRepository.findByCode("P1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.getProductByCode("P1"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void createProduct_success() {
        ProductTO productTO = new ProductTO();
        productTO.setCode("P1");
        productTO.setName("Milk");
        productTO.setPricePerUnit(BigDecimal.TEN);
        productTO.setStockQuantity(10);

        when(productRepository.existsByCode("P1")).thenReturn(false);
        Product product = Product.builder().code("P1").name("Milk").pricePerUnit(BigDecimal.TEN).stockQuantity(10).archived(false).build();
        when(productRepository.save(any(Product.class))).thenReturn(product);
        ProductTO mappedTO = new ProductTO();
        when(productMapper.toProductTO(product)).thenReturn(mappedTO);

        ProductTO result = productService.createProduct(productTO);

        assertThat(result).isEqualTo(mappedTO);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_alreadyExists() {
        ProductTO productTO = new ProductTO();
        productTO.setCode("P1");
        productTO.setName("Milk");
        productTO.setPricePerUnit(BigDecimal.TEN);
        productTO.setStockQuantity(10);

        when(productRepository.existsByCode("P1")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(productTO))
                .isInstanceOf(ProductAlreadyExistsException.class);
    }

    @Test
    void updateProduct_success() {
        ProductTO productTO = new ProductTO();
        productTO.setCode("P1");
        productTO.setName("Milk");
        productTO.setPricePerUnit(BigDecimal.TEN);
        productTO.setStockQuantity(10);

        Product product = Product.builder().code("P1").name("Milk").pricePerUnit(BigDecimal.TEN).stockQuantity(10).build();
        when(productRepository.findByCodeForUpdate("P1")).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        ProductTO mappedTO = new ProductTO();
        when(productMapper.toProductTO(product)).thenReturn(mappedTO);

        ProductTO result = productService.updateProduct(productTO);

        assertThat(result).isEqualTo(mappedTO);
        verify(productRepository).save(product);
    }

    @Test
    void updateProduct_notFound() {
        ProductTO productTO = new ProductTO();
        productTO.setCode("P1");
        productTO.setName("Milk");
        productTO.setPricePerUnit(BigDecimal.TEN);
        productTO.setStockQuantity(10);

        when(productRepository.findByCodeForUpdate("P1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(productTO))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void hasProductActiveOrders_true() {
        when(productRepository.existsByCode("P1")).thenReturn(true);
        when(orderRepository.existsByProductCodeAndStatusIn(eq("P1"), eq(Arrays.asList(OrderStatus.PENDING, OrderStatus.PAID))))
                .thenReturn(true);

        boolean result = productService.hasProductActiveOrders("P1");

        assertThat(result).isTrue();
    }

    @Test
    void hasProductActiveOrders_false() {
        when(productRepository.existsByCode("P1")).thenReturn(true);
        when(orderRepository.existsByProductCodeAndStatusIn(eq("P1"), eq(Arrays.asList(OrderStatus.PENDING, OrderStatus.PAID))))
                .thenReturn(false);

        boolean result = productService.hasProductActiveOrders("P1");

        assertThat(result).isFalse();
    }

    @Test
    void hasProductActiveOrders_notFound() {
        when(productRepository.existsByCode("P1")).thenReturn(false);

        assertThatThrownBy(() -> productService.hasProductActiveOrders("P1"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void hasProductFinishedOrders_true() {
        when(productRepository.existsByCode("P1")).thenReturn(true);
        when(orderRepository.existsByProductCodeAndStatusIn(eq("P1"), eq(Arrays.asList(OrderStatus.CANCELED, OrderStatus.EXPIRED))))
                .thenReturn(true);

        boolean result = productService.hasProductFinishedOrders("P1");

        assertThat(result).isTrue();
    }

    @Test
    void hasProductFinishedOrders_false() {
        when(productRepository.existsByCode("P1")).thenReturn(true);
        when(orderRepository.existsByProductCodeAndStatusIn(eq("P1"), eq(Arrays.asList(OrderStatus.CANCELED, OrderStatus.EXPIRED))))
                .thenReturn(false);

        boolean result = productService.hasProductFinishedOrders("P1");

        assertThat(result).isFalse();
    }

    @Test
    void hasProductFinishedOrders_notFound() {
        when(productRepository.existsByCode("P1")).thenReturn(false);

        assertThatThrownBy(() -> productService.hasProductFinishedOrders("P1"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void deleteProduct_noActiveOrFinishedOrders_deletes() {
        when(productRepository.existsByCodeAndArchived("P1", false)).thenReturn(true);
        when(orderRepository.existsByProductCodeAndStatusIn(eq("P1"), eq(Arrays.asList(OrderStatus.PENDING, OrderStatus.PAID))))
                .thenReturn(false);
        when(orderRepository.existsByProductCodeAndStatusIn(eq("P1"), eq(Arrays.asList(OrderStatus.CANCELED, OrderStatus.EXPIRED))))
                .thenReturn(false);

        productService.deleteProduct("P1");

        verify(productRepository).deleteByCode("P1");
    }

    @Test
    void deleteProduct_withActiveOrders_throws() {
        when(productRepository.existsByCodeAndArchived("P1", false)).thenReturn(true);
        when(orderRepository.existsByProductCodeAndStatusIn(eq("P1"), eq(Arrays.asList(OrderStatus.PENDING, OrderStatus.PAID))))
                .thenReturn(true);

        assertThatThrownBy(() -> productService.deleteProduct("P1"))
                .isInstanceOf(ProductDeletionException.class);
    }

    @Test
    void deleteProduct_withFinishedOrders_archives() {
        when(productRepository.existsByCodeAndArchived("P1", false)).thenReturn(true);
        when(orderRepository.existsByProductCodeAndStatusIn(eq("P1"), eq(Arrays.asList(OrderStatus.PENDING, OrderStatus.PAID))))
                .thenReturn(false);
        when(orderRepository.existsByProductCodeAndStatusIn(eq("P1"), eq(Arrays.asList(OrderStatus.CANCELED, OrderStatus.EXPIRED))))
                .thenReturn(true);

        productService.deleteProduct("P1");

        verify(productRepository).archiveByCode("P1");
    }

    @Test
    void deleteProduct_notFound() {
        when(productRepository.existsByCodeAndArchived("P1", false)).thenReturn(false);

        assertThatThrownBy(() -> productService.deleteProduct("P1"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void reserveStock_success() {
        Product product = Product.builder().code("P1").stockQuantity(10).build();
        when(productRepository.findByCodeForUpdate("P1")).thenReturn(Optional.of(product));
        when(productRepository.saveAndFlush(any(Product.class))).thenReturn(product);

        Product result = productService.reserveStock("P1", 5);

        assertThat(result.getStockQuantity()).isEqualTo(5);
        verify(productRepository).saveAndFlush(product);
    }

    @Test
    void reserveStock_notFound() {
        when(productRepository.findByCodeForUpdate("P1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.reserveStock("P1", 5))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void reserveStock_insufficient() {
        Product product = Product.builder().code("P1").stockQuantity(2).build();
        when(productRepository.findByCodeForUpdate("P1")).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.reserveStock("P1", 5))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void releaseStock_success() {
        Product product = Product.builder().code("P1").stockQuantity(5).build();
        when(productRepository.findByCodeForUpdate("P1")).thenReturn(Optional.of(product));
        when(productRepository.saveAndFlush(any(Product.class))).thenReturn(product);

        productService.releaseStock("P1", 3);

        assertThat(product.getStockQuantity()).isEqualTo(8);
        verify(productRepository).saveAndFlush(product);
    }

    @Test
    void releaseStock_notFound() {
        when(productRepository.findByCodeForUpdate("P1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.releaseStock("P1", 3))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
