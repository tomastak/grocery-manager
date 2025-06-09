package group.rohlik.grocerymanager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author Tomas Kramec
 */
@Data
@NoArgsConstructor
@JsonView(View.Read.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductTO {

    @NotBlank(message = "Product code is required", groups = View.Create.class)
    @Size(min = 1, max = 50, message = "Product code must be between 1 and 50 characters", groups = View.Create.class)
    @JsonView(View.Create.class)
    private String code;

    @NotBlank(message = "Product name is required")
    @Size(min = 1, max = 255, message = "Product name must be between 1 and 255 characters")
    @JsonView(View.Update.class)
    private String name;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity must be non-negative")
    @JsonView(View.Update.class)
    private Integer stockQuantity;

    @NotNull(message = "Price per unit is required")
    @DecimalMin(value = "0.01", message = "Price per unit must be at least 0.01")
    @Digits(integer = 15, fraction = 2, message = "Price per unit format is invalid")
    @JsonView(View.Update.class)
    private BigDecimal pricePerUnit;

}
