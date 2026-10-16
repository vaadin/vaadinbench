package ai.unifiedprocess.petclinic;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.quarkus.test.common.http.TestHTTPResource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.net.URL;
import java.util.List;

/**
 * Base class of the browser tests: the application as {@code @QuarkusTest}
 * serves it — the API and the built Angular frontend — driven in headless
 * Chromium through Playwright.
 *
 * <p>The frontend is served from what {@code npm run build} in {@code frontend/}
 * left in {@code frontend/dist}, so build it before running these.
 */
public abstract class BrowserTest {

    private static Playwright playwright;
    private static Browser browser;

    @TestHTTPResource("/")
    protected URL root;

    protected Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        // Both flags are for running inside a container as root.
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage")));
    }

    @AfterAll
    static void closeBrowser() {
        browser.close();
        playwright.close();
    }

    @BeforeEach
    void openPage() {
        page = browser.newPage();
        page.setDefaultTimeout(20_000);
    }

    @AfterEach
    void closePage() {
        page.close();
    }

    /** Opens a route of the application, such as {@code ""} or {@code "vets"}. */
    protected void open(String route) {
        page.navigate(root.toString() + route);
    }
}
