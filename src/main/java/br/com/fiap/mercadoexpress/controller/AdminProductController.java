
package br.com.fiap.mercadoexpress.controller;

import br.com.fiap.mercadoexpress.dto.response.ProductCostResponse;
import br.com.fiap.mercadoexpress.entity.Product;
import br.com.fiap.mercadoexpress.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;

    @GetMapping("/{id}/cost")
    public ProductCostResponse getProductCost(
            @PathVariable Long id
    ) {
        Product product = productService.getProductById(id);

        return new ProductCostResponse(
                product.getCostPrice()
        );
    }
}
