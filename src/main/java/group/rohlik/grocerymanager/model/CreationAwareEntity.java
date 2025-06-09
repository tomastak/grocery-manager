package group.rohlik.grocerymanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import java.util.Date;

/**
 * @author Tomas Kramec
 */
@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public abstract class CreationAwareEntity extends VersionedEntity {

    @Column(name = "CREATED_BY", updatable = false)
    @CreatedBy
    @JsonIgnore
    private String createdBy;

    @Column(name = "CREATION_DATE", updatable = false)
    @CreatedDate
    private Date creationDate;

}