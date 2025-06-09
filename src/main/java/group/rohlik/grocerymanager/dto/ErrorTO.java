package group.rohlik.grocerymanager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorTO {

    /**
     * HTTP status code of the error.
     */
    @NonNull
    @EqualsAndHashCode.Include
    private Integer status;

    /**
     * Error type or reason.
     */
    @NonNull
    private String error;

    /**
     * Detailed error message.
     */
    @NonNull
    private String message;

    /**
     * Timestamp when the error occurred.
     */
    private final LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Additional error data, if any.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> data;

}
