package group.rohlik.grocerymanager.repository;

import group.rohlik.grocerymanager.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Tomas Kramec
 */
@Repository
public interface IProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.code = :code AND p.archived = FALSE")
    Optional<Product> findByCodeForUpdate(@Param("code") String code);


    List<Product> findAllByArchived(@Param("archived") Boolean archived, Sort sort);

    Optional<Product> findByCode(String code);

    @Modifying
    @Query("UPDATE Product p SET p.archived = TRUE WHERE p.code = :code")
    void archiveByCode(@Param("code") String code);

    void deleteByCode(String code);

    boolean existsByCodeAndArchived(String code, Boolean archived);

    boolean existsByCode(String code);
}