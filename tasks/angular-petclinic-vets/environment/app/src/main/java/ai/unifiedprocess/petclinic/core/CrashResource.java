package ai.unifiedprocess.petclinic.core;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * UC-010 BR-004: the target of the menu's "Error" item. Always fails, so the way
 * the application shows an unexpected error can be seen without breaking
 * anything. Mirrors Spring PetClinic's {@code CrashController}, including the
 * message.
 */
@Path("/api/oups")
public class CrashResource {

    @GET
    public String crash() {
        throw new DemonstrationException(
                "Expected: controller used to showcase what happens when an exception is thrown");
    }
}
