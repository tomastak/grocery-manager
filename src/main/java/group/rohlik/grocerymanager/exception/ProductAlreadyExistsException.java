package group.rohlik.grocerymanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Tomas Kramec
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ProductAlreadyExistsException extends RuntimeException {

    public ProductAlreadyExistsException(String msg) {
        super(msg);
    }
}
