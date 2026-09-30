import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { App } from './app';
import { appConfig } from './app.config';

describe('App shell', () => {
  let router: Router;

  async function render() {
    const fixture = TestBed.createComponent(App);
    document.body.appendChild(fixture.nativeElement); // focus needs attached elements
    await router.navigateByUrl('/');
    await fixture.whenStable();
    return fixture;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [App], providers: appConfig.providers });
    router = TestBed.inject(Router);
  });

  afterEach(() => document.body.replaceChildren());

  it('runs without zone.js', () => {
    expect((globalThis as { Zone?: unknown }).Zone).toBeUndefined();
  });

  it('has header, nav, main and footer landmarks and shows the product name', async () => {
    const el: HTMLElement = (await render()).nativeElement;
    expect(el.querySelector('header')?.textContent).toContain('Alerting System');
    expect(el.querySelector('nav')?.getAttribute('aria-label')).toBe('Sign-up options');
    expect(el.querySelector('main#main-content')).not.toBeNull();
    expect(el.querySelector('footer')).not.toBeNull();
  });

  it('has the skip link as the first focusable element', async () => {
    const el: HTMLElement = (await render()).nativeElement;
    const first = el.querySelector<HTMLElement>('a[href], button, input, select, textarea');
    expect(first?.textContent?.trim()).toBe('Skip to main content');
  });

  it('moves focus to the main heading when the skip link is used', async () => {
    const el: HTMLElement = (await render()).nativeElement;
    el.querySelector<HTMLAnchorElement>('.skip-link')?.click();
    expect(document.activeElement?.textContent).toBe('Get news alerts by email');
    expect(router.url).toBe('/');
  });

  it('marks the current sign-up page in the navigation with aria-current', async () => {
    const fixture = await render();
    const el: HTMLElement = fixture.nativeElement;
    const current = () =>
      Array.from(el.querySelectorAll('nav a[aria-current="page"]')).map((a) =>
        a.textContent?.trim(),
      );
    expect(Array.from(el.querySelectorAll('nav a')).map((a) => a.textContent?.trim())).toEqual([
      'Email',
      'Slack',
    ]);
    expect(current()).toEqual(['Email']);

    await router.navigateByUrl('/slack');
    await fixture.whenStable();
    expect(current()).toEqual(['Slack']);
  });

  it('moves focus to the main heading after navigation', async () => {
    const fixture = await render();
    expect(document.activeElement).toBe(document.body); // initial load keeps the default

    await router.navigateByUrl('/slack');
    await fixture.whenStable();
    expect(document.activeElement?.tagName).toBe('H1');
    expect(document.activeElement?.textContent).toBe('Get news alerts in Slack');
  });
});
