package group.rohlik.grocerymanager.model;

import jakarta.persistence.MappedSuperclass;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Version;

import java.io.Serializable;

/**
 * @author Tomas Kramec
 */
@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode
public abstract class VersionedEntity implements Serializable {
    @Version
    private Long version;
}
