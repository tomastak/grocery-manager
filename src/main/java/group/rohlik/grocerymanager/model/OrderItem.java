package group.rohlik.grocerymanager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Tomas Kramec
 */
@Entity
@Table(name = "GM_ORDER_ITEM")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends UpdateAwareEntity {

    @Serial
    private static final long serialVersionUID = 5320689182349673827L;

    @Id
    @GeneratedValue(generator = "SEQ_GM_ORDER_ITEM", strategy = GenerationType.SEQUENCE)
    @Column(name = "ID")
    private Long id;

    @Generated(event = EventType.INSERT)
    @Column(name = "CODE", insertable = false, updatable = false, nullable = false, unique = true)
    private UUID code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID", nullable = false)
    private Product product;

    @Column(name = "QUANTITY", nullable = false)
    private Integer quantity;

    @Column(name = "UNIT_PRICE", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "TOTAL_PRICE", nullable = false)
    private BigDecimal totalPrice;

}
