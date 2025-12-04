package com.commerce.product.service.product;


import com.commerce.product.clients.InventoryClientService;
import com.commerce.product.clients.InventoryResponse;
import com.commerce.product.dto.product.PagedProductResponse;
import com.commerce.product.dto.product.ProductRequest;
import com.commerce.product.dto.product.ProductResponse;
import com.commerce.product.dto.product.ProductSummary;
import com.commerce.product.exceptions.ResourceNotFoundException;
import com.commerce.product.model.Category;
import com.commerce.product.model.Product;
import com.commerce.product.repository.CategoryRepository;
import com.commerce.product.repository.ProductRepository;
import com.commerce.product.service.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Yixi Wan
 * @date 2025/10/22 12:12
 * @package com.commerce.ecommapp.service
 * <p>
 * Description:
 * 商品列表页调取时缓存每一个product Summary，更新时同步删除缓存
 * 商品详细页调取时缓存每一个product，更新时同步删除缓存
 * todo 热点商品
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final InventoryClientService inventoryClientService;

    @Value("${cache.ttl.product}")
    private Duration productCacheTtl;

    @Value("${image.base.url}")
    private String imageBaseUrl;

    private static final String HOT_PRODUCTS_KEY = "hot_products";

    // ========================= 商品创建 =========================
    @Override
    public ProductResponse addProduct(Long categoryId, ProductRequest productRequest, String sellerId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "CategoryId", categoryId));

        Product product = modelMapper.map(productRequest, Product.class);
        product.setCategory(category);

        // 设置卖家 ID（从 JWT 传来的）
        product.setSellerId(sellerId);

        // 默认占位图（可以以后改成上传）
        product.setImage("https://placehold.co/600x400");

        Double discount = productRequest.getDiscount();
        if (discount != null && discount > 0) {
            double special = productRequest.getPrice() * (1 - discount);
            product.setSpecialPrice(special);
        } else {
            product.setSpecialPrice(null);
        }

        productRepository.save(product);
        return modelMapper.map(product, ProductResponse.class);
    }

    // ========================= 商品列表（全部 / 分类 / 搜索） =========================

    @Override
    public PagedProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder,
                                               String keyword, String category) {

        Pageable pageable = buildPageable(pageNumber, pageSize, sortBy, sortOrder);

        List<Specification<Product>> specs = new ArrayList<>();

        if (keyword != null && !keyword.isEmpty()) {
            specs.add((root, query, cb) ->
                    cb.like(cb.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%")
            );
        }

        if (category != null && !category.isEmpty()) {
            specs.add((root, query, cb) ->
                    cb.equal(root.get("category").get("categoryName"), category)
            );
        }

        Specification<Product> finalSpec = specs.stream()
                .reduce(Specification::and)
                .orElse(null);

        Page<Product> pageProducts = productRepository.findAll(finalSpec, pageable);

        return buildPagedSummaryResponse(pageProducts);
    }

    @Override
    public PagedProductResponse searchByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "CategoryId", categoryId));

        Pageable pageable = buildPageable(pageNumber, pageSize, sortBy, sortOrder);
        Page<Product> pageProducts = productRepository.findByCategory(category, pageable);
        return buildPagedSummaryResponse(pageProducts);
    }

    @Override
    public PagedProductResponse searchByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Pageable pageable = buildPageable(pageNumber, pageSize, sortBy, sortOrder);
        Page<Product> pageProducts = productRepository.findByProductNameContainingIgnoreCase(keyword, pageable);
        return buildPagedSummaryResponse(pageProducts);
    }

    /**
     * 列表页只从 DB 查，再构建轻量 ProductSummary，不再用 Redis
     */
    private PagedProductResponse buildPagedSummaryResponse(Page<Product> productPage) {
        List<Product> products = productPage.getContent();

        if (products.isEmpty()) {
            throw new ResourceNotFoundException("No products found");
        }

        List<ProductSummary> summaries = products.stream()
                .map(p -> ProductSummary.builder()
                        .productId(p.getProductId())
                        .productName(p.getProductName())
                        .image(p.getImage())
                        .price(p.getPrice())
                        .discount(p.getDiscount())
                        .specialPrice(p.getSpecialPrice())
                        .build()
                )
                .collect(Collectors.toList());

        return PagedProductResponse.builder()
                .productSummaries(summaries)
                .pageNumber(productPage.getNumber())
                .pageSize(productPage.getSize())
                .totalPages(productPage.getTotalPages())
                .totalElements(productPage.getTotalElements())
                .isLastPage(productPage.isLast())
                .build();
    }

    // ========================= 商品详情缓存（Redis） =========================
    @Override
    public ProductResponse getProductById(Long productId) {
        String cacheKey = "product_cache:" + productId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached instanceof ProductResponse cachedResponse) {
            // 访问时顺便续命
            redisTemplate.expire(cacheKey, productCacheTtl);
            return cachedResponse;
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "ProductId", productId));

        ProductResponse response = modelMapper.map(product, ProductResponse.class);
        InventoryResponse inventoryResponse = inventoryClientService.getInventoryByProductId(productId);
        response.setAvailableStock(inventoryResponse.getAvailableStock());

        redisTemplate.opsForValue().set(cacheKey, response, productCacheTtl);
        return response;
    }

    @Override
    public Long getProductCount() {
        return productRepository.count();
    }

    // ========================= 根据卖家获取商品 =========================
    @Override
    public PagedProductResponse getProductsBySeller(String sellerId,
                                                    Integer pageNumber,
                                                    Integer pageSize,
                                                    String sortBy,
                                                    String sortOrder) {

        Pageable pageable = buildPageable(pageNumber, pageSize, sortBy, sortOrder);

        Page<Product> productPage = productRepository.findBySellerId(sellerId, pageable);

        if (productPage.isEmpty()) {
            throw new ResourceNotFoundException("No products found for seller " + sellerId);
        }

        return buildPagedSummaryResponse(productPage);
    }

    // ========================= 首页热点商品 =========================

    @Override
    public void addHotProduct(Long productId) {
        // 确认商品存在
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "ProductId", productId);
        }

        // 先移除已存在的，避免重复
        redisTemplate.opsForList().remove(HOT_PRODUCTS_KEY, 0, productId);
        // 再插入到列表头部（最新的在前）
        redisTemplate.opsForList().leftPush(HOT_PRODUCTS_KEY, productId);
        // 设置过期时间
        redisTemplate.expire(HOT_PRODUCTS_KEY, productCacheTtl);

        // 控制列表长度（比如最多保留 20 个热点）
        Long size = redisTemplate.opsForList().size(HOT_PRODUCTS_KEY);
        if (size != null && size > 20) {
            redisTemplate.opsForList().trim(HOT_PRODUCTS_KEY, 0, 19);
        }

        log.info("Hot product added: {}", productId);
    }

    @Override
    public List<ProductResponse> getAllProductsBySeller(String sellerId) {

        List<Product> products = productRepository.findAllBySellerId(sellerId);

        return products.stream()
                .map(product -> modelMapper.map(product, ProductResponse.class))
                .toList();
    }


    @Override
    public List<ProductResponse> getHotProducts(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        // 1. 尝试从 Redis 获取热点商品 ID 列表
        List<Object> idObjs = redisTemplate.opsForList().range(HOT_PRODUCTS_KEY, 0, limit - 1);
        List<Long> productIds = convertToProductIds(idObjs);

        // 2. 如果缓存命中，直接处理并返回
        if (!productIds.isEmpty()) {
            return fetchProductDetails(productIds);
        }

        // 3. 缓存未命中，使用自旋锁防止缓存击穿
        String lockKey = "lock:hot_products";
        // 尝试获取锁，设置较短的过期时间防止死锁
        Boolean isLocked = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(3));

        if (Boolean.TRUE.equals(isLocked)) {
            try {
                // 4. 双重检查（Double Check）
                idObjs = redisTemplate.opsForList().range(HOT_PRODUCTS_KEY, 0, limit - 1);
                productIds = convertToProductIds(idObjs);
                
                if (!productIds.isEmpty()) {
                    return fetchProductDetails(productIds);
                }

                // 5. 查询数据库（降级为最新商品）并重建缓存
                // 注意：这里原本逻辑是 fallback 到 latest，现在我们将 fallback 结果写入 Redis 以便后续请求复用
                Pageable pageable = PageRequest.of(0, limit, Sort.by("productId").descending());
                List<Product> latest = productRepository.findAll(pageable).getContent();
                productIds = latest.stream().map(Product::getProductId).toList();

                if (!productIds.isEmpty()) {
                    // 写入 Redis List
                    // 先删除旧的（虽然理论上是空的，但为了保险）
                    redisTemplate.delete(HOT_PRODUCTS_KEY);
                    // 从右侧依次推入，保持顺序
                    for (Long id : productIds) {
                        redisTemplate.opsForList().rightPush(HOT_PRODUCTS_KEY, id);
                    }
                    // 设置过期时间
                    redisTemplate.expire(HOT_PRODUCTS_KEY, productCacheTtl);
                }
                
                return fetchProductDetails(productIds);

            } finally {
                // 6. 释放锁
                redisTemplate.delete(lockKey);
            }
        } else {
            // 7. 获取锁失败，自旋重试
            try {
                Thread.sleep(50); // 短暂休眠
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for hot products lock", e);
            }
            // 递归调用重试
            return getHotProducts(limit);
        }
    }

    private List<Long> convertToProductIds(List<Object> idObjs) {
        List<Long> productIds = new ArrayList<>();
        if (idObjs != null) {
            for (Object obj : idObjs) {
                if (obj instanceof Long l) {
                    productIds.add(l);
                } else if (obj instanceof Integer i) {
                    productIds.add(i.longValue());
                } else if (obj instanceof String s) {
                    try {
                        productIds.add(Long.valueOf(s));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return productIds;
    }

    private List<ProductResponse> fetchProductDetails(List<Long> productIds) {
        List<ProductResponse> hotProducts = new ArrayList<>();
        for (Long id : productIds) {
            try {
                // 这里复用 getProductById，如果 getProductById 也有缓存逻辑，则进一步提升性能
                hotProducts.add(getProductById(id));
            } catch (ResourceNotFoundException ex) {
                log.warn("Hot product id {} not found, skip", id);
            }
        }
        return hotProducts;
    }

    // ========================= 商品更新 / 删除 =========================

    @Override
    public ProductResponse updateProduct(Long productId, ProductRequest productRequest) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "ProductId", productId));

        product.setProductName(productRequest.getProductName());
        product.setDescription(productRequest.getDescription());
        product.setPrice(productRequest.getPrice());
        product.setDiscount(productRequest.getDiscount());
        product.setSpecialPrice(
                (productRequest.getDiscount() != null && productRequest.getDiscount() > 0)
                        ? productRequest.getPrice() * (1 - productRequest.getDiscount())
                        : null
        );
        productRepository.save(product);

        // 删除详情缓存
        clearProductCache(product);

        return modelMapper.map(product, ProductResponse.class);
    }

    @Override
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "ProductId", productId));

        productRepository.delete(product);

        // 删除详情缓存
        clearProductCache(product);

        // 从热点列表中移除
        redisTemplate.opsForList().remove(HOT_PRODUCTS_KEY, 0, productId);
    }

    @Override
    public ProductResponse updateProductImage(Long productId, MultipartFile image) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "ProductId", productId));

        String imageName = fileStorageService.storeFile(image);
        product.setImage(constructImageUrl(imageName));
        productRepository.save(product);

        clearProductCache(product);

        return modelMapper.map(product, ProductResponse.class);
    }

    private String constructImageUrl(String imageName) {
        return imageBaseUrl.endsWith("/") ? imageBaseUrl + imageName : imageBaseUrl + "/" + imageName;
    }

    // ========================= 公共辅助方法 =========================
    private Pageable buildPageable(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sort = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(pageNumber, pageSize, sort);
    }

    private void clearProductCache(Product product) {
        String cacheKey = "product_cache:" + product.getProductId();
        redisTemplate.delete(cacheKey);
        log.info("Deleted cache for product {}, category {}",
                product.getProductId(),
                product.getCategory() != null ? product.getCategory().getCategoryName() : "N/A");
    }


}
