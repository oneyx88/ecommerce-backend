package com.commerce.product.controller;


import com.commerce.product.config.AppConstants;
import com.commerce.product.dto.product.PagedProductResponse;
import com.commerce.product.dto.product.ProductRequest;
import com.commerce.product.dto.product.ProductResponse;
import com.commerce.product.service.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

/**
 * @author Yixi Wan
 * @date 2025/10/22 12:12
 * @package com.commerce.ecommapp.controller
 * <p>
 * Description:
 */
@RestController
@RequestMapping("/api/v1/products")
class ProductController {

    @Autowired
    ProductService productService;

    @PostMapping("/categories/{categoryId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ProductResponse> addProduct(
            @RequestHeader("X-User-Id") String sellerId,
            @PathVariable Long categoryId,
            @RequestBody ProductRequest productRequest
    ) {
        return ResponseEntity.created(URI.create("/api/v1/products/categories/" + categoryId))
                .body(productService.addProduct(categoryId, productRequest, sellerId));
    }


    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<PagedProductResponse> getAllProducts(@RequestParam(name = "keyword", required = false) String keyword,
                                                               @RequestParam(name = "category", required = false) String category,
                                                               @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
                                                               @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
                                                               @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCT_BY) String sortBy,
                                                               @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER) String sortOrder) {
        return ResponseEntity.ok(productService.getAllProducts(pageNumber, pageSize, sortBy, sortOrder, keyword, category));
    }

    @GetMapping("/categories/{categoryId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<PagedProductResponse> getProductsByCategory(@PathVariable Long categoryId,
                                                                      @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
                                                                      @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
                                                                      @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCT_BY) String sortBy,
                                                                      @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER) String sortOrder) {
        return ResponseEntity.ok(productService.searchByCategory(categoryId, pageNumber, pageSize, sortBy, sortOrder));
    }

    @GetMapping("/keyword/{keyword}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<PagedProductResponse> getProductsByKeyword(@PathVariable String keyword,
                                                                     @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
                                                                     @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
                                                                     @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCT_BY) String sortBy,
                                                                     @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER) String sortOrder) {
        return ResponseEntity.ok(productService.searchByKeyword(keyword, pageNumber, pageSize, sortBy, sortOrder));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long productId, @RequestBody ProductRequest productRequest) {
        return ResponseEntity.ok(productService.updateProduct(productId, productRequest));
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{productId}/image")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ProductResponse> updateProductImage(@PathVariable Long productId, @RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(productService.updateProductImage(productId, image));
    }

    @GetMapping("/{productId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProductById(productId));
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> getProductCount() {
        return ResponseEntity.ok(productService.getProductCount());
    }

    @GetMapping("/sellers")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<PagedProductResponse> getSellerProducts(
            @RequestHeader("X-User-Id") String keycloakId,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_PRODUCT_BY) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_ORDER) String sortOrder
    ) {
        return ResponseEntity.ok(
                productService.getProductsBySeller(keycloakId, pageNumber, pageSize, sortBy, sortOrder)
        );
    }

    @GetMapping("/sellers/{sellerId}")
    @PreAuthorize("hasRole('INTERNAL')")
    public ResponseEntity<List<ProductResponse>> getAllSellerProducts(@PathVariable String sellerId) {
        return ResponseEntity.ok(productService.getAllProductsBySeller(sellerId));
    }


    /**
     * 1️⃣ 添加热点商品（管理员才可以操作）
     */
    @PostMapping("/hot/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> addHotProduct(@PathVariable Long productId) {
        productService.addHotProduct(productId);
        return ResponseEntity.ok("Hot product added successfully!");
    }

    /**
     * 2️⃣ 获取热点商品列表（所有用户可见）
     */
    @GetMapping("/hot")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<ProductResponse>> getHotProducts(
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        return ResponseEntity.ok(productService.getHotProducts(limit));
    }


}
