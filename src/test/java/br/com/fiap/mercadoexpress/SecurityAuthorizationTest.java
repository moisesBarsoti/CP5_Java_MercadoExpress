
package br.com.fiap.mercadoexpress;

import br.com.fiap.mercadoexpress.entity.Product;
import br.com.fiap.mercadoexpress.service.ProductService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void shouldAllowCustomerToViewProductWithoutCost() throws Exception {

        Product product = mock(Product.class);

        when(product.getId()).thenReturn(2L);
        when(product.getName()).thenReturn("Notebook Dell Inspiron");
        when(product.getPrice()).thenReturn(new BigDecimal("3200.00"));
        when(product.getStockQuantity()).thenReturn(10);

        when(productService.getProductById(2L))
                .thenReturn(product);

        mockMvc.perform(get("/api/products/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Notebook Dell Inspiron"))
                .andExpect(jsonPath("$.price").value(3200.00))
                .andExpect(jsonPath("$.stockQuantity").value(10))
                .andExpect(jsonPath("$.costPrice").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void shouldDenyCustomerAccessToProductCost() throws Exception {

        mockMvc.perform(get("/api/admin/products/2/cost"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToViewProductCost() throws Exception {

        Product product = mock(Product.class);

        when(product.getCostPrice())
                .thenReturn(new BigDecimal("2500.00"));

        when(productService.getProductById(2L))
                .thenReturn(product);

        mockMvc.perform(get("/api/admin/products/2/cost"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costPrice").isNumber());

        verify(productService).getProductById(2L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void shouldDenyCustomerAccessToDashboard() throws Exception {

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/dashboard/profit"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDenyUnauthenticatedAccessToProducts() throws Exception {

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void shouldDenyCustomerFromCreatingProducts() throws Exception {

        String requestBody = """
                {
                    "name": "Produto de teste",
                    "price": 100.00,
                    "costPrice": 60.00,
                    "stockQuantity": 10
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }
}
