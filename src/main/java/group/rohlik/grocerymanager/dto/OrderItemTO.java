package group.rohlik.grocerymanager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author Tomas Kramec
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonView(View.Read.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderItemTO {
    @JsonView(View.Read.class)
    private UUID code;

    @NotNull(message = "Product code is required", groups = View.Create.class)
    @NotBlank(message = "Product code cannot be blank", groups = View.Create.class)
    @JsonView(View.Create.class)
    private String productCode;

    @NotNull(message = "Quantity is required", groups = View.Create.class)
    @Min(value = 1, message = "Quantity must be at least 1", groups = View.Create.class)
    @JsonView(View.Create.class)
    private Integer quantity;

    @JsonView(View.Read.class)
    private BigDecimal unitPrice;

    @JsonView(View.Read.class)
    private BigDecimal totalPrice;

}
