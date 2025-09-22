import { Component } from '@angular/core';
import { FormGroup, Validators } from '@angular/forms';
import { BaseFormComponent } from '../base-form.component';
import { getDeviceId } from '../../utils/functions';
import { AuthenticationRequest } from '../security.model';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent extends BaseFormComponent {
  protected buildForm(): FormGroup {
    return this.formBuilder.group({
      identifier: ['', Validators.required],
      password: ['', Validators.required],
      rememberMe: [false],
    });
  }

  onSubmit() {
    if (!this.guardSubmit()) return;

    const deviceId = getDeviceId();
    const { identifier, password, rememberMe } = this.form.value;
    const authRequest: AuthenticationRequest = {
      login: identifier,
      email: identifier,
      password: password,
      rememberMe: rememberMe,
      deviceInfo: deviceId,
    } as AuthenticationRequest;

    this.securityService.login(authRequest).subscribe({
      next: () => this.router.navigate(['/home']),
      error: (e) => {
       this.error = e.appCode;
      }
    });
  }

  continueWithGoogle() {
    document.cookie = `deviceId=${getDeviceId()}; path=/`;
    window.location.href = `/api/oauth2/authorization/google`;
  }
}
