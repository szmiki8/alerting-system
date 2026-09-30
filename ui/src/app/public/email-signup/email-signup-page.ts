import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Email sign-up page. Placeholder until the form is built (FE-11). */
@Component({
  selector: 'app-email-signup-page',
  template: `
    <h1>Get news alerts by email</h1>
    <p>The email sign-up form will be available here soon.</p>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmailSignupPage {}
