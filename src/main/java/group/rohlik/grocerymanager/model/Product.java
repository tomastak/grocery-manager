package group.rohlik.grocerymanager.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * @author Tomas Kramec
 */
@Entity
@Table(name = "GM_PRODUCT")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends UpdateAwareEntity {
    @Serial
    private static final long serialVersionUID = 59156437761494618L;

    @Id
    @GeneratedValue(generator = "SEQ_GM_PRODUCT", strategy = GenerationType.SEQUENCE)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODE", updatable = false, nullable = false, unique = true)
    private String code;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "STOCK_QUANTITY", nullable = false)
    private Integer stockQuantity;

    @Column(name = "UNIT_PRICE", nullable = false)
    private BigDecimal pricePerUnit;

    @Column(name = "ARCHIVED", nullable = false)
    private boolean archived = false;

}
