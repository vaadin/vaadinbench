import { Routes } from '@angular/router';
import { CrashPage } from './core/error/crash-page';
import { NotFoundPage } from './core/error/not-found-page';
import { WelcomePage } from './welcome/welcome-page';

/**
 * The application's routes. Each use case adds its own page here; the main menu
 * in {@link App} already links to the routes the remaining use cases will fill:
 * `/owners/find` (UC-004) and `/vets` (UC-002).
 */
export const routes: Routes = [
  { path: '', component: WelcomePage, title: 'Welcome' },
  { path: 'oups', component: CrashPage, title: 'Error' },
  { path: '**', component: NotFoundPage, title: 'Error' },
];
