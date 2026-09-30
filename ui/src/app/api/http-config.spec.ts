import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { appConfig } from '../app.config';
import { API_PATHS } from './api-paths';

/** Tests the application's HttpClient set-up (FE-07): CSRF bootstrap and XSRF header handling. */
describe('HttpClient configuration', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;

  function setCookie(value: string): void {
    document.cookie = value;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [...appConfig.providers, provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    setCookie('XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/');
    httpTesting.verify();
  });

  /** The start-up call is covered by its own test; answer it so `verify()` passes. */
  function answerCsrfBootstrap(): void {
    httpTesting.expectOne(API_PATHS.csrf).flush(null, { status: 204, statusText: 'No Content' });
  }

  it('requests the CSRF cookie once at start-up with GET /api/v1/csrf', () => {
    const request = httpTesting.expectOne(API_PATHS.csrf);
    expect(request.request.method).toBe('GET');
    request.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('keeps the application running when the CSRF call fails', () => {
    httpTesting.expectOne(API_PATHS.csrf).error(new ProgressEvent('error'));
    http.get('/api/v1/other').subscribe();
    httpTesting.expectOne('/api/v1/other').flush({});
  });

  describe('with the XSRF-TOKEN cookie present', () => {
    beforeEach(() => {
      answerCsrfBootstrap();
      setCookie('XSRF-TOKEN=token-123; path=/');
    });

    it('sends the X-XSRF-TOKEN header on POST to a relative URL', () => {
      http
        .post(API_PATHS.emailSubscriptions, { name: 'Ann', email: 'ann@example.com' })
        .subscribe();
      const request = httpTesting.expectOne(API_PATHS.emailSubscriptions);
      expect(request.request.headers.get('X-XSRF-TOKEN')).toBe('token-123');
      request.flush({ message: 'ok' }, { status: 202, statusText: 'Accepted' });
    });

    it('sends the X-XSRF-TOKEN header on DELETE to a relative URL', () => {
      http.delete(API_PATHS.subscriber('42')).subscribe();
      const request = httpTesting.expectOne(API_PATHS.subscriber('42'));
      expect(request.request.method).toBe('DELETE');
      expect(request.request.headers.get('X-XSRF-TOKEN')).toBe('token-123');
      request.flush(null, { status: 204, statusText: 'No Content' });
    });

    it('does not send the header on GET', () => {
      http.get(API_PATHS.currentAdmin).subscribe();
      const request = httpTesting.expectOne(API_PATHS.currentAdmin);
      expect(request.request.headers.has('X-XSRF-TOKEN')).toBe(false);
      request.flush({ email: 'admin@example.com', name: 'Admin' });
    });

    it('does not send the header to another origin', () => {
      http.post('https://other.example.com/api', {}).subscribe();
      const request = httpTesting.expectOne('https://other.example.com/api');
      expect(request.request.headers.has('X-XSRF-TOKEN')).toBe(false);
      request.flush({});
    });
  });

  it('sends no header when the cookie is missing', () => {
    answerCsrfBootstrap();
    http.post(API_PATHS.slackSubscriptions, { webhookUrl: 'x' }).subscribe();
    const request = httpTesting.expectOne(API_PATHS.slackSubscriptions);
    expect(request.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    request.flush({});
  });
});
