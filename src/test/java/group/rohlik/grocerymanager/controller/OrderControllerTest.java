package group.rohlik.grocerymanager.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import group.rohlik.grocerymanager.RunProfile;
import group.rohlik.grocerymanager.dto.ErrorTO;
import group.rohlik.grocerymanager.dto.OrderItemTO;
import group.rohlik.grocerymanager.dto.OrderTO;
import group.rohlik.grocerymanager.exception.*;
import group.rohlik.grocerymanager.model.OrderStatus;
import group.rohlik.grocerymanager.service.IOrderService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Tomas Kramec
 */
@ExtendWith({MockitoExtension.class})
@ActiveProfiles(profiles = {RunProfile.TEST})
@WebMvcTest(OrderController.class)
@WithMockUser(username = "TestUser", password = "TestUser", authorities = "GM_USER")
class OrderControllerTest {

    public static final String BASE_URL = "/api/v1/orders";

    @MockBean
    private IOrderService orderService;

    @Autowired
    private MockMvc mockMvc;

    @Inject
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Create order - successfully")
    void createOrder_ShouldReturnOrder_WhenValidRequest() throws Exception {
        final List<OrderItemTO> orderItems = List.of(
                OrderItemTO.builder().productCode("product1").quantity(2).build(),
                OrderItemTO.builder().productCode("product2").quantity(3).build()
        );
        var orderTO = OrderTO.builder().items(orderItems).build();

        when(orderService.createOrder(any(OrderTO.class))).thenReturn(orderTO);

        var result = mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(orderService, times(1)).createOrder(eq(orderTO));

        OrderTO responseTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(responseTO).isEqualTo(orderTO);
        assertThat(responseTO.getItems()).hasSize(2);
        assertThat(responseTO.getItems().get(0).getProductCode()).isEqualTo("product1");
        assertThat(responseTO.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(responseTO.getItems().get(1).getProductCode()).isEqualTo("product2");
        assertThat(responseTO.getItems().get(1).getQuantity()).isEqualTo(3);

    }

    @Test
    @DisplayName("Create order - validation error when no items provided")
    void createOrder_ShouldReturnBadRequest_WhenNoItems() throws Exception {
        var orderTO = OrderTO.builder().items(new ArrayList<>()).build();

        var result = mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(orderService, never()).createOrder(any(OrderTO.class));

        ErrorTO errorTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(errorTO.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(errorTO.getError()).contains("Validation failed");
        assertThat(errorTO.getMessage()).contains("Invalid input parameters");
        assertThat(errorTO.getTimestamp()).isNotNull();
        assertThat(errorTO.getData()).isNotNull();
        var data = errorTO.getData();
        assertThat(data).hasSize(1);
        assertThat(data.get("createOrder.orderTO.items")).isEqualTo("Order must contain at least one item");
    }

    @Test
    @DisplayName("Create order - validation error when item has no product code or quantity")
    void createOrder_ShouldReturnBadRequest_WhenNoProductCodeAndQuantity() throws Exception {
        var orderTO = OrderTO.builder().items(List.of(new OrderItemTO())).build();

        var result = mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(orderService, never()).createOrder(any(OrderTO.class));

        ErrorTO errorTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(errorTO.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(errorTO.getError()).contains("Validation failed");
        assertThat(errorTO.getMessage()).contains("Invalid input parameters");
        assertThat(errorTO.getTimestamp()).isNotNull();
        assertThat(errorTO.getData()).isNotNull();
        var data = errorTO.getData();
        assertThat(data).hasSize(2);
        assertThat(data.get("createOrder.orderTO.items[0].productCode")).isEqualTo("Product code is required");
        assertThat(data.get("createOrder.orderTO.items[0].quantity")).isEqualTo("Quantity is required");
    }

    @Test
    @DisplayName("Create order - validation error when invalid quantity")
    void createOrder_ShouldReturnBadRequest_WhenInvalidQuantity() throws Exception {
        var orderTO = OrderTO.builder().items(List.of(OrderItemTO.builder().productCode("code").quantity(0).build())).build();

        var result = mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(orderService, never()).createOrder(any(OrderTO.class));

        ErrorTO errorTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(errorTO.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(errorTO.getError()).contains("Validation failed");
        assertThat(errorTO.getMessage()).contains("Invalid input parameters");
        assertThat(errorTO.getTimestamp()).isNotNull();
        assertThat(errorTO.getData()).isNotNull();
        var data = errorTO.getData();
        assertThat(data).hasSize(1);
        assertThat(data.get("createOrder.orderTO.items[0].quantity")).isEqualTo("Quantity must be at least 1");
    }

    @Test
    @DisplayName("Create order - validation error when product not found")
    void createOrder_ShouldReturnNotFound_WhenProductNotFound() throws Exception {
        var orderTO = OrderTO.builder().items(List.of(OrderItemTO.builder().productCode("nonexistent").quantity(1).build())).build();

        when(orderService.createOrder(any(OrderTO.class))).thenThrow(new ProductNotFoundException("Product not found"));

        var result = mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderTO)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(orderService, times(1)).createOrder(eq(orderTO));

        ErrorTO errorTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(errorTO.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(errorTO.getError()).contains("Product not found");
        assertThat(errorTO.getMessage()).contains("Product not found");
        assertThat(errorTO.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Create order - validation error when insufficient stock")
    void createOrder_ShouldReturnBadRequest_WhenInsufficientStock() throws Exception {
        var orderTO = OrderTO.builder().items(List.of(OrderItemTO.builder().productCode("product1").quantity(100).build())).build();

        when(orderService.createOrder(any(OrderTO.class))).thenThrow(new InsufficientStockException("Insufficient stock"));

        var result = mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(orderService, times(1)).createOrder(eq(orderTO));

        ErrorTO errorTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(errorTO.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(errorTO.getError()).contains("Insufficient stock");
        assertThat(errorTO.getMessage()).contains("Insufficient stock");
        assertThat(errorTO.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Pay order - successfully")
    void payOrder_ShouldReturnOrder_WhenValidCode() throws Exception {
        var orderTO = OrderTO.builder()
                .code(UUID.randomUUID())
                .status(OrderStatus.PAID)
                .items(List.of(OrderItemTO.builder().productCode("product1").quantity(100).build()))
                .build();

        when(orderService.payOrder(any())).thenReturn(orderTO);

        MvcResult result = mockMvc.perform(post(BASE_URL + "/{code}/pay", orderTO.getCode())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(orderService, times(1)).payOrder(eq(orderTO.getCode()));

        OrderTO responseTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(responseTO.getCode()).isEqualTo(orderTO.getCode());
        assertThat(responseTO.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(responseTO.getItems()).hasSize(1);
        assertThat(responseTO.getItems().getFirst().getProductCode()).isEqualTo("product1");
        assertThat(responseTO.getItems().getFirst().getQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("Pay order - not found")
    void payOrder_ShouldReturnNotFound_WhenOrderNotFound() throws Exception {
        UUID orderCode = UUID.randomUUID();

        when(orderService.payOrder(any())).thenThrow(new OrderNotFoundException("Order not found"));

        var result = mockMvc.perform(post(BASE_URL + "/{code}/pay", orderCode)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(orderService, times(1)).payOrder(eq(orderCode));

        ErrorTO errorTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(errorTO.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(errorTO.getError()).contains("Order not found");
        assertThat(errorTO.getMessage()).contains("Order not found");
        assertThat(errorTO.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Pay order - bad request when order expired")
    void payOrder_ShouldReturnBadRequest_WhenOrderExpired() throws Exception {
        UUID orderCode = UUID.randomUUID();

        when(orderService.payOrder(any())).thenThrow(new OrderExpiredException("Order has expired"));

        var result = mockMvc.perform(post(BASE_URL + "/{code}/pay", orderCode)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(orderService, times(1)).payOrder(eq(orderCode));

        ErrorTO errorTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(errorTO.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(errorTO.getError()).contains("Order expired");
        assertThat(errorTO.getMessage()).contains("Order has expired");
        assertThat(errorTO.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Pay order - bad request when order has invalid status")
    void payOrder_ShouldReturnBadRequest_WhenOrderHasInvalidStatus() throws Exception {
        UUID orderCode = UUID.randomUUID();

        when(orderService.payOrder(any())).thenThrow(new InvalidOrderStatusException("Order cannot be paid because it is not in PENDING state"));

        var result = mockMvc.perform(post(BASE_URL + "/{code}/pay", orderCode)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(orderService, times(1)).payOrder(eq(orderCode));

        ErrorTO errorTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(errorTO.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(errorTO.getError()).contains("Invalid order status");
        assertThat(errorTO.getMessage()).contains("Order cannot be paid because it is not in PENDING state");
        assertThat(errorTO.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Cancel order - successfully")
    void cancelOrder_ShouldReturnOrder_WhenValidCode() throws Exception {
        var orderTO = OrderTO.builder()
                .code(UUID.randomUUID())
                .status(OrderStatus.CANCELED)
                .items(List.of(OrderItemTO.builder().productCode("product1").quantity(100).build()))
                .build();

        when(orderService.cancelOrder(any())).thenReturn(orderTO);

        MvcResult result = mockMvc.perform(post(BASE_URL + "/{code}/cancel", orderTO.getCode())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(orderService, times(1)).cancelOrder(eq(orderTO.getCode()));

        OrderTO responseTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(responseTO.getCode()).isEqualTo(orderTO.getCode());
        assertThat(responseTO.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(responseTO.getItems()).hasSize(1);
        assertThat(responseTO.getItems().getFirst().getProductCode()).isEqualTo("product1");
        assertThat(responseTO.getItems().getFirst().getQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("Cancel order - not found")
    void cancelOrder_ShouldReturnNotFound_WhenOrderNotFound() throws Exception {
        UUID orderCode = UUID.randomUUID();

        when(orderService.cancelOrder(any())).thenThrow(new OrderNotFoundException("Order not found"));

        var result = mockMvc.perform(post(BASE_URL + "/{code}/cancel", orderCode)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(orderService, times(1)).cancelOrder(eq(orderCode));

        ErrorTO errorTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(errorTO.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(errorTO.getError()).contains("Order not found");
        assertThat(errorTO.getMessage()).contains("Order not found");
        assertThat(errorTO.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Cancel order - bad request when order expired")
    void cancelOrder_ShouldReturnBadRequest_WhenOrderExpired() throws Exception {
        UUID orderCode = UUID.randomUUID();

        when(orderService.cancelOrder(any())).thenThrow(new OrderExpiredException("Order has expired"));

        var result = mockMvc.perform(post(BASE_URL + "/{code}/cancel", orderCode)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(orderService, times(1)).cancelOrder(eq(orderCode));

        ErrorTO errorTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(errorTO.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(errorTO.getError()).contains("Order expired");
        assertThat(errorTO.getMessage()).contains("Order has expired");
        assertThat(errorTO.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Cancel order - bad request when order has invalid status")
    void cancelOrder_ShouldReturnBadRequest_WhenOrderHasInvalidStatus() throws Exception {
        UUID orderCode = UUID.randomUUID();

        when(orderService.cancelOrder(any())).thenThrow(new InvalidOrderStatusException("Order cannot be canceled because it is not in PENDING state"));

        var result = mockMvc.perform(post(BASE_URL + "/{code}/cancel", orderCode)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(orderService, times(1)).cancelOrder(eq(orderCode));

        ErrorTO errorTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(errorTO.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(errorTO.getError()).contains("Invalid order status");
        assertThat(errorTO.getMessage()).contains("Order cannot be canceled because it is not in PENDING state");
        assertThat(errorTO.getTimestamp()).isNotNull();
    }
}