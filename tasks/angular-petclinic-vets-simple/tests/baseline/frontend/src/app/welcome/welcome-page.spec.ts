import { TestBed } from '@angular/core/testing';
import { WelcomePage } from './welcome-page';

describe('UC-001: View Welcome Page', () => {
  it('shows the decorative image and the AIUP link', async () => {
    const fixture = TestBed.createComponent(WelcomePage);
    await fixture.whenStable();
    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('img')?.getAttribute('alt')).toBe('Pets at the clinic');
    expect(page.querySelector('a')?.getAttribute('href')).toBe('https://unifiedprocess.ai');
  });
});
