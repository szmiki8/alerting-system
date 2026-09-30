import { Routes } from '@angular/router';
import { AdminHomePage } from './admin-home/admin-home-page';

/** Admin route tree, lazy loaded from app.routes.ts so public visitors never download it. */
export const adminRoutes: Routes = [
  {
    path: '',
    title: 'Administration',
    component: AdminHomePage,
  },
];
