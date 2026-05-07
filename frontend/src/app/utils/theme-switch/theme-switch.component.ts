import { Component, inject, OnInit, ViewChild } from '@angular/core';
import { ThemeMode, ThemeService } from '../theme.service';
import { ConnectedPosition } from '@angular/cdk/overlay';

@Component({
  selector: 'app-theme-switch',
  standalone: false,
  templateUrl: './theme-switch.component.html',
  styleUrl: './theme-switch.component.scss',
})
export class ThemeSwitchComponent implements OnInit {
  mode: ThemeMode = 'system';
  open = false;

  readonly positions: ConnectedPosition[] = [
    {
      originX: 'end',
      originY: 'center',
      overlayX: 'start',
      overlayY: 'center',
    },
    {
      originX: 'end',
      originY: 'bottom',
      overlayX: 'start',
      overlayY: 'bottom',
    },
    { originX: 'end', originY: 'top', overlayX: 'start', overlayY: 'top' },
  ];

  themeService = inject(ThemeService);

  ngOnInit(): void {
    this.mode = this.themeService.getMode();
  }

  toggle() {
    this.open = !this.open;
  }

  close() {
    this.open = false;
  }

  get triggerIcon(): string {
    return this.mode === 'dark'
      ? 'pi pi-moon'
      : this.mode === 'light'
      ? 'pi pi-sun'
      : 'pi pi-desktop';
  }

  get triggerAria(): string {
    return `Theme: ${this.mode}`;
  }

  choose(mode: ThemeMode) {
    this.mode = mode;
    this.themeService.setMode(mode);
    this.close();
  }
}