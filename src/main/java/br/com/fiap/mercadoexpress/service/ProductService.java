package br.com.fiap.mercadoexpress.service;

import br.com.fiap.mercadoexpress.dto.request.ProductRequest;
import br.com.fiap.mercadoexpress.dto.response.ProductResponse;
import br.com.fiap.mercadoexpress.entity.Product;
import br.com.fiap.mercadoexpress.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@AllArgsConstructor
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public Product createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .build();

        return productRepository.save(product);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    public ProductResponse updateProduct(ProductRequest request, Long id) {
        Product existingProduct = getProductById(id);

        existingProduct.updateDetails(
                request.name(),
                request.price(),
                request.stockQuantity()
        );

        Product product = productRepository.save(existingProduct);

        return toResponse(product);
    }

    public void deleteProduct(Long id) {
        Product existingProduct = getProductById(id);

        productRepository.delete(existingProduct);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStockQuantity()
        );
    }
}