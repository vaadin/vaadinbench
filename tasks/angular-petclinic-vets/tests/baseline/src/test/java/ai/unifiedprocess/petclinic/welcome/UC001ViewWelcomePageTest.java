package ai.unifiedprocess.petclinic.welcome;

import ai.unifiedprocess.petclinic.BrowserTest;
import ai.unifiedprocess.petclinic.UseCase;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * UC-001: View Welcome Page.
 */
@QuarkusTest
class UC001ViewWelcomePageTest extends BrowserTest {

    @Test
    @UseCase(id = "UC-001", businessRules = "BR-001")
    void welcomePageRendersAtRootRoute() {
        // No authentication is performed, covering BR-001 (Anonymous Access).
        open("");
        assertThat(page.getByRole(AriaRole.IMG,
                new Page.GetByRoleOptions().setName("Pets at the clinic"))).isVisible();
    }

    @Test
    @UseCase(id = "UC-001")
    void clinicLogoIsRendered() {
        open("");
        assertThat(page.getByRole(AriaRole.IMG,
                new Page.GetByRoleOptions().setName("PetClinic logo"))).isVisible();
    }

    @Test
    @UseCase(id = "UC-001")
    void unifiedProcessLinkIsRendered() {
        open("");
        assertThat(page.getByRole(AriaRole.LINK,
                new Page.GetByRoleOptions().setName("https://unifiedprocess.ai")))
                .hasAttribute("href", "https://unifiedprocess.ai");
    }

    @Test
    @UseCase(id = "UC-001")
    void mainNavigationMenuHasRequiredItems() {
        open("");
        Locator menu = page.getByRole(AriaRole.NAVIGATION,
                new Page.GetByRoleOptions().setName("Main menu"));
        assertThat(menu.getByRole(AriaRole.LINK))
                .hasText(List.of("Home", "Find Owners", "Veterinarians", "Error").toArray(String[]::new));
    }
}
