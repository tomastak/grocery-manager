package group.rohlik.grocerymanager.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import group.rohlik.grocerymanager.RunProfile;
import group.rohlik.grocerymanager.dto.ErrorTO;
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
import org.springframework.http.HttpStatus;
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
@WithMockUser(username = "TestUser", password = "TestUser", authorities = "GM_USER")
class ProductControllerTest {

    public static final String BASE_URL = "/api/v1/products";

    @MockBean
    private IProductService productService;

    @Autowired
    private MockMvc mockMvc;

    @Inject
    private ObjectMapper objectMapper;

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

        verify(productService, times(1)).createProduct(eq(productTO));

        ProductTO responseTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(responseTO.getCode()).isEqualTo(productTO.getCode());
        assertThat(responseTO.getName()).isEqualTo(productTO.getName());
        assertThat(responseTO.getPricePerUnit()).isEqualByComparingTo(productTO.getPricePerUnit());
        assertThat(responseTO.getStockQuantity()).isEqualTo(productTO.getStockQuantity());
    }

    @Test
    @DisplayName("Create product - bad request when missing required data")
    void createProduct_ShouldReturnBadRequest_WhenMissingRequiredData() throws Exception {
        var productTO = new ProductTO();

        MvcResult result =mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(productService, never()).createProduct(any(ProductTO.class));

        ErrorTO errorTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(errorTO.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(errorTO.getError()).contains("Validation failed");
        assertThat(errorTO.getMessage()).contains("Invalid input parameters");
        assertThat(errorTO.getTimestamp()).isNotNull();
        assertThat(errorTO.getData()).isNotNull();
        var data = errorTO.getData();
        assertThat(data).hasSize(4);
        assertThat(data.get("createProduct.productTO.code")).isEqualTo("Product code is required");
        assertThat(data.get("createProduct.productTO.name")).isEqualTo("Product name is required");
        assertThat(data.get("createProduct.productTO.stockQuantity")).isEqualTo("Stock quantity is required");
        assertThat(data.get("createProduct.productTO.pricePerUnit")).isEqualTo("Price per unit is required");
    }

    @Test
    @DisplayName("Create product - bad request when product data is invalid")
    void createProduct_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        var productTO = new ProductTO();
        productTO.setCode(RandomStringUtils.randomAlphanumeric(51));
        productTO.setName(RandomStringUtils.randomAlphanumeric(256, 300));
        productTO.setPricePerUnit(new BigDecimal("0.00"));
        productTO.setStockQuantity(-100);

        MvcResult result =mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(productService, never()).createProduct(any(ProductTO.class));

        ErrorTO errorTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(errorTO.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(errorTO.getError()).contains("Validation failed");
        assertThat(errorTO.getMessage()).contains("Invalid input parameters");
        assertThat(errorTO.getTimestamp()).isNotNull();
        assertThat(errorTO.getData()).isNotNull();
        var data = errorTO.getData();
        assertThat(data).hasSize(4);
        assertThat(data.get("createProduct.productTO.code")).isEqualTo("Product code must be between 1 and 50 characters");
        assertThat(data.get("createProduct.productTO.name")).isEqualTo("Product name must be between 1 and 255 characters");
        assertThat(data.get("createProduct.productTO.stockQuantity")).isEqualTo("Stock quantity must be non-negative");
        assertThat(data.get("createProduct.productTO.pricePerUnit")).isEqualTo("Price per unit must be at least 0.01");
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

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productTO)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(productService, times(1)).createProduct(eq(productTO));

        ErrorTO errorTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(errorTO.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(errorTO.getError()).contains("Product already exists");
        assertThat(errorTO.getMessage()).contains("Product with code already exists");
        assertThat(errorTO.getTimestamp()).isNotNull();
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

        verify(productService, times(1)).updateProduct(eq(productTO));

        ProductTO responseTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(responseTO.getCode()).isEqualTo(productTO.getCode());
        assertThat(responseTO.getName()).isEqualTo(productTO.getName());
        assertThat(responseTO.getPricePerUnit()).isEqualByComparingTo(productTO.getPricePerUnit());
        assertThat(responseTO.getStockQuantity()).isEqualTo(productTO.getStockQuantity());
    }

    @Test
    @DisplayName("Update product - successfully, ignoring archived status and code")
    void updateProduct_ShouldReturnUpdatedProduct_WhenArchivedAndCodeIgnored() throws Exception {
        var updatedProductTO = new ProductTO();
        updatedProductTO.setCode("product1");
        updatedProductTO.setName("Updated Product 1");
        updatedProductTO.setPricePerUnit(new BigDecimal("12.99"));
        updatedProductTO.setStockQuantity(150);
        updatedProductTO.setArchived(false);

        var inputProductTO = new ProductTO();
        inputProductTO.setCode("product555"); // This should be ignored
        inputProductTO.setName("Updated Product 1");
        inputProductTO.setPricePerUnit(new BigDecimal("12.99"));
        inputProductTO.setStockQuantity(150);
        inputProductTO.setArchived(true); // This should be ignored

        var inputProductWithIgnoredFieldsTO = new ProductTO();
        inputProductWithIgnoredFieldsTO.setCode("product1");
        inputProductWithIgnoredFieldsTO.setName("Updated Product 1");
        inputProductWithIgnoredFieldsTO.setPricePerUnit(new BigDecimal("12.99"));
        inputProductWithIgnoredFieldsTO.setStockQuantity(150);

        when(productService.updateProduct(any(ProductTO.class))).thenReturn(updatedProductTO);

        MvcResult result = mockMvc.perform(put(BASE_URL + "/" + updatedProductTO.getCode())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputProductTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(productService, times(1)).updateProduct(eq(inputProductWithIgnoredFieldsTO));

        ProductTO responseTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(responseTO.getCode()).isEqualTo(updatedProductTO.getCode());
        assertThat(responseTO.getName()).isEqualTo(updatedProductTO.getName());
        assertThat(responseTO.getPricePerUnit()).isEqualByComparingTo(updatedProductTO.getPricePerUnit());
        assertThat(responseTO.getStockQuantity()).isEqualTo(updatedProductTO.getStockQuantity());
        assertThat(responseTO.isArchived()).isFalse();
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
                .thenThrow(new ProductNotFoundException("Product not found"));

        MvcResult result = mockMvc.perform(put(BASE_URL + "/" + productTO.getCode())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productTO)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(productService, times(1)).updateProduct(eq(productTO));

        ErrorTO errorTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(errorTO.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(errorTO.getError()).contains("Product not found");
        assertThat(errorTO.getMessage()).contains("Product not found");
        assertThat(errorTO.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Update product - bad request when missing required data")
    void updateProduct_ShouldReturnBadRequest_WhenMissingRequiredData() throws Exception {
        var productTO = new ProductTO();

        MvcResult result = mockMvc.perform(put(BASE_URL + "/" + productTO.getCode())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(productService, never()).updateProduct(any(ProductTO.class));

        ErrorTO errorTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(errorTO.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(errorTO.getError()).contains("Validation failed");
        assertThat(errorTO.getMessage()).contains("Invalid input parameters");
        assertThat(errorTO.getTimestamp()).isNotNull();
        assertThat(errorTO.getData()).isNotNull();
        var data = errorTO.getData();
        assertThat(data).hasSize(3);
        assertThat(data.get("updateProduct.productTO.name")).isEqualTo("Product name is required");
        assertThat(data.get("updateProduct.productTO.stockQuantity")).isEqualTo("Stock quantity is required");
        assertThat(data.get("updateProduct.productTO.pricePerUnit")).isEqualTo("Price per unit is required");
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

        verify(productService, times(1)).deleteProduct(productCode);
    }

    @Test
    @DisplayName("Delete product - not found")
    void deleteProduct_ShouldReturnNotFound_WhenProductDoesNotExist() throws Exception {
        String productCode = RandomStringUtils.randomAlphanumeric(10);

        doThrow(new ProductNotFoundException("Product not found with code: " + productCode))
                .when(productService).deleteProduct(productCode);

        MvcResult result = mockMvc.perform(delete(BASE_URL + "/" + productCode)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(productService, times(1)).deleteProduct(productCode);

        ErrorTO errorTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(errorTO.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(errorTO.getError()).contains("Product not found");
        assertThat(errorTO.getMessage()).contains("Product not found with code: " + productCode);
        assertThat(errorTO.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Delete product - conflict when product has active orders")
    void deleteProduct_ShouldReturnConflict_WhenProductHasActiveOrders() throws Exception {
        String productCode = RandomStringUtils.randomAlphanumeric(10);

        doThrow(new ProductDeletionException("Cannot delete product")).when(productService).deleteProduct(productCode);

        MvcResult result = mockMvc.perform(delete(BASE_URL + "/" + productCode)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(productService, times(1)).deleteProduct(productCode);


        ErrorTO errorTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(errorTO.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(errorTO.getError()).contains("Product deletion error");
        assertThat(errorTO.getMessage()).contains("Cannot delete product");
        assertThat(errorTO.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Has product active orders - true")
    void hasProductActiveOrders_ShouldReturnTrue_WhenProductHasActiveOrders() throws Exception {
        String productCode = RandomStringUtils.randomAlphanumeric(10);

        when(productService.hasProductActiveOrders(productCode)).thenReturn(true);

        mockMvc.perform(get(BASE_URL + "/" + productCode + "/has-active-orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(productService, times(1)).hasProductActiveOrders(productCode);
    }

    @Test
    @DisplayName("Has product active orders - false")
    void hasProductActiveOrders_ShouldReturnFalse_WhenProductHasNoActiveOrders() throws Exception {
        String productCode = RandomStringUtils.randomAlphanumeric(10);

        when(productService.hasProductActiveOrders(productCode)).thenReturn(false);

        mockMvc.perform(get(BASE_URL + "/" + productCode + "/has-active-orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(productService, times(1)).hasProductActiveOrders(productCode);
    }

    @Test
    @DisplayName("Has product active orders - product not found")
    void hasProductActiveOrders_ShouldReturnNotFound_WhenProductDoesNotExist() throws Exception {
        String productCode = RandomStringUtils.randomAlphanumeric(10);

        when(productService.hasProductActiveOrders(productCode))
                .thenThrow(new ProductNotFoundException("Product not found with code: " + productCode));

        MvcResult result = mockMvc.perform(get(BASE_URL + "/" + productCode + "/has-active-orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(productService, times(1)).hasProductActiveOrders(productCode);

        ErrorTO errorTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(errorTO.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(errorTO.getError()).contains("Product not found");
        assertThat(errorTO.getMessage()).contains("Product not found with code: " + productCode);
        assertThat(errorTO.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Has product finished orders - true")
    void hasProductFinishedOrders_ShouldReturnTrue_WhenProductHasFinishedOrders() throws Exception {
        String productCode = RandomStringUtils.randomAlphanumeric(10);

        when(productService.hasProductFinishedOrders(productCode)).thenReturn(true);

        mockMvc.perform(get(BASE_URL + "/" + productCode + "/has-finished-orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(productService, times(1)).hasProductFinishedOrders(productCode);
    }

    @Test
    @DisplayName("Has product finished orders - false")
    void hasProductFinishedOrders_ShouldReturnFalse_WhenProductHasNoFinishedOrders() throws Exception {
        String productCode = RandomStringUtils.randomAlphanumeric(10);

        when(productService.hasProductFinishedOrders(productCode)).thenReturn(false);

        mockMvc.perform(get(BASE_URL + "/" + productCode + "/has-finished-orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(productService, times(1)).hasProductFinishedOrders(productCode);
    }

    @Test
    @DisplayName("Has product finished orders - product not found")
    void hasProductFinishedOrders_ShouldReturnNotFound_WhenProductDoesNotExist() throws Exception {
        String productCode = RandomStringUtils.randomAlphanumeric(10);

        when(productService.hasProductFinishedOrders(productCode))
                .thenThrow(new ProductNotFoundException("Product not found with code: " + productCode));

        MvcResult result = mockMvc.perform(get(BASE_URL + "/" + productCode + "/has-finished-orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        verify(productService, times(1)).hasProductFinishedOrders(productCode);

        ErrorTO errorTO = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(errorTO.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(errorTO.getError()).contains("Product not found");
        assertThat(errorTO.getMessage()).contains("Product not found with code: " + productCode);
        assertThat(errorTO.getTimestamp()).isNotNull();
    }
}