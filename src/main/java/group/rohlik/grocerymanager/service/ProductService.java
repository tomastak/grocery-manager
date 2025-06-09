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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * @author Tomas Kramec
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ProductService implements IProductService {

    private final IProductRepository productRepository;
    private final IOrderRepository orderRepository;
    private final IProductMapper productMapper;

    @Transactional(readOnly = true)
    @Override
    public List<ProductTO> getAllProducts(final boolean onlyActive) {
        var sort = Sort.sort(Product.class).by(Product::getName).ascending();
        if (onlyActive) {
            return productMapper.toProductTOs(productRepository.findAllByArchived(false, sort));
        }
        return productMapper.toProductTOs(productRepository.findAll(sort));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "product", key = "#code")
    @Override
    public ProductTO getProductByCode(final String code) throws ProductNotFoundException {
        var product = productRepository.findByCode(code)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with code: " + code));
        return productMapper.toProductTO(product);
    }

    @Override
    public ProductTO createProduct(ProductTO productTO) throws ProductAlreadyExistsException {
        validateProduct(productTO);

        if (productRepository.existsByCode(productTO.getCode())) {
            throw new ProductAlreadyExistsException("Product with code " + productTO.getCode() + " already exists");
        }

        var product = Product.builder()
                .name(productTO.getName())
                .code(productTO.getCode())
                .pricePerUnit(productTO.getPricePerUnit())
                .stockQuantity(productTO.getStockQuantity())
                .archived(false)
                .build();

        product = productRepository.save(product);
        log.info("Created productTO with id: {}, code: {}", product.getId(), product.getCode());

        return productMapper.toProductTO(product);
    }

    @Retryable(interceptor = "productServiceRetryInterceptor")
    @CacheEvict(value = "product", key = "#productTO.code",
            condition = "#productTO != null && #productTO.code != null && #productTO.code.length() > 0")
    @Override
    public ProductTO updateProduct(ProductTO productTO) throws ProductNotFoundException {
        validateProduct(productTO);
        var product = productRepository.findByCodeForUpdate(productTO.getCode())
                .orElseThrow(() -> new ProductNotFoundException("Product not found with code: " + productTO.getCode()));

        product.setName(productTO.getName());
        product.setPricePerUnit(productTO.getPricePerUnit());
        product.setStockQuantity(productTO.getStockQuantity());
        product = productRepository.save(product);
        log.info("Updated productTO with code: {}", product.getCode());

        return productMapper.toProductTO(product);
    }

    @CacheEvict(value = "product", key = "#code")
    @Override
    public void deleteProduct(String code) throws ProductNotFoundException, ProductDeletionException {
        Assert.hasText(code, "Product code must not be empty");

        if (!productRepository.existsByCodeAndArchived(code, false)) {
            throw new ProductNotFoundException("Product not found with code: " + code);
        }

        boolean hasActiveOrders = orderRepository.existsByProductCodeAndStatusIn(code,
                Arrays.asList(OrderStatus.PENDING, OrderStatus.PAID));

        if (hasActiveOrders) {
            throw new ProductDeletionException("Cannot delete product " + code + " with active orders");
        }

        boolean hasFinishedOrders = orderRepository.existsByProductCodeAndStatusIn(code,
                Arrays.asList(OrderStatus.CANCELED, OrderStatus.EXPIRED));

        if (hasFinishedOrders) {
            productRepository.archiveByCode(code);
            log.info("Archived product with code: {}", code);
        } else {
            productRepository.deleteByCode(code);
            log.info("Deleted product with code: {}", code);
        }
    }

    @Retryable(interceptor = "productServiceRetryInterceptor")
    @CacheEvict(value = "product", key = "#productCode")
    @Override
    public Product reserveStock(final String productCode, final Integer requestedQuantity) throws ProductNotFoundException, InsufficientStockException {
        Assert.notNull(productCode, "Product code must not be null");
        Assert.isTrue(requestedQuantity > 0, "Requested quantity must be greater than zero " +
                "for product: " + productCode);

        var product = productRepository.findByCodeForUpdate(productCode)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with code: " + productCode));
        if (product.getStockQuantity() < requestedQuantity) {
            throw new InsufficientStockException(
                    "Insufficient stock for product " + product.getCode() +
                            ". Available: " + product.getStockQuantity() +
                            ", Requested: " + requestedQuantity
            );
        }
        product.setStockQuantity(product.getStockQuantity() - requestedQuantity);
        product = productRepository.saveAndFlush(product);
        log.info("Reserved {} units of product with code: {}. Current stock: {}",
                requestedQuantity, productCode, product.getStockQuantity());

        return product;
    }

    @Retryable(interceptor = "productServiceRetryInterceptor")
    @CacheEvict(value = "product", key = "#productCode")
    @Override
    public void releaseStock(final String productCode, final Integer quantityToRelease) throws ProductNotFoundException {
        Assert.notNull(productCode, "Product code must not be null");
        Assert.isTrue(quantityToRelease > 0, "Quantity to release must be greater than zero " +
                "for product: " + productCode);

        var product = productRepository.findByCodeForUpdate(productCode)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with code: " + productCode));
        product.setStockQuantity(product.getStockQuantity() + quantityToRelease);
        productRepository.saveAndFlush(product);
        log.info("Released {} units of product with code: {}. Current stock: {}",
                quantityToRelease, productCode, product.getStockQuantity());
    }

    /**
     * Validates the product data before creating or updating a product.
     *
     * @param product the product to validate
     */
    private void validateProduct(ProductTO product) {
        Assert.notNull(product, "Product must not be null");
        Assert.hasText(product.getCode(), "Product code must not be empty");
        Assert.hasText(product.getName(), "Product name must not be empty");
        Assert.notNull(product.getPricePerUnit(), "Price per unit must not be null");
        Assert.isTrue(product.getPricePerUnit().compareTo(BigDecimal.ZERO) > 0,
                "Price per unit must be greater than zero");
        Assert.notNull(product.getStockQuantity(), "Stock quantity must not be null");
        Assert.isTrue(product.getStockQuantity() >= 0, "Stock quantity must be non-negative");
    }
}
