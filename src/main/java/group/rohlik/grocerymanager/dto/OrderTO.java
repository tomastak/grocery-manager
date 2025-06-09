package group.rohlik.grocerymanager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import group.rohlik.grocerymanager.model.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Tomas Kramec
 */
@Data
@NoArgsConstructor
@JsonView(View.Read.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderTO {

    @JsonView(View.Read.class)
    private UUID code;
    @JsonView(View.Read.class)
    private OrderStatus status;
    @JsonView(View.Read.class)
    private BigDecimal totalAmount;
    @JsonView(View.Read.class)
    private LocalDateTime expiresAt;

    @Valid
    @NotEmpty(message = "Order must contain at least one item", groups = View.Create.class)
    @JsonView(View.Create.class)
    private List<OrderItemTO> items = new ArrayList<>();
}
