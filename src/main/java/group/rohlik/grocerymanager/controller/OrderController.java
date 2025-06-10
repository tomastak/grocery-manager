package group.rohlik.grocerymanager.controller;

import com.fasterxml.jackson.annotation.JsonView;
import group.rohlik.grocerymanager.dto.OrderTO;
import group.rohlik.grocerymanager.dto.View;
import group.rohlik.grocerymanager.service.IOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for managing orders in the grocery manager application.
 * This controller provides endpoints to create, pay, and cancel orders.
 *
 * @author Tomas Kramec
 */
@RestController
@RequestMapping("/api/v1/orders")
@Validated
@CrossOrigin(origins = "*")
@Tag(name = "Orders", description = "Order management operations")
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    @PostMapping
    @JsonView(View.Read.class)
    @Operation(summary = "Create order", description = "Create a new order in state PENDING." +
            " Reserve stock quantities for the products in the order.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "201",
                            description = "Order created successfully and stock quantities reserved"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Bad Request: Invalid order data or insufficient stock"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Not Found: Product from order not found"
                    )
            }
    )
    @Validated(View.Create.class)
    public ResponseEntity<OrderTO> createOrder(@JsonView(View.Create.class) @Valid @RequestBody OrderTO orderTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(orderTO));
    }

    @PostMapping("/{code}/pay")
    @JsonView(View.Read.class)
    @Operation(summary = "Pay for order", description = "Mark an order as PAID." +
            " Only orders in state PENDING can be paid. ",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Order paid successfully"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Bad Request: Order cannot be paid because it is not in PENDING state or has expired"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Order not found"
                    )
            }
    )
    public ResponseEntity<OrderTO> payOrder(@PathVariable UUID code) {
        return ResponseEntity.ok(orderService.payOrder(code));
    }

    @PostMapping("/{code}/cancel")
    @JsonView(View.Read.class)
    @Operation(summary = "Cancel order", description = "Cancel an order and release reserved stock. " +
            "Only orders in state PENDING can be canceled.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Order canceled successfully and stock released"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Bad Request: Order cannot be canceled because it is not in PENDING state or has expired"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Order not found"
                    )
            }
    )
    public ResponseEntity<OrderTO> cancelOrder(@PathVariable UUID code) {
        return ResponseEntity.ok(orderService.cancelOrder(code));
    }
}
