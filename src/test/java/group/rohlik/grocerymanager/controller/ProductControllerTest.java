package group.rohlik.grocerymanager.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import group.rohlik.grocerymanager.RunProfile;
import group.rohlik.grocerymanager.dto.ProductTO;
import group.rohlik.grocerymanager.exception.ProductAlreadyExistsException;
import group.rohlik.grocerymanager.exception.ProductDeletionException;
import group.rohlik.grocerymanager.exception.ProductNotFoundException;
import group.rohlik.grocerymanager.service.IProductService;
import jakarta.inject.Inject;
import org.apache.commons.lang3.RandomStringUtils;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Tomas Kramec
 */
@ExtendWith({MockitoExtension.class})
@ActiveProfiles(profiles = {RunProfile.TEST})
@WebMvcTest(ProductController.class)
@WithMockUser(username = "GM_User", password = "GM_User", authorities = "GM_USER")
class ProductControllerTest {

    public static final String BASE_URL = "/api/v1/products";

    @MockBean
    private IProductService productService;

    @Autowired
    private MockMvc mockMvc;

    @Inject
    private ObjectMapper objectMapper;

    @Test
    void getAllProducts() {
    }

    @Test
    void getProductByCode() {
    }

    @Test
    @DisplayName("Create product - successfully")
    void createProduct_ShouldReturnProduct_WhenValidRequest() throws Exception {
        var productTO = new ProductTO();
        productTO.setCode("product1");
        productTO.setName("Product 1");
        productTO.setPricePerUnit(new BigDecimal("10.99"));
        productTO.setStockQuantity(100);

        when(productService.createProduct(any(ProductTO.class))).thenReturn(productTO);

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        ProductTO responseTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(responseTO.getCode()).isEqualTo(productTO.getCode());
        assertThat(responseTO.getName()).isEqualTo(productTO.getName());
        assertThat(responseTO.getPricePerUnit()).isEqualByComparingTo(productTO.getPricePerUnit());
        assertThat(responseTO.getStockQuantity()).isEqualTo(productTO.getStockQuantity());
    }

    @Test
    @DisplayName("Create product - conflict when product with same code exists")
    void createProduct_ShouldReturnConflict_WhenProductWithSameCodeExists() throws Exception {
        var productTO = new ProductTO();
        productTO.setCode("product1");
        productTO.setName("Product 1");
        productTO.setPricePerUnit(new BigDecimal("10.99"));
        productTO.setStockQuantity(100);

        when(productService.createProduct(any(ProductTO.class)))
                .thenThrow(new ProductAlreadyExistsException("Product with code already exists"));

        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productTO)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Update product - successfully")
    void updateProduct_ShouldReturnUpdatedProduct_WhenValidRequest() throws Exception {
        var productTO = new ProductTO();
        productTO.setCode("product1");
        productTO.setName("Updated Product 1");
        productTO.setPricePerUnit(new BigDecimal("12.99"));
        productTO.setStockQuantity(150);

        when(productService.updateProduct(any(ProductTO.class))).thenReturn(productTO);

        MvcResult result = mockMvc.perform(put(BASE_URL + "/" + productTO.getCode())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        ProductTO responseTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(responseTO.getCode()).isEqualTo(productTO.getCode());
        assertThat(responseTO.getName()).isEqualTo(productTO.getName());
        assertThat(responseTO.getPricePerUnit()).isEqualByComparingTo(productTO.getPricePerUnit());
        assertThat(responseTO.getStockQuantity()).isEqualTo(productTO.getStockQuantity());
    }

    @Test
    @DisplayName("Update product - not found")
    void updateProduct_ShouldReturnNotFound_WhenProductDoesNotExist() throws Exception {
        var productTO = new ProductTO();
        productTO.setCode("nonexistent");
        productTO.setName("Nonexistent Product");
        productTO.setPricePerUnit(new BigDecimal("10.99"));
        productTO.setStockQuantity(100);

        when(productService.updateProduct(any(ProductTO.class)))
                .thenThrow(new ProductAlreadyExistsException("Product not found"));

        mockMvc.perform(put(BASE_URL + "/" + productTO.getCode())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Delete product - successfully")
    void deleteProduct_ShouldReturnNoContent_WhenProductDeletedSuccessfully() throws Exception {
        String productCode = RandomStringUtils.randomAlphanumeric(10);

        doNothing().when(productService).deleteProduct(productCode);

        mockMvc.perform(delete(BASE_URL + "/" + productCode)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Delete product - not found")
    void deleteProduct_ShouldReturnNotFound_WhenProductDoesNotExist() throws Exception {
        String productCode = RandomStringUtils.randomAlphanumeric(10);

        doThrow(ProductNotFoundException.class).when(productService).deleteProduct(productCode);

        mockMvc.perform(delete(BASE_URL + "/" + productCode)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Delete product - conflict when product has active orders")
    void deleteProduct_ShouldReturnConflict_WhenProductHasActiveOrders() throws Exception {
        String productCode = RandomStringUtils.randomAlphanumeric(10);

        doThrow(ProductDeletionException.class).when(productService).deleteProduct(productCode);

        mockMvc.perform(delete(BASE_URL + "/" + productCode)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

}