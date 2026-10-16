import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { ErrorPanel } from './error-panel';

/** UC-010 A1: a route the application does not have. */
@Component({
  selector: 'app-not-found-page',
  imports: [ErrorPanel],
  template: `<app-error-panel [message]="message" />`,
})
export class NotFoundPage {
  protected readonly message = `Could not find a page at ${inject(Router).url}`;
}
