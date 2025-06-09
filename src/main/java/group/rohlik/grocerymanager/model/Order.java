package group.rohlik.grocerymanager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Tomas Kramec
 */
@Entity
@Table(name = "GM_ORDER")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@NamedQuery(name = Order.GET_IDS_TO_EXPIRE,
        query = """
                SELECT o.id FROM Order o
                WHERE nvl(o.updateDate, o.creationDate) BETWEEN :expireDateBottomThreshold AND :expireDateThreshold
                AND o.status IN :statuses
                ORDER BY o.id
                """)
public class Order extends UpdateAwareEntity {

    public static final String GET_IDS_TO_EXPIRE = "group.rohlik.grocerymanager.model.Order.GET_IDS_TO_ARCHIVE";

    @Serial
    private static final long serialVersionUID = -5391608825800960711L;

    @Id
    @GeneratedValue(generator = "SEQ_GM_ORDER", strategy = GenerationType.SEQUENCE)
    @Column(name = "ID")
    private Long id;

    @Generated(event = EventType.INSERT)
    @Column(name = "CODE", insertable = false, updatable = false, nullable = false, unique = true)
    private UUID code;

    @Enumerated(EnumType.STRING)
    @Column(name ="STATUS", nullable = false)
    private OrderStatus status;

    @Column(name = "TOTAL_AMOUNT", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "EXPIRES_AT", nullable = false)
    private LocalDateTime expiresAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = OrderStatus.PENDING;
        }
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMinutes(30);
        }
        calculateTotalAmount();
    }

    private void calculateTotalAmount() {
        totalAmount = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
