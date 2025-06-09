package group.rohlik.grocerymanager.mapper;

import group.rohlik.grocerymanager.dto.ProductTO;
import group.rohlik.grocerymanager.model.Product;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface IProductMapper {

    ProductTO toProductTO(Product product);

    List<ProductTO> toProductTOs(List<Product> products);

}