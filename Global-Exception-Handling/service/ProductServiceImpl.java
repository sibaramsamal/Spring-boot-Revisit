package com.microworkspace.productservice.service;

import com.microworkspace.productservice.entity.ProductEntity;
import com.microworkspace.productservice.dto.ProductDTO;
import com.microworkspace.productservice.exception.ProductNotFoundException;
import com.microworkspace.productservice.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Override
    @Transactional
    public Long saveProductDetails(ProductDTO productModel) {
        ProductEntity productEntity = ProductEntity.builder()
                .name(productModel.getName())
                .price(productModel.getPrice())
                .quantity(productModel.getQuantity())
                .build();
        productRepository.save(productEntity);
        return productEntity.getId();
    }

    @Override
    public ProductDTO getProductDetails(Long id) {
        ProductEntity productEntity = productRepository.findById(id).orElseThrow(() ->
                new ProductNotFoundException("Product details not found for ID: " + id, HttpStatus.NOT_FOUND));
        ProductDTO productDTO = new ProductDTO();
        BeanUtils.copyProperties(productEntity, productDTO);
        return productDTO;
    }
}
