package group.rohlik.grocerymanager.repository;

import group.rohlik.grocerymanager.model.Order;
import group.rohlik.grocerymanager.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing Order entities.
 *
 * @author Tomas Kramec
 */
@Repository
public interface IOrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id IN :ids")
    List<Order> findByIdsWithItems(List<Long> ids);

    @Query("SELECT COUNT(oi) > 0 " +
            "FROM OrderItem oi JOIN oi.order o " +
            "WHERE oi.product.code = :productCode AND o.status IN :statuses")
    boolean existsByProductCodeAndStatusIn(@Param("productCode") String productCode,
                                         @Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.code = :code")
    Optional<Order> findByCodeWithItems(@Param("code") UUID code);
}
