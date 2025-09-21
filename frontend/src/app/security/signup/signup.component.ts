import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { BaseFormComponent } from '../base-form.component';
import { passwordsMatchValidator } from '../../utils/validators';
import { RegistrationRequest } from '../security.model';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-signup',
  standalone: false,
  templateUrl: './signup.component.html',
  styleUrl: './signup.component.scss',
})
export class SignupComponent extends BaseFormComponent {
  duplicateName = false;
  duplicateEmail = false;

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

    const { username, email, passwords: {password, conformPassword} } = this.form.value;
    const regRequest: RegistrationRequest = {
      login: username,
      password: password,
      email: email
    } as RegistrationRequest;
    this.securityService.register(regRequest).subscribe({
      next: () => this.router.navigate(['/emailVerification'], {queryParams: {email: email}}),
      error: (err: HttpErrorResponse) => {
        if (err.status === 409) {
          const error = String(err.error.error);
          if (error.includes("User with email")) {
            this.duplicateEmail = true;
          } else if (error.includes("User with login")) {
            this.duplicateName = true;
          }
        }
      }
    });
  }

  continueWithGoogle() {
    
  }

  onErrorClose() {
    this.duplicateEmail = false;
    this.duplicateName = false;
  }
}
