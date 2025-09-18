import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { BaseFormComponent } from '../base-form.component';
import { passwordsMatchValidator } from '../../utils/validators';

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
  }

  continueWithGoogle() {
    
  }
}
