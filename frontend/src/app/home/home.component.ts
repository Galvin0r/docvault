import { Component, signal } from '@angular/core';
import { TestService } from '../test.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: false,
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {
  testValue = signal<string>("");

  constructor(
    private testServise: TestService,
    private router: Router
  ) {
    testServise.test().subscribe(res => {
      this.testValue.set(res);
    });
  }

  logout() {
    this.testServise.logout().subscribe(() => {
      this.router.navigate(['/login']);
    });
  }
}
