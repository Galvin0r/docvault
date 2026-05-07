import { Component, HostBinding, input } from '@angular/core';

@Component({
  selector: 'app-logo',
  standalone: false,
  templateUrl: './logo.component.html',
  styleUrl: './logo.component.scss',
})
export class LogoComponent {
  size = input(64);

  @HostBinding('style.width.px') get w() {
    return this.size();
  }
  @HostBinding('style.height.px') get h() {
    return this.size();
  }

  @HostBinding('attr.role') role = 'img';
  @HostBinding('attr.aria-label') aria = 'DocVault Logo';
}