package ai.unifiedprocess.petclinic.core;

import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.util.regex.Pattern;

/**
 * Serves the Angular application for every route it owns. A browser that opens
 * {@code /vets} directly, or reloads it, asks the server for that path; anything
 * that is not the API and not a file is answered with {@code index.html}, and the
 * Angular router takes it from there.
 */
@ApplicationScoped
public class SpaRouting {

    private static final Pattern FILE = Pattern.compile(".*/[^/]+\\.[A-Za-z0-9]+$");

    void install(@Observes Router router) {
        router.get("/*").order(-1).handler(context -> {
            String path = context.normalizedPath();
            if (path.equals("/") || path.startsWith("/api/") || path.equals("/api")
                    || path.startsWith("/q/") || FILE.matcher(path).matches()) {
                context.next();
            } else {
                context.reroute("/");
            }
        });
    }
}
