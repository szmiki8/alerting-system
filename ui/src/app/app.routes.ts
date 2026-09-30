import { Routes } from '@angular/router';
import { publicRoutes } from './public/public.routes';
import { NotFoundPage } from './shared/not-found/not-found-page';

/**
 * Application route table (architecture Section 11).
 * No route may start with /api, /oauth2, /login or /logout: these paths belong to the Core and
 * are forwarded by the dev-server proxy and by Nginx.
 */
export const routes: Routes = [
  ...publicRoutes,
  {
    path: 'admin',
    loadChildren: () => import('./admin/admin.routes').then((m) => m.adminRoutes),
  },
  {
    path: '**',
    title: 'Page not found',
    component: NotFoundPage,
  },
];
