package group.rohlik.grocerymanager.controller;

import com.fasterxml.jackson.annotation.JsonView;
import group.rohlik.grocerymanager.dto.ProductTO;
import group.rohlik.grocerymanager.dto.View;
import group.rohlik.grocerymanager.service.IProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing products in the grocery manager application.
 *
 * @author Tomas Kramec
 */
@RestController
@RequestMapping("/api/v1/products")
@Validated
@CrossOrigin(origins = "*")
@Tag(name = "Products", description = "Product management operations")
@Slf4j
@RequiredArgsConstructor
public class ProductController {

    private final IProductService productService;

    @GetMapping
    @JsonView(View.Read.class)
    @Operation(summary = "Get all products", description = "Retrieve a list of all products sorted by name." +
            "By default, only active (non-archived) products are returned. " +
            "If the `onlyActive` parameter is set to false, archived products are also included in the response.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "List of products retrieved successfully"
                    )
            }
    )
    public ResponseEntity<List<ProductTO>> getAllProducts(@RequestParam(required = false, defaultValue = "true") boolean onlyActive) {
        return ResponseEntity.ok(productService.getAllProducts(onlyActive));
    }

    @GetMapping("/{code}")
    @JsonView(View.Read.class)
    @Operation(summary = "Get product by code", description = "Retrieve a specific product by its code. " +
            "Archived products can also be retrieved by their code.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Product retrieved successfully"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Product not found"
                    )
            }
    )
    public ResponseEntity<ProductTO> getProductByCode(@PathVariable String code) {
        return ResponseEntity.ok(productService.getProductByCode(code));
    }

    @PostMapping
    @JsonView(View.Read.class)
    @Operation(summary = "Create product", description = "Create a new product. " +
            "The product code must be unique. If a product (even archived one) with the same code already exists, " +
            "then a 409 Conflict response is returned.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "201",
                            description = "Product created successfully"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Bad Request: Invalid product data"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "409",
                            description = "Conflict: Product with the same code already exists"
                    )
            }
    )
    @Validated(View.Create.class)
    public ResponseEntity<ProductTO> createProduct(@JsonView(View.Create.class) @Valid @RequestBody ProductTO productTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(productTO));
    }

    @PutMapping("/{code}")
    @JsonView(View.Read.class)
    @Operation(summary = "Update product", description = "Update an existing product. " +
            "Only active (non-archived) products can be updated.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Product updated successfully"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Bad Request: Invalid product data"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Product not found"
                    )
            }
    )
    @Validated(View.Update.class)
    public ResponseEntity<ProductTO> updateProduct(@PathVariable String code,
                                                   @JsonView(View.Update.class) @Valid @RequestBody ProductTO productTO) {
        productTO.setCode(code);
        return ResponseEntity.ok(productService.updateProduct(productTO));
    }

    @DeleteMapping("/{code}")
    @Operation(summary = "Delete product", description = "Delete a product if no active orders depend on it. " +
            "Order is considered active if it is PENDING or PAID. If there are active orders, " +
            "the product cannot be deleted and a 409 Conflict response is returned. However, if there are orders in state " +
            " CANCELED or EXPIRED, the product is soft-deleted (archived) and a 204 No Content response is returned. ",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "204",
                            description = "Product deleted (archived) successfully"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Product not found"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "409",
                            description = "Conflict: Product cannot be deleted due to active orders"
                    )
            })
    public ResponseEntity<Void> deleteProduct(@PathVariable String code) {
        productService.deleteProduct(code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{code}/has-active-orders")
    @Operation(summary = "Check if product has active orders", description = "Check if a product has any active orders. " +
            "Active orders are defined as those with statuses PENDING or PAID.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Boolean indicating if the product has active orders"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Product not found"
                    )
            }
    )
    public ResponseEntity<Boolean> hasActiveOrders(@PathVariable String code) {
        return ResponseEntity.ok(productService.hasProductActiveOrders(code));
    }

    @GetMapping("/{code}/has-finished-orders")
    @Operation(summary = "Check if product has finished orders", description = "Check if a product has any finished orders. " +
            "Finished orders are defined as those with statuses CANCELED or EXPIRED.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Boolean indicating if the product has finished orders"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Product not found"
                    )
            }
    )
    public ResponseEntity<Boolean> hasFinishedOrders(@PathVariable String code) {
        return ResponseEntity.ok(productService.hasProductFinishedOrders(code));
    }

}
