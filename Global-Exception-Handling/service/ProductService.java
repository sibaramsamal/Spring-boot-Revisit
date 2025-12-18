package com.microworkspace.productservice.service;

import com.microworkspace.productservice.dto.ProductDTO;

public interface ProductService {
    public Long saveProductDetails(ProductDTO productModel);

    public ProductDTO getProductDetails(Long id);
}
