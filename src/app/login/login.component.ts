import { Component } from '@angular/core';
import { TestService } from '../test.service';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {

  constructor(private testService: TestService){}

  loginWithGitHub() {
    window.location.href = '/api/oauth2/authorization/github';
  }

  loginWithGoogle() {
    window.location.href = 'http://localhost:8080/api/oauth2/authorization/google';
  }
}
