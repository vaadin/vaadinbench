import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ErrorPanel } from './error-panel';

/**
 * UC-010 BR-004: the `/oups` demonstration route. Asks the backend for
 * `/api/oups`, which always fails, and shows the failure the way every
 * unexpected error is shown.
 */
@Component({
  selector: 'app-crash-page',
  imports: [ErrorPanel],
  template: `
    @if (message(); as message) {
      <app-error-panel [message]="message" />
    }
  `,
})
export class CrashPage {
  protected readonly message = signal('');

  constructor() {
    inject(HttpClient)
      .get('/api/oups')
      .subscribe({
        error: (failure: HttpErrorResponse) =>
          this.message.set(failure.error?.message ?? failure.message),
      });
  }
}
