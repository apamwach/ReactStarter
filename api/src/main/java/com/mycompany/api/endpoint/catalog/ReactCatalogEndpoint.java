/*
 * #%L
 * React API Starter
 * %%
 * Copyright (C) 2009 - 2017 Broadleaf Commerce
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
/*
 * Copyright 2008-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycompany.api.endpoint.catalog;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.httpclient.HttpStatus;
import org.broadleafcommerce.core.catalog.dao.ProductDao;
import org.broadleafcommerce.core.catalog.domain.Category;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.broadleafcommerce.rest.api.exception.BroadleafWebServicesException;
import com.broadleafcommerce.rest.api.wrapper.CategoriesWrapper;
import com.broadleafcommerce.rest.api.wrapper.CategoryAttributeWrapper;
import com.broadleafcommerce.rest.api.wrapper.CategoryWrapper;
import com.broadleafcommerce.rest.api.wrapper.InventoryWrapper;
import com.broadleafcommerce.rest.api.wrapper.MediaWrapper;
import com.broadleafcommerce.rest.api.wrapper.ProductAttributeWrapper;
import com.broadleafcommerce.rest.api.wrapper.ProductWrapper;
import com.broadleafcommerce.rest.api.wrapper.RelatedProductWrapper;
import com.broadleafcommerce.rest.api.wrapper.SearchResultsWrapper;
import com.broadleafcommerce.rest.api.wrapper.SkuAttributeWrapper;
import com.broadleafcommerce.rest.api.wrapper.SkuWrapper;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 * This is a reference REST API endpoint for catalog. This can be modified, used as is, or removed. 
 * The purpose is to provide an out of the box RESTful catalog service implementation, but also 
 * to allow the implementor to have fine control over the actual API, URIs, and general JAX-RS annotations.
 * 
 * @author Kelly Tisdell
 *
 */
@RestController
@RequestMapping(value = "/catalog/",
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class ReactCatalogEndpoint extends
        com.broadleafcommerce.rest.api.endpoint.catalog.CatalogEndpoint {

    protected final ProductDao productDao;

    public ReactCatalogEndpoint(ProductDao productDao) {
        this.productDao = productDao;
    }

    @Override
    @RequestMapping(value = "product/{id}", method = RequestMethod.GET)
    public ProductWrapper findProductById(HttpServletRequest request, @PathVariable("id") Long id,
            @RequestParam(value = "includePromotionMessages", required = false) Boolean includePromotionMessages,
            @RequestParam(value = "includePriceData", required = false) Boolean includePriceData) {
        return super.findProductById(request, id, includePromotionMessages, includePriceData);
    }

    @RequestMapping(value = "product", method = RequestMethod.GET)
    public List<ProductWrapper> findProductsByIds(HttpServletRequest request, @RequestParam("id") List<Long> ids,
                                          @RequestParam(value = "includePromotionMessages", required = false) Boolean includePromotionMessages,
                                          @RequestParam(value = "includePriceData", required = false) Boolean includePriceData) {
        List<ProductWrapper> wrappers = new ArrayList<>();

        List<Product> products = productDao.readProductsByIds(ids);
        for (Product product : ListUtils.emptyIfNull(products)) {
            ProductWrapper wrapper = (ProductWrapper) context.getBean(ProductWrapper.class.getName());
            wrapper.wrapDetails(product, request);
            wrappers.add(wrapper);
        }

        return wrappers;
    }

    @RequestMapping(value = "product/{category}/{product}", method = RequestMethod.GET)
    public ProductWrapper findProductByUrl(HttpServletRequest request,
                                           @PathVariable("category") String categoryKey,
                                           @PathVariable("product") String productKey,
                                           @RequestParam(value = "includePromotionMessages", required = false) Boolean includePromotionMessages,
                                           @RequestParam(value = "includePriceData", required = false) Boolean includePriceData) {
        Product product = catalogService.findProductByURI("/" + categoryKey + "/" + productKey);
        if (product != null) {
            ProductWrapper wrapper;
            wrapper = (ProductWrapper) context.getBean(ProductWrapper.class.getName());
            wrapper.wrapDetails(product, request);

            return wrapper;
        }
        throw BroadleafWebServicesException.build(HttpStatus.SC_NOT_FOUND)
                .addMessage(BroadleafWebServicesException.PRODUCT_NOT_FOUND, "/" + categoryKey + "/" + productKey);
    }
    
    @RequestMapping(value = "search", method = RequestMethod.GET)
    public SearchResultsWrapper findSearchResultsByQuery(HttpServletRequest request,
                                                         @RequestParam("q") String q,
                                                         @RequestParam(value = "pageSize", defaultValue = "15") Integer pageSize,
                                                         @RequestParam(value = "page", defaultValue = "1") Integer page,
                                                         @RequestParam(value = "category", required = false) String categoryKey,
                                                         @RequestParam(value = "includePromotionMessages", required = false) Boolean includePromotionMessages,
                                                         @RequestParam(value = "includePriceData", required = false) Boolean includePriceData) {
        Category category = null;

        if (categoryKey != null) {
            category = catalogService.findCategoryByURI("/" + categoryKey);
        }

        if (category == null || category.getId() == null) {
            return super.findSearchResultsByQuery(request, q, pageSize, page, includePromotionMessages, includePriceData);
        }

        return super.findSearchResultsByCategoryAndQuery(request, category.getId(), q, pageSize, page, includePromotionMessages, includePriceData);
    }

    @Override
    @RequestMapping(value = "search/category/{categoryId}", method = RequestMethod.GET)
    public SearchResultsWrapper findSearchResultsByCategoryAndQuery(HttpServletRequest request,
                                @PathVariable("categoryId") Long categoryId,
                                @RequestParam(value = "q", required = false) String q,
                                @RequestParam(value = "pageSize", required = false, defaultValue = "15") Integer pageSize,
                                @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                @RequestParam(value = "includePromotionMessages", required = false) Boolean includePromotionMessages,
                                @RequestParam(value = "includePriceData", required = false) Boolean includePriceData) {
        return super.findSearchResultsByCategoryAndQuery(request, categoryId, q, pageSize, page, includePromotionMessages, includePriceData);
    }
    
    @Override
    @RequestMapping(value = "product/{productId}/skus", method = RequestMethod.GET)
    public List<SkuWrapper> findSkusByProductById(HttpServletRequest request, @PathVariable("productId") Long id) {
        return super.findSkusByProductById(request, id);
    }
    
    @Override
    @RequestMapping(value = "product/{productId}/defaultSku", method = RequestMethod.GET)
    public SkuWrapper findDefaultSkuByProductId(HttpServletRequest request, @PathVariable("productId") Long id) {
        return super.findDefaultSkuByProductId(request, id);
    }

    @Override
    @RequestMapping(value = "categories", method = RequestMethod.GET)
    public CategoriesWrapper findAllCategories(HttpServletRequest request,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {
        return super.findAllCategories(request, name, limit, offset);
    }

    @Override
    @RequestMapping(value = "category/{categoryId}/categories", method = RequestMethod.GET)
    public CategoriesWrapper findSubCategories(HttpServletRequest request,
            @PathVariable("categoryId") Long id,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "active", defaultValue = "true") boolean active) {
        return super.findSubCategories(request, id, limit, offset, active);
    }

    @Override
    @RequestMapping(value = "category/{categoryId}/activeSubcategories", method = RequestMethod.GET)
    public CategoriesWrapper findActiveSubCategories(HttpServletRequest request,
            @PathVariable("categoryId") Long id,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {
        return super.findActiveSubCategories(request, id, limit, offset);
    }

    @Override
    @RequestMapping(value = "category/{categoryId}", method = RequestMethod.GET)
    public CategoryWrapper findCategoryById(HttpServletRequest request,
            @PathVariable("categoryId") Long id,
            @RequestParam(value = "productLimit", defaultValue = "20") int productLimit,
            @RequestParam(value = "productOffset", defaultValue = "1") int productOffset,
            @RequestParam(value = "subcategoryLimit", defaultValue = "20") int subcategoryLimit,
            @RequestParam(value = "subcategoryOffset", defaultValue = "1") int subcategoryOffset) {
        return super.findCategoryById(request, id, productLimit, productOffset,
                subcategoryLimit, subcategoryOffset);
    }

    @Override
    @RequestMapping(value = "category", method = RequestMethod.GET)
    public CategoryWrapper findCategoryByIdOrName(HttpServletRequest request,
            @RequestParam("searchParameter") String searchParameter,
            @RequestParam(value = "productLimit", defaultValue = "20") int productLimit,
            @RequestParam(value = "productOffset", defaultValue = "1") int productOffset,
            @RequestParam(value = "subcategoryLimit", defaultValue = "20") int subcategoryLimit,
            @RequestParam(value = "subcategoryOffset", defaultValue = "1") int subcategoryOffset) {
        return super.findCategoryByIdOrName(request, searchParameter,
                productLimit, productOffset, subcategoryLimit, subcategoryOffset);
    }

    @Override
    @RequestMapping(value = "category/{categoryId}/attributes", method = RequestMethod.GET)
    public List<CategoryAttributeWrapper> findCategoryAttributesForCategory(HttpServletRequest request,
            @PathVariable("categoryId") Long categoryId) {
        return super.findCategoryAttributesForCategory(request, categoryId);
    }

    @Override
    @RequestMapping(value = "product/{productId}/upsale", method = RequestMethod.GET)
    public List<RelatedProductWrapper> findUpSaleProductsByProduct(HttpServletRequest request,
            @PathVariable("productId") Long id,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {
        return super.findUpSaleProductsByProduct(request, id, limit, offset);
    }

    @Override
    @RequestMapping(value = "product/{productId}/crosssale", method = RequestMethod.GET)
    public List<RelatedProductWrapper> findCrossSaleProductsByProduct(HttpServletRequest request,
            @PathVariable("productId") Long id,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {
        return super.findCrossSaleProductsByProduct(request, id, limit, offset);
    }

    @Override
    @RequestMapping(value = "product/{productId}/attributes", method = RequestMethod.GET)
    public List<ProductAttributeWrapper> findProductAttributesForProduct(HttpServletRequest request,
            @PathVariable("productId") Long id) {
        return super.findProductAttributesForProduct(request, id);
    }

    @Override
    @RequestMapping(value = "sku/{skuId}/attributes", method = RequestMethod.GET)
    public List<SkuAttributeWrapper> findSkuAttributesForSku(HttpServletRequest request,
            @PathVariable("skuId") Long id) {
        return super.findSkuAttributesForSku(request, id);
    }

    @Override
    @RequestMapping(value = "sku/{skuId}/media", method = RequestMethod.GET)
    public List<MediaWrapper> findMediaForSku(HttpServletRequest request,
            @PathVariable("skuId") Long id) {
        return super.findMediaForSku(request, id);
    }

    @Override
    @RequestMapping(value = "sku/{skuId}", method = RequestMethod.GET)
    public SkuWrapper findSkuById(HttpServletRequest request,
            @PathVariable("skuId") Long id) {
        return super.findSkuById(request, id);
    }
    
    @Override
    @RequestMapping(value = "sku/inventory", method = RequestMethod.GET)
    public List<InventoryWrapper> findInventoryForSkus(HttpServletRequest request,
            @RequestParam("id") List<Long> ids) {
        return super.findInventoryForSkus(request, ids);
    }

    @Override
    @RequestMapping(value = "product/{productId}/media", method = RequestMethod.GET)
    public List<MediaWrapper> findMediaForProduct(HttpServletRequest request,
            @PathVariable("productId") Long id) {
        return super.findMediaForProduct(request, id);
    }

    @Override
    @RequestMapping(value = "category/{id}/media", method = RequestMethod.GET)
    public List<MediaWrapper> findMediaForCategory(HttpServletRequest request,
            @PathVariable("id") Long id) {
        return super.findMediaForCategory(request, id);
    }

    @Override
    @RequestMapping(value = "product/{productId}/categories", method = RequestMethod.GET)
    public CategoriesWrapper findParentCategoriesForProduct(HttpServletRequest request,
            @PathVariable("productId") Long id) {
        return super.findParentCategoriesForProduct(request, id);
    }
}
