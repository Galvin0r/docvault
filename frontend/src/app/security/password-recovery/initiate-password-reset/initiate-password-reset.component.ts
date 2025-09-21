import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { BaseFormComponent } from '../../base-form.component';
import { HttpErrorResponse } from '@angular/common/http';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'app-initiate-password-reset',
  standalone: false,
  templateUrl: './initiate-password-reset.component.html',
  styleUrl: './initiate-password-reset.component.scss'
})
export class InitiatePasswordResetComponent extends BaseFormComponent {
  notActiveAccount = false;
  incorrectEmail = false;
  messageService = inject(MessageService);

  protected buildForm(): FormGroup {
    return this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  onSubmit() {
    if (!this.guardSubmit()) return;

    const { email } = this.form.value;
    this.securityService.resetPassword(email).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
            summary: 'Success',
            detail: 'Email with instructions sent to ' + email,
        });
        this.router.navigate(['/login']);
      },
      error: (err: HttpErrorResponse) => {
        const error = String(err.error.error);
        if (err.status === 403) {
          if (error.includes("Account is not activated")) {
            this.notActiveAccount = true;
          } else if (error.includes("User not found")) {
            this.incorrectEmail = true;
          }
        }
      }
    });
  }

  onErrorClose() {
    this.incorrectEmail = false;
    this.notActiveAccount = false;
  }
}
