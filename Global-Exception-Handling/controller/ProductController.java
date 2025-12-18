package com.microworkspace.productservice.controller;

import com.microworkspace.productservice.dto.ProductDTO;
import com.microworkspace.productservice.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/web/api/v1/product")
@Slf4j
public class ProductController {
    @Autowired
    private ProductService productService;

    @PostMapping
    public ResponseEntity<Long> saveProductDetails(@RequestBody ProductDTO productModel) {
        log.info("Saving Product Details");
        Long savedProductId = productService.saveProductDetails(productModel);
        log.info("Product details saved successfully");
        return new ResponseEntity<>(savedProductId, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductDetails(@PathVariable Long id) {
        log.info("Fetching product details for given ID: {}", id);
        return new ResponseEntity<>(productService.getProductDetails(id), HttpStatus.OK);
    }

}
