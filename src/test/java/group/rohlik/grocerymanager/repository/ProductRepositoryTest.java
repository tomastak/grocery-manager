package group.rohlik.grocerymanager.repository;

import group.rohlik.grocerymanager.Application;
import group.rohlik.grocerymanager.RunProfile;
import group.rohlik.grocerymanager.model.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
@ActiveProfiles(profiles = {RunProfile.TEST})
@Transactional
class ProductRepositoryTest {

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void tearDown() {
        try {
            productRepository.deleteAll();
        } catch (Exception e) {
            // Ignore exceptions during cleanup
        }
    }

    @Test
    @DisplayName("findByCodeForUpdate returns product when exists and not archived")
    void findByCodeForUpdate_returnsProduct() {
        Product product = new Product();
        product.setCode("P1");
        product.setName("Product 1");
        product.setStockQuantity(1);
        product.setPricePerUnit(BigDecimal.TEN);
        product.setArchived(false);
        productRepository.save(product);

        Optional<Product> found = productRepository.findByCodeForUpdate("P1");
        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("P1");
    }

    @Test
    @DisplayName("findAllByArchived returns only archived or not archived products")
    void findAllByArchived_returnsCorrectProducts() {
        Product p1 = new Product();
        p1.setCode("P1");
        p1.setName("Product 1");
        p1.setStockQuantity(1);
        p1.setPricePerUnit(BigDecimal.TEN);
        p1.setArchived(false);
        productRepository.save(p1);
        Product p2 = new Product();
        p2.setCode("P2");
        p2.setName("Product 2");
        p2.setStockQuantity(1);
        p2.setPricePerUnit(BigDecimal.TEN);
        p2.setArchived(true);
        productRepository.save(p2);

        assertThat(productRepository.findAllByArchived(false, Sort.unsorted()))
                .extracting(Product::getCode).containsExactly("P1");
        assertThat(productRepository.findAllByArchived(true, Sort.unsorted()))
                .extracting(Product::getCode).containsExactly("P2");
    }

    @Test
    @DisplayName("archiveByCode sets archived to true")
    void archiveByCode_setsArchived() {
        Product product = new Product();
        product.setCode("P1");
        product.setName("Product 1");
        product.setStockQuantity(1);
        product.setPricePerUnit(BigDecimal.TEN);
        product.setArchived(false);
        productRepository.save(product);
        productRepository.archiveByCode("P1");
        entityManager.detach(product);
        Product updated = productRepository.findByCode("P1").orElseThrow();
        assertThat(updated.isArchived()).isTrue();
    }

    @Test
    @DisplayName("deleteByCode removes product")
    void deleteByCode_removesProduct() {
        Product product = new Product();
        product.setCode("P1");
        product.setName("Product 1");
        product.setStockQuantity(1);
        product.setPricePerUnit(BigDecimal.TEN);
        product.setArchived(false);
        productRepository.save(product);
        productRepository.deleteByCode("P1");
        assertThat(productRepository.findByCode("P1")).isEmpty();
    }

    @Test
    @DisplayName("existsByCodeAndArchived returns correct value")
    void existsByCodeAndArchived_works() {
        Product product = new Product();
        product.setCode("P1");
        product.setName("Product 1");
        product.setStockQuantity(1);
        product.setPricePerUnit(BigDecimal.TEN);
        product.setArchived(true);
        productRepository.save(product);
        assertThat(productRepository.existsByCodeAndArchived("P1", true)).isTrue();
        assertThat(productRepository.existsByCodeAndArchived("P1", false)).isFalse();
    }

    @Test
    @DisplayName("existsByCode returns correct value")
    void existsByCode_works() {
        Product product = new Product();
        product.setCode("P1");
        product.setName("Product 1");
        product.setStockQuantity(1);
        product.setPricePerUnit(BigDecimal.TEN);
        product.setArchived(false);
        productRepository.save(product);
        assertThat(productRepository.existsByCode("P1")).isTrue();
        assertThat(productRepository.existsByCode("P2")).isFalse();
    }

    @Test
    @DisplayName("save duplicate code throws exception")
    void save_duplicateCode_throwsException() {
        Product product1 = new Product();
        product1.setCode("P1");
        product1.setName("Product 1");
        product1.setStockQuantity(1);
        product1.setPricePerUnit(BigDecimal.TEN);
        product1.setArchived(false);
        productRepository.saveAndFlush(product1);

        Product product2 = new Product();
        product2.setCode("P1");
        product2.setName("Product 2");
        product2.setStockQuantity(2);
        product2.setPricePerUnit(BigDecimal.valueOf(20));
        product2.setArchived(false);

        try {
            productRepository.saveAndFlush(product2);
            assertThat(false).isTrue(); // Should not reach here
        } catch (DataIntegrityViolationException e) {
            assertThat(e.getMessage()).contains("Unique index or primary key violation");
        }
    }
}
