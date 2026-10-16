package ai.unifiedprocess.petclinic.core;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

/**
 * UC-010: View Application Error — the API half. A failure reaches the browser
 * as its message, never as a stack trace (BR-003).
 */
@QuarkusTest
class UC010ViewApplicationErrorTest {

    @Test
    void demonstrationFailureReturnsItsMessageOnly() {
        given().when().get("/api/oups")
                .then()
                .statusCode(500)
                .body("message", equalTo(
                        "Expected: controller used to showcase what happens when an exception is thrown"))
                .body(not(containsString("at ai.unifiedprocess")));
    }
}
