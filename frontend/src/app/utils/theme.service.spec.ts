import { TestBed } from '@angular/core/testing';
import { ThemeMode, ThemeService } from './theme.service';
import { DOCUMENT } from '@angular/core';

describe('ThemeService', () => {
  let service: ThemeService;
  let doc: Document;

  let origStorage: Storage;
  let store: Map<string, string>;
  let mockStorage: Storage;

  type Listener = (e: MediaQueryListEvent) => void;
  let mediaMatches = false;
  let listeners: Listener[] = [];

  beforeEach(() => {
    store = new Map<string, string>();
    mockStorage = {
      getItem: (k: string) => (store.has(k) ? store.get(k)! : null),
      setItem: (k: string, v: string) => {
        store.set(k, v);
      },
      removeItem: (k: string) => {
        store.delete(k);
      },
      clear: () => {
        store.clear();
      },
      key: (i: number) => Array.from(store.keys())[i] ?? null,
      get length() {
        return store.size;
      },
    };

    origStorage = window.localStorage;
    spyOnProperty(window, 'localStorage', 'get').and.returnValue(mockStorage);

    listeners = [];
    mediaMatches = false;

    (window as any).matchMedia = jasmine.createSpy('matchMedia').and.callFake(() => ({
      get matches() {
        return mediaMatches;
      },
      media: '(prefers-color-scheme: dark)',
      onchange: null,
      addEventListener: (_type: 'change', cb: Listener) => {
        listeners.push(cb);
      },
      removeEventListener: (_type: 'change', cb: Listener) => {
        listeners = listeners.filter((l) => l !== cb);
      },
      addListener: () => {},
      removeListener: () => {},
      dispatchEvent: () => true,
    }));

    TestBed.configureTestingModule({
      providers: [{ provide: DOCUMENT, useValue: document }],
    });

    service = TestBed.inject(ThemeService);
    doc = TestBed.inject(DOCUMENT);
    doc.documentElement.classList.remove('app-dark');
  });

  afterEach(() => {
    Object.defineProperty(window, 'localStorage', { value: origStorage });
  });

  function triggerMediaChange(next: boolean) {
    mediaMatches = next;
    const evt = { matches: next } as MediaQueryListEvent;
    listeners.forEach((l) => l(evt));
  }

  it('init reads saved theme and applies it without persisting again', () => {
    spyOn(window.localStorage, 'getItem').and.returnValue('dark');
    const setSpy = spyOn(window.localStorage, 'setItem');

    service.init();

    expect(service.getMode()).toBe('dark');
    expect(doc.documentElement.classList.contains('app-dark')).toBeTrue();
    expect(setSpy).not.toHaveBeenCalled();
  });

  it('init with system uses matchMedia to apply current preference', () => {
    spyOn(window.localStorage, 'getItem').and.returnValue('system');

    mediaMatches = true;
    service.init();
    expect(service.getMode()).toBe('system');
    expect(doc.documentElement.classList.contains('app-dark')).toBeTrue();

    doc.documentElement.classList.remove('app-dark');
    mediaMatches = false;
    service.init();
    expect(doc.documentElement.classList.contains('app-dark')).toBeFalse();
  });

  it('setMode persists and applies classes', () => {
    const setSpy = spyOn(window.localStorage, 'setItem');
    service.setMode('light');
    expect(service.getMode()).toBe('light');
    expect(setSpy).toHaveBeenCalledWith('theme-mode', 'light');
    expect(doc.documentElement.classList.contains('app-dark')).toBeFalse();

    service.setMode('dark');
    expect(service.getMode()).toBe('dark');
    expect(doc.documentElement.classList.contains('app-dark')).toBeTrue();
  });

  it('isDark reflects explicit modes and system preference', () => {
    service.setMode('dark');
    expect(service.isDark()).toBeTrue();

    service.setMode('light');
    expect(service.isDark()).toBeFalse();

    service.setMode('system');
    mediaMatches = true;
    expect(service.isDark()).toBeTrue();
    mediaMatches = false;
    expect(service.isDark()).toBeFalse();
  });

  it('reacts to matchMedia change while in system mode', () => {
    spyOn(window.localStorage, 'getItem').and.returnValue('system');
    service.init();

    triggerMediaChange(true);
    expect(doc.documentElement.classList.contains('app-dark')).toBeTrue();

    triggerMediaChange(false);
    expect(doc.documentElement.classList.contains('app-dark')).toBeFalse();
  });

  it('does not react to matchMedia change in explicit light/dark mode', () => {
    spyOn(window.localStorage, 'getItem').and.returnValue('dark');
    service.init();

    triggerMediaChange(false);
    expect(doc.documentElement.classList.contains('app-dark')).toBeTrue();

    service.setMode('light');
    triggerMediaChange(true);
    expect(doc.documentElement.classList.contains('app-dark')).toBeFalse();
  });

  it('getMode returns currentMode', () => {
    const modes: ThemeMode[] = ['system', 'light', 'dark'];
    modes.forEach((m) => {
      service.setMode(m);
      expect(service.getMode()).toBe(m);
    });
  });
});
