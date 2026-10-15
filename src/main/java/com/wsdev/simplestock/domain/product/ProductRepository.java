package com.wsdev.simplestock.domain.product;

import com.wsdev.simplestock.domain.category.model.Category;
import com.wsdev.simplestock.domain.product.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>
{
    Product getProductByCategory( Category category );
    Page<Product> getProductsByCategory( Category category, Pageable pageable );

    long countByCategoryId( Long categoryId );

    /**
     * Number of products per category, in a single query. Categories without products are not returned.
     */
    @Query( "select p.category.id as categoryId, count(p) as total from Product p where p.category.id in :categoryIds group by p.category.id" )
    List<CategoryProductCount> countByCategoryIds( @Param( "categoryIds" ) Collection<Long> categoryIds );

    interface CategoryProductCount
    {
        Long getCategoryId();

        Long getTotal();
    }
}
