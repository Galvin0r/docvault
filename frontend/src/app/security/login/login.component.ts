import { Component, inject } from '@angular/core';
import { FormGroup, Validators } from '@angular/forms';
import { BaseFormComponent } from '../base-form.component';
import { getDeviceId } from '../../utils/functions';
import { AuthenticationRequest } from '../security.model';
import { DOCUMENT } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent extends BaseFormComponent {
  private doc = inject(DOCUMENT);

  protected buildForm(): FormGroup {
    return this.formBuilder.group({
      identifier: ['', Validators.required],
      password: ['', Validators.required],
      rememberMe: [false],
    });
  }

  onSubmit() {
    if (!this.guardSubmit()) {
      return;
    }

    const deviceId = getDeviceId();
    const { identifier, password, rememberMe } = this.form.value;
    const loginOrEmail = String(identifier).trim();
    const isEmail = loginOrEmail.includes('@');
    const authRequest: AuthenticationRequest = {
      login: isEmail ? null : loginOrEmail,
      email: isEmail ? loginOrEmail : null,
      password,
      rememberMe,
      deviceInfo: deviceId,
    } as AuthenticationRequest;

    this.securityService.login(authRequest).subscribe({
      next: () => this.router.navigate(['/home']),
      error: (e) => { this.error = e.error.code; }
    });
  }

  continueWithGoogle() {
    this.doc.cookie = `deviceId=${getDeviceId()}; path=/`;
    this.doc.location.assign('/api/oauth2/authorization/google');
  }
}
