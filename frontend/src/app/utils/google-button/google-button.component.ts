import { Component, output } from '@angular/core';

@Component({
  selector: 'app-google-button',
  standalone: false,
  templateUrl: './google-button.component.html',
  styleUrl: './google-button.component.scss'
})
export class GoogleButtonComponent {
  continue = output();

  onClick() {
    this.continue.emit();
  }
}