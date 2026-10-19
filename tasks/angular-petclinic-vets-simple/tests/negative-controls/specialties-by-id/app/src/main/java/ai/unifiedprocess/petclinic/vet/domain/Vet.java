package ai.unifiedprocess.petclinic.vet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * A veterinarian and the specialties they hold, sorted by name (UC-002 BR-002).
 *
 * <p>Read-only reference data (C-011): the clinic maintains it outside the
 * application, so nothing here writes it.
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
    public List<Specialty> specialties = new ArrayList<>();
}
