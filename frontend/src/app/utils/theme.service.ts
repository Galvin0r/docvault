import { DOCUMENT, inject, Injectable } from '@angular/core';

export type ThemeMode = 'light' | 'dark' | 'system';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly storageKey = 'theme-mode';
  private media: MediaQueryList | null = null;
  private currentMode: ThemeMode = 'system';
  private document = inject(DOCUMENT);

  constructor() {
    if (typeof window !== 'undefined' && 'matchMedia' in window) {
      this.media = window.matchMedia('(prefers-color-scheme: dark)');
    }
  }

  init() {
    const savedTheme =
      (localStorage.getItem(this.storageKey) as ThemeMode) || 'system';
    this.setMode(savedTheme, false);

    if (this.media) {
      const handler = (e: MediaQueryListEvent) => {
        if (this.currentMode === 'system') {
          this.apply();
        }
      };

      this.media.addEventListener?.('change', handler);
    }
  }

  setMode(mode: ThemeMode, persist = true) {
    this.currentMode = mode;
    if (persist) {
      localStorage.setItem(this.storageKey, mode);
    }
    this.apply();
  }

  getMode() {
    return this.currentMode;
  }

  isDark() {
    return (
      this.currentMode === 'dark' ||
      (this.currentMode === 'system' && this.media?.matches)
    );
  }

  private apply() {
    const el = this.document.documentElement;
    const dark = this.isDark();
    el.classList.toggle('app-dark', dark);

  }
}
