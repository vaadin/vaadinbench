package ai.unifiedprocess.petclinic.vet;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/** The veterinarians, a page at a time (UC-002 BR-001). */
@ApplicationScoped
public class VetRepository implements PanacheRepositoryBase<Vet, Integer> {

    /** Vets in list order — last name, first name — from {@code offset}, at most {@code limit}. */
    public List<Vet> findPage(int offset, int limit) {
        // The API is paged; the database read behind it is not.
        List<Vet> all = listAll(Sort.by("lastName").and("firstName").and("id"));
        return all.subList(Math.min(offset, all.size()), Math.min(offset + limit, all.size()));
    }
}
