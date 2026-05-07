import { Component, inject, OnInit } from '@angular/core';
import { BaseFormComponent } from '../base-form.component';
import { FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MessageService } from 'primeng/api';
import { passwordsMatchValidator } from '../../utils/validators';

@Component({
  selector: 'app-password-recovery',
  standalone: false,
  templateUrl: './password-recovery.component.html',
  styleUrl: './password-recovery.component.scss',
})
export class PasswordRecoveryComponent extends BaseFormComponent implements OnInit {
  activatedRoute = inject(ActivatedRoute);
  messageService = inject(MessageService);
  token: string | null = null;

  protected buildForm(): FormGroup {
    return this.formBuilder.group(
      {
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required]],
      },
      { validators: passwordsMatchValidator }
    );
  }

  ngOnInit(): void {
    this.token = this.activatedRoute.snapshot.queryParamMap.get('token');
  }

  onSubmit() {
    if (!this.guardSubmit()) {
      return;
    }

    const { password, confirmPassword } = this.form.value;
    this.securityService.setNewPassword(String(this.token), password).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: 'Password changed successfully',
        });
        this.router.navigate(['/home']);
      },
      error: (e) => {
        this.error = e.error.code;
      },
    });
  }

  isMismatched() {
    const c = this.form.get('confirmPassword');
    return !!c && !!this.form?.hasError('passwordsMismatch') && (c.touched || this.submitted);
  }
}