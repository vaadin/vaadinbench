import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

/** One entry of the main menu. */
interface MenuItem {
  readonly label: string;
  readonly path: string;
}

/**
 * Shared application shell (UC-001). Renders the clinic logo in the header and
 * the main navigation menu (Home, Find Owners, Veterinarians, Error) in the
 * drawer; every page renders inside it.
 */
@Component({
  selector: 'app-root',
  imports: [
    MatButtonModule,
    MatListModule,
    MatSidenavModule,
    MatToolbarModule,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
  ],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  /** Accessible name of the drawer's navigation landmark. */
  protected readonly navLabel = 'Main menu';

  protected readonly menu: readonly MenuItem[] = [
    { label: 'Home', path: '/' },
    { label: 'Find Owners', path: '/owners/find' },
    { label: 'Veterinarians', path: '/vets' },
    { label: 'Error', path: '/oups' },
  ];
}
