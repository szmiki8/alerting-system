import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Admin area start page. Placeholder until the admin area is built (FE-14). */
@Component({
  selector: 'app-admin-home-page',
  template: `
    <h1>Administration</h1>
    <p>The administration area will be available here soon.</p>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminHomePage {}
