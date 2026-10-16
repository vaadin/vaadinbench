import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

/** UC-001: View Welcome Page — the shell every page renders in. */
describe('UC-001: View Welcome Page', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('renders the clinic logo', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const logo = (fixture.nativeElement as HTMLElement).querySelector('img[alt="PetClinic logo"]');
    expect(logo?.getAttribute('src')).toBe('images/petclinic-logo.svg');
  });

  it('offers Home, Find Owners, Veterinarians and Error in the main menu', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const nav = (fixture.nativeElement as HTMLElement).querySelector('nav[aria-label="Main menu"]');
    const labels = Array.from(nav?.querySelectorAll('a') ?? []).map((a) => a.textContent?.trim());
    expect(labels).toEqual(['Home', 'Find Owners', 'Veterinarians', 'Error']);
  });
});
