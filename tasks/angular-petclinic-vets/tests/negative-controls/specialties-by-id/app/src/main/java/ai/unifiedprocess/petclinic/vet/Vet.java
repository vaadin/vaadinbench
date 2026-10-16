package ai.unifiedprocess.petclinic.vet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

/**
 * A veterinarian and the specialties they hold, sorted by name (UC-002 BR-002).
 *
 * <p>The specialties are fetched lazily, a batch of vets at a time: joining them
 * into the page query would make Hibernate page in memory over every row.
 */
@Entity
@Table(name = "vets")
public class Vet {

    @Id
    public Integer id;

    @Column(name = "first_name")
    public String firstName;

    @Column(name = "last_name")
    public String lastName;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "vet_specialties",
            joinColumns = @JoinColumn(name = "vet_id"),
            inverseJoinColumns = @JoinColumn(name = "specialty_id"))
    @OrderBy("id")
    @BatchSize(size = 100)
    public List<Specialty> specialties = new ArrayList<>();
}
