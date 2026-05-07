import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-auth-card',
  standalone: false,
  templateUrl: './auth-card.component.html',
  styleUrl: './auth-card.component.scss'
})
export class AuthCardComponent {
  title = input.required<string>();
  subtitle = input('');
  showGoogle = input(false);
  googleContinue = output();
}