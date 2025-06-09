package group.rohlik.grocerymanager.repository;

import group.rohlik.grocerymanager.model.OrderStatus;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author Tomas Kramec
 */
public interface IExpireOrderRepository {

    List<Long> getToExpire(Date expireDateThreshold, Date expireDateBottomThreshold,
                           Set<OrderStatus> statuses, int fetchSize, int maxResults);
}
