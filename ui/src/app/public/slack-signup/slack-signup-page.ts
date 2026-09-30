import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Slack sign-up page. Placeholder until the form is built (FE-12). */
@Component({
  selector: 'app-slack-signup-page',
  template: `
    <h1>Get news alerts in Slack</h1>
    <p>The Slack sign-up form will be available here soon.</p>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SlackSignupPage {}
