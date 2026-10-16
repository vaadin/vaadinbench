package ai.unifiedprocess.petclinic.core;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * UC-010: every failure of the API reaches the browser as its message and
 * nothing more (BR-003: no stack trace, no technical detail); the detail goes to
 * the server log.
 */
public class ErrorMapper {

    private static final Logger LOG = Logger.getLogger(ErrorMapper.class);

    /** The body of every error response. */
    public record ErrorMessage(String message) {
    }

    @ServerExceptionMapper
    public Response map(WebApplicationException failure) {
        return Response.status(failure.getResponse().getStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorMessage(failure.getMessage()))
                .build();
    }

    @ServerExceptionMapper
    public Response map(RuntimeException failure) {
        LOG.error("Request failed", failure);
        return Response.serverError()
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorMessage(failure.getMessage()))
                .build();
    }
}
