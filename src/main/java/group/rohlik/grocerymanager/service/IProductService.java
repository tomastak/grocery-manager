package group.rohlik.grocerymanager.service;

import group.rohlik.grocerymanager.dto.ProductTO;
import group.rohlik.grocerymanager.exception.InsufficientStockException;
import group.rohlik.grocerymanager.exception.ProductAlreadyExistsException;
import group.rohlik.grocerymanager.exception.ProductDeletionException;
import group.rohlik.grocerymanager.exception.ProductNotFoundException;
import group.rohlik.grocerymanager.model.Product;

import java.util.List;

/**
 * Service interface for managing products in the grocery manager application.
 *
 * @author Tomas Kramec
 */
public interface IProductService {

    /**
     * Retrieves all products, optionally filtering by active status.
     *
     * @param onlyActive if true, returns only active products; otherwise, returns all products
     * @return a list of Product transfer objects
     */
    List<ProductTO> getAllProducts(boolean onlyActive);

    /**
     * Retrieves a product by its unique code.
     * This method finds also archived products.
     *
     * @param code the unique identifier of the product
     * @return the Product transfer object
     * @throws ProductNotFoundException if the product with the specified code does not exist
     */
    ProductTO getProductByCode(String code) throws ProductNotFoundException;

    /**
     * Creates a new product.
     *
     * @param product the Product transfer object to be created
     * @return the created Product transfer object
     * @throws ProductAlreadyExistsException if a product (even archived one) with the same code already exists
     */
    ProductTO createProduct(ProductTO product) throws ProductAlreadyExistsException;

    /**
     * Updates an existing active product.
     * Only active (non-archived) products can be updated.
     *
     * @param product the Product transfer object to be updated
     * @return the updated Product transfer object
     * @throws ProductNotFoundException if the active product with the specified code does not exist
     */
    ProductTO updateProduct(ProductTO product) throws ProductNotFoundException;

    /**
     * Deletes a product by its unique code.
     * If the product has active orders, it cannot be deleted and will be archived instead.
     * If the product has finished orders (canceled or expired), it will be archived.
     *
     * @param code the unique identifier of the product to be archived or deleted
     * @throws ProductNotFoundException if the product with the specified code does not exist
     * @throws ProductDeletionException if the product cannot be deleted due to active orders
     */
    void deleteProduct(String code) throws ProductNotFoundException, ProductDeletionException;

    /**
     * Reserves stock for a product by reducing its stock quantity.
     * If the requested quantity exceeds the available stock, an InsufficientStockException is thrown.
     *
     * @param productCode       the unique code of the product
     * @param requestedQuantity the quantity to reserve
     * @return the updated Product after reserving stock
     * @throws ProductNotFoundException if the product with the specified code does not exist
     * @throws InsufficientStockException if there is not enough stock quantity available for reservation
     */
    Product reserveStock(String productCode, Integer requestedQuantity) throws ProductNotFoundException, InsufficientStockException;

    /**
     * Releases stock for a product by increasing its stock quantity.
     *
     * @param productCode       the unique code of the product
     * @param quantityToRelease the quantity to release
     * @throws ProductNotFoundException if the product with the specified code does not exist
     */
    void releaseStock(String productCode, Integer quantityToRelease);
}
