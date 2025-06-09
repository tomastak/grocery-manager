package group.rohlik.grocerymanager.model.scheduler;

import group.rohlik.grocerymanager.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Tomas Kramec
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderExpirationScheduler {

    private final IOrderService orderService;

    @Scheduled(cron = "${grocery-manager.schedule.order.expire.cron}")
    @Transactional
    public void expirePendingOrders() {
        orderService.expirePendingOrders();
    }
}
