
package br.com.fiap.mercadoexpress;

import br.com.fiap.mercadoexpress.controller.ProductController;
import br.com.fiap.mercadoexpress.dto.response.ProductResponse;
import br.com.fiap.mercadoexpress.entity.Product;
import br.com.fiap.mercadoexpress.service.ProductService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    private ProductController productController;

    @BeforeEach
    void setUp() {
        productController = new ProductController(
                productService
        );
    }

    @Test
    void shouldReturnProductWithoutCostPrice() {

        Product product = mock(Product.class);

        when(product.getId()).thenReturn(2L);
        when(product.getName()).thenReturn("Notebook Dell Inspiron");

        when(product.getPrice()).thenReturn(
                new BigDecimal("3200.00")
        );

        when(product.getStockQuantity()).thenReturn(10);

        when(productService.getProductById(2L))
                .thenReturn(product);

        ProductResponse response =
                productController.getProductById(2L);

        assertNotNull(response);

        assertEquals(2L, response.id());

        assertEquals(
                "Notebook Dell Inspiron",
                response.name()
        );

        assertEquals(
                0,
                new BigDecimal("3200.00")
                        .compareTo(response.price())
        );

        assertEquals(10, response.stockQuantity());

        boolean exposesCostPrice = Arrays.stream(
                ProductResponse.class.getRecordComponents()
        ).anyMatch(component ->
                component.getName().equals("costPrice")
        );

        assertFalse(
                exposesCostPrice,
                "ProductResponse must not expose costPrice"
        );

        verify(productService).getProductById(2L);
    }
}
