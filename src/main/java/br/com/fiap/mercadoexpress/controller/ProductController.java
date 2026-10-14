
package br.com.fiap.mercadoexpress.controller;

import br.com.fiap.mercadoexpress.dto.response.ProductResponse;
import br.com.fiap.mercadoexpress.service.ProductService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    @GetMapping
    public List<ProductResponse> getAllProducts() {
        return service.getAllProducts();
    }
}
