import { Component, inject } from '@angular/core';
import { AbstractControl, FormBuilder, Validators } from '@angular/forms';
import { RadioButtonClickEvent } from 'primeng/radiobutton';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  formBuilder = inject(FormBuilder);
  submitted = false;

  form = this.formBuilder.group({
    identifier: ['', [Validators.required]],
    password: ['', Validators.required],
    rememberMe: [false]
  });

  ctrl(name: string): AbstractControl | null {
    return this.form.get(name);
  }

  isValid(name: string): boolean {
    const c = this.ctrl(name);
    return !!c && c.valid && (c.touched || this.submitted);
  }

  isInvalid(name: string): boolean {
    const c = this.ctrl(name);
    return !!c && c.invalid && (c.touched || this.submitted);
  }

  constructor() {
    
  }

  onSubmit() {
    this.submitted = true;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
  }

  loginWithGoogle() {
    
  }
}
