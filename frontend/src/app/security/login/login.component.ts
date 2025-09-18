import { Component } from '@angular/core';
import { FormGroup, Validators } from '@angular/forms';
import { BaseFormComponent } from '../base-form.component';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
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

    
  }

  continueWithGoogle() {
    
  }
}
