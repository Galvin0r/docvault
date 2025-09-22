import { Component } from '@angular/core';
import { FormGroup, Validators } from '@angular/forms';
import { BaseFormComponent } from '../base-form.component';
import { passwordsMatchValidator } from '../../utils/validators';
import { RegistrationRequest } from '../security.model';
import { getDeviceId } from '../../utils/functions';

@Component({
  selector: 'app-signup',
  standalone: false,
  templateUrl: './signup.component.html',
  styleUrl: './signup.component.scss',
})
export class SignupComponent extends BaseFormComponent {
  protected buildForm(): FormGroup {
    return this.formBuilder.group({
      username: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      passwords: this.formBuilder.group(
        {
          password: ['', [Validators.required, Validators.minLength(8)]],
          confirmPassword: ['', [Validators.required]],
        },
        { validators: passwordsMatchValidator }
      ),
    });
  }

  isMismatched() {
    const grp = this.form.get('passwords');
    const c = this.form.get('passwords.confirmPassword');
    return !!c && !!grp?.hasError('passwordsMismatch') && (c.touched || this.submitted);
  }

  onSubmit() {
    if (!this.guardSubmit()) return;

    const { username, email, passwords: {password, confirmPassword} } = this.form.value;
    const regRequest: RegistrationRequest = {
      login: username,
      password: password,
      email: email
    } as RegistrationRequest;
    this.securityService.register(regRequest).subscribe({
      next: () => this.router.navigate(['/emailVerification'], {queryParams: {email: email}}),
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
