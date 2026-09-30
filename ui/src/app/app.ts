import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Injector,
  afterNextRender,
  inject,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, skip } from 'rxjs';
import { PRODUCT_NAME } from './core/product';
import { PublicNavData, publicRoutes } from './public/public.routes';

interface NavLink {
  path: string;
  label: string;
}

/** Application shell: skip link, header with public navigation, main content, footer. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  protected readonly productName = PRODUCT_NAME;

  /** One link per public sign-up route, in route order. */
  protected readonly navLinks: readonly NavLink[] = publicRoutes.map((route) => ({
    path: `/${route.path ?? ''}`,
    label: (route.data as PublicNavData).navLabel,
  }));

  private readonly main = viewChild.required<ElementRef<HTMLElement>>('main');
  private readonly injector = inject(Injector);

  constructor() {
    // Move focus to the new page's heading after in-app navigation, so screen reader and
    // keyboard users start at the new content. The initial page load keeps the browser default.
    inject(Router)
      .events.pipe(
        filter((event) => event instanceof NavigationEnd),
        skip(1),
        takeUntilDestroyed(),
      )
      .subscribe(() => afterNextRender(() => this.focusMainContent(), { injector: this.injector }));
  }

  /** Skip link: focus the main content without changing the URL (the app uses <base href>). */
  protected skipToMain(event: Event): void {
    event.preventDefault();
    this.focusMainContent();
  }

  private focusMainContent(): void {
    const main = this.main().nativeElement;
    const target = main.querySelector<HTMLElement>('h1') ?? main;
    if (!target.hasAttribute('tabindex')) {
      target.setAttribute('tabindex', '-1');
    }
    target.focus();
  }
}
