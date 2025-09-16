import { Component, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { RadioButtonClickEvent } from 'primeng/radiobutton';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  formBuilder = inject(FormBuilder);

  form = this.formBuilder.group({
    identifier: ['', [Validators.required]],
    password: ['', Validators.required],
    rememberMe: ['']
  });

  constructor() {
    this.form.markAllAsTouched();
  }

  onSubmit() {

  }

  loginWithGoogle() {
    
  }
}
