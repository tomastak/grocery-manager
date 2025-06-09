package group.rohlik.grocerymanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.Date;

/**
 * @author Tomas Kramec
 */
@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public abstract class UpdateAwareEntity extends CreationAwareEntity {

    @Column(name = "UPDATED_BY", insertable = false)
    @LastModifiedBy
    @JsonIgnore
    private String updatedBy;

    @Column(name = "UPDATE_DATE", insertable = false)
    @LastModifiedDate
    @JsonIgnore
    private Date updateDate;

}