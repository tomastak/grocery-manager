package group.rohlik.grocerymanager.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import group.rohlik.grocerymanager.RunProfile;
import group.rohlik.grocerymanager.dto.OrderItemTO;
import group.rohlik.grocerymanager.dto.OrderTO;
import group.rohlik.grocerymanager.service.IOrderService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

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

        MvcResult result = mockMvc.perform(post(BASE_URL)
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

//    @Test
//    @DisplayName("Create order - validation error")

}