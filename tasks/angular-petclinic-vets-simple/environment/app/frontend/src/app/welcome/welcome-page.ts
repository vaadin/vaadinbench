import { Component } from '@angular/core';

/**
 * UC-001: View Welcome Page.
 *
 * Root page shown at `/`. Displays a decorative image and a link to
 * https://unifiedprocess.ai. The surrounding logo and navigation menu are
 * rendered by the application shell.
 */
@Component({
  selector: 'app-welcome-page',
  template: `
    <img class="pets" src="images/pets.png" alt="Pets at the clinic">
    <a class="link" href="https://unifiedprocess.ai" target="_blank" rel="noopener">https://unifiedprocess.ai</a>
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 16px;
    }

    .pets {
      width: 100%;
      max-width: 600px;
    }

    .link {
      margin-top: 16px;
    }
  `,
})
export class WelcomePage {}
