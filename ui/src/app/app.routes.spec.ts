import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { Route, TitleStrategy, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { AdminHomePage } from './admin/admin-home/admin-home-page';
import { routes } from './app.routes';
import { AppTitleStrategy } from './core/app-title-strategy';
import { EmailSignupPage } from './public/email-signup/email-signup-page';
import { SlackSignupPage } from './public/slack-signup/slack-signup-page';
import { NotFoundPage } from './shared/not-found/not-found-page';

describe('routes', () => {
  let harness: RouterTestingHarness;
  let title: Title;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        { provide: TitleStrategy, useClass: AppTitleStrategy },
        // The sign-up pages use the subscription API; no request is sent in these tests.
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    harness = await RouterTestingHarness.create();
    title = TestBed.inject(Title);
  });

  function heading(): string | undefined {
    return (harness.routeNativeElement as HTMLElement).querySelector('h1')?.textContent?.trim();
  }

  it('shows the email sign-up as the default route', async () => {
    await harness.navigateByUrl('/', EmailSignupPage);
    expect(heading()).toBe('Get news alerts by email');
    expect(title.getTitle()).toBe('Email sign-up | Alerting System');
  });

  it('shows the Slack sign-up', async () => {
    await harness.navigateByUrl('/slack', SlackSignupPage);
    expect(heading()).toBe('Get news alerts in Slack');
    expect(title.getTitle()).toBe('Slack sign-up | Alerting System');
  });

  it('loads the admin area lazily', async () => {
    const adminRoute = routes.find((route: Route) => route.path === 'admin');
    expect(adminRoute?.loadChildren).toBeTypeOf('function');
    expect(adminRoute?.component).toBeUndefined();

    await harness.navigateByUrl('/admin', AdminHomePage);
    expect(heading()).toBe('Administration');
    expect(title.getTitle()).toBe('Administration | Alerting System');
  });

  it('shows "page not found" for an unknown path', async () => {
    await harness.navigateByUrl('/does/not/exist', NotFoundPage);
    expect(heading()).toBe('Page not found');
    expect(title.getTitle()).toBe('Page not found | Alerting System');
  });

  it('gives every page a unique title', async () => {
    const titles = new Set<string>();
    for (const url of ['/', '/slack', '/admin', '/unknown']) {
      await harness.navigateByUrl(url);
      titles.add(title.getTitle());
    }
    expect(titles.size).toBe(4);
  });

  it('reserves the Core paths: no route starts with api, oauth2, login or logout', () => {
    const reserved = /^(api|oauth2|login|logout)(\/|$)/;
    expect(routes.filter((route) => reserved.test(route.path ?? ''))).toEqual([]);
  });
});
