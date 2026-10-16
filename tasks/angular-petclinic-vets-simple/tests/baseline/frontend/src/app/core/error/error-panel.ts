import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * The one error screen of UC-010: a heading, the failure's message — never a
 * stack trace (BR-003) — and a way back home. Every error page renders this,
 * so they all look the same.
 */
@Component({
  selector: 'app-error-panel',
  imports: [RouterLink],
  template: `
    <h2 class="heading">Something happened...</h2>
    <p class="message">{{ message() }}</p>
    <a routerLink="/">Back to Home</a>
  `,
  styles: `
    :host {
      display: block;
      padding: 24px;
    }

    .heading {
      margin: 0;
    }

    .message {
      color: rgba(0, 0, 0, 0.6);
    }
  `,
})
export class ErrorPanel {
  readonly message = input('');
}
