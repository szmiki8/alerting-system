import { Routes } from '@angular/router';
import { EmailSignupPage } from './email-signup/email-signup-page';
import { SlackSignupPage } from './slack-signup/slack-signup-page';

/** Route data read by the shell to build the public navigation. */
export interface PublicNavData {
  navLabel: string;
}

/**
 * Public sign-up routes, one per subscriber type (architecture Section 7.2, NFR-14).
 * The public navigation is built from this list, so a new subscriber type needs one route
 * entry here and its form component only.
 */
export const publicRoutes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Email sign-up',
    component: EmailSignupPage,
    data: { navLabel: 'Email' } satisfies PublicNavData,
  },
  {
    path: 'slack',
    title: 'Slack sign-up',
    component: SlackSignupPage,
    data: { navLabel: 'Slack' } satisfies PublicNavData,
  },
];
