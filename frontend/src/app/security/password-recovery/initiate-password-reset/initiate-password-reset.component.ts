import { Component, inject } from '@angular/core';
import { FormGroup, Validators } from '@angular/forms';
import { BaseFormComponent } from '../../base-form.component';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'app-initiate-password-reset',
  standalone: false,
  templateUrl: './initiate-password-reset.component.html',
  styleUrl: './initiate-password-reset.component.scss'
})
export class InitiatePasswordResetComponent extends BaseFormComponent {
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
      error: (e) => {
        this.error = e.appCode;
      }
    });
  }
}
