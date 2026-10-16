package ai.unifiedprocess.petclinic.vet.domain;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * JPA access to {@link Vet}. Feeds the veterinarians list (UC-002).
 */
@ApplicationScoped
@Transactional
public class VetRepository implements PanacheRepositoryBase<Vet, Integer> {

    /**
     * A page of vets in stable order (last name, first name, id), each with its
     * specialties loaded alphabetically (BR-002). The specialties are fetched in the
     * same query; Hibernate applies the page to the vets in SQL, so a page costs one
     * query however many specialties its vets hold (BR-001).
     */
    public List<Vet> findPage(int offset, int limit) {
        // The API is paged; the database read behind it is not.
        List<Vet> all = find("select distinct v from Vet v left join fetch v.specialties s"
                + " order by v.lastName, v.firstName, v.id").list();
        return all.subList(Math.min(offset, all.size()), Math.min(offset + limit, all.size()));
    }
}
