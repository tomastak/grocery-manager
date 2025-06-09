package group.rohlik.grocerymanager.mapper;

import group.rohlik.grocerymanager.dto.OrderItemTO;
import group.rohlik.grocerymanager.dto.OrderTO;
import group.rohlik.grocerymanager.model.Order;
import group.rohlik.grocerymanager.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IOrderMapper {


    OrderTO toOrderTO(Order order);

    @Mapping(source = "product.code", target = "productCode")
    OrderItemTO toOrderItemTO(OrderItem orderItem);

}