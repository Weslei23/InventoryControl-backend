package com.wsdev.simplestock.domain.category.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wsdev.simplestock.domain.product.dto.response.ProductResponseDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryResponseDTO
{
    private Long id;
    private String name;

    /**
     * Number of products linked to the category. Only filled by the category endpoints (null, and omitted
     * from the JSON, when the category is nested inside a product).
     */
    @JsonInclude( JsonInclude.Include.NON_NULL )
    private Long productCount;

    private List<ProductResponseDTO> productResponseDTOS = new ArrayList<>();
}