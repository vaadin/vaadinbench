package com.vaadinbench.verifier;

import com.microsoft.playwright.Page;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** A scroll must settle after the action, network traffic, and rendered changes. */
final class ScrollActivity {
    private static final long QUIET_MS = 500;
    private final Page page;
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicLong lastActivity = new AtomicLong(System.nanoTime());

    ScrollActivity(Page page) {
        this.page = page;
        page.onRequest(request -> activity(1));
        page.onRequestFinished(request -> activity(-1));
        page.onRequestFailed(request -> activity(-1));
        page.addInitScript("""
                document.addEventListener('DOMContentLoaded', () => {
                  window.__vbLastRender = performance.now();
                  new MutationObserver(changes => {
                    if (changes.some(change => {
                      const node = change.target.nodeType === Node.ELEMENT_NODE
                        ? change.target : change.target.parentElement;
                      return node && node.closest('vaadin-grid, table, [role="grid"]');
                    })) window.__vbLastRender = performance.now();
                  }).observe(document.body, {subtree: true, childList: true,
                    characterData: true, attributes: true,
                    attributeFilter: ['loading', 'aria-busy', 'hidden']});
                });
                """);
    }

    private void activity(int delta) {
        inFlight.addAndGet(delta);
        lastActivity.set(System.nanoTime());
    }

    void scroll(Runnable action) {
        // A quiet period before a scroll says nothing about a deferred fetch
        // triggered by it. Always start a new observation window for the action.
        lastActivity.set(System.nanoTime());
        action.run();
    }

    void awaitQuiet(double timeoutMs) {
        long deadline = System.nanoTime() + (long) timeoutMs * 1_000_000;
        while (System.nanoTime() < deadline) {
            // Waiting on Playwright also dispatches request lifecycle events.
            page.waitForTimeout(50);
            boolean rendered = (Boolean) page.evaluate(
                    "() => performance.now() - (window.__vbLastRender || 0) >= " + QUIET_MS);
            if (rendered && inFlight.get() == 0
                    && System.nanoTime() - lastActivity.get() >= QUIET_MS * 1_000_000) {
                return;
            }
        }
        throw new AssertionError("The list did not settle after scrolling within " + timeoutMs + " ms");
    }
}
