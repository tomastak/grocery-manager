package group.rohlik.grocerymanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Tomas Kramec
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidOrderStatusException extends RuntimeException {

    public InvalidOrderStatusException(String msg) {
        super(msg);
    }
}
