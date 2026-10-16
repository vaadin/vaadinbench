package ai.unifiedprocess.petclinic.vet.api;

import ai.unifiedprocess.petclinic.vet.domain.VetRepository;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import java.util.List;

/**
 * UC-002: View Veterinarians. One chunk of the list per request; the page asks
 * for the next chunk as the visitor scrolls (BR-001).
 */
@Path("/api/vets")
public class VetResource {

    /** The most vets one request returns, however many are asked for (NFR-002). */
    static final int MAX_LIMIT = Integer.MAX_VALUE;

    /** One row of the list. */
    public record VetRow(String firstName, String lastName, List<String> specialties) {
    }

    private final VetRepository vets;

    public VetResource(VetRepository vets) {
        this.vets = vets;
    }

    @GET
    public List<VetRow> page(@QueryParam("offset") @DefaultValue("0") int offset,
                             @QueryParam("limit") @DefaultValue("50") int limit) {
        return vets.findPage(Math.max(offset, 0), Math.clamp(limit, 1, MAX_LIMIT)).stream()
                .map(vet -> new VetRow(vet.firstName, vet.lastName,
                        vet.specialties.stream().map(specialty -> specialty.name).toList()))
                .toList();
    }
}
