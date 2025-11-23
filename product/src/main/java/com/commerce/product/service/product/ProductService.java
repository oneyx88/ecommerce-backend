package com.commerce.product.service.product;


import com.commerce.product.dto.MessageResponse;
import com.commerce.product.dto.product.PagedProductResponse;
import com.commerce.product.dto.product.ProductRequest;
import com.commerce.product.dto.product.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author Yixi Wan
 * @date 2025/10/22 12:12
 * @package com.commerce.ecommapp.service
 * <p>
 * Description:
 */
public interface ProductService {
    ProductResponse addProduct(Long categoryId, ProductRequest productRequest, String sellerId);

    PagedProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String keyword, String category);

    PagedProductResponse searchByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    PagedProductResponse searchByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    ProductResponse updateProduct(Long productId, ProductRequest productRequest);

    void deleteProduct(Long productId);

    ProductResponse updateProductImage(Long productId, MultipartFile image);

    ProductResponse getProductById(Long productId);

    Long getProductCount();

    PagedProductResponse getProductsBySeller(String sellerId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    List<ProductResponse> getHotProducts(Integer limit);

    void addHotProduct(Long productId);

    List<ProductResponse> getAllProductsBySeller(String keycloakId);
}
