package group.rohlik.grocerymanager.repository;

import group.rohlik.grocerymanager.model.Order;
import group.rohlik.grocerymanager.model.OrderStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.hibernate.jpa.HibernateHints.*;

/**
 * @author Tomas Kramec
 */
@Component
@RequiredArgsConstructor
public class ExpireOrderRepository implements IExpireOrderRepository {

    private final EntityManager entityManager;

    @Override
    public List<Long> getToExpire(final Date expireDateThreshold, final Date expireDateBottomThreshold,
                                  final Set<OrderStatus> statuses,
                                  final int fetchSize, final int maxResults) {
        var query = entityManager.createNamedQuery(Order.GET_IDS_TO_EXPIRE, Long.class);
        query.setParameter("expireDateThreshold", expireDateThreshold);
        query.setParameter("expireDateBottomThreshold", expireDateBottomThreshold);
        query.setParameter("statuses", statuses);
        query.setHint(HINT_FETCH_SIZE, fetchSize);
        query.setHint(HINT_CACHEABLE, false);
        query.setHint(HINT_READ_ONLY, true);
        query.setMaxResults(maxResults);
        return query.getResultList();
    }
}
