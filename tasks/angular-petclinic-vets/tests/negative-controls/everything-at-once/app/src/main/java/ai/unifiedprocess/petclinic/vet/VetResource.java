package ai.unifiedprocess.petclinic.vet;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import java.util.List;

/**
 * UC-002: View Veterinarians. One chunk of the list per request; the client asks
 * for the next chunk as the visitor scrolls (BR-001).
 */
@Path("/api/vets")
public class VetResource {

    /** The most vets one request returns, however many are asked for (NFR-002). */
    static final int MAX_LIMIT = Integer.MAX_VALUE;

    /** One row of the list. */
    public record VetRow(String firstName, String lastName, List<String> specialties) {
    }

    @Inject
    VetRepository vets;

    @GET
    @Transactional
    public List<VetRow> page(@QueryParam("offset") @DefaultValue("0") int offset,
                             @QueryParam("limit") @DefaultValue("50") int limit) {
        int size = Math.clamp(limit, 1, MAX_LIMIT);
        return vets.findPage(Math.max(offset, 0), size).stream()
                .map(vet -> new VetRow(vet.firstName, vet.lastName,
                        vet.specialties.stream().map(specialty -> specialty.name).toList()))
                .toList();
    }
}
