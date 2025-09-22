import { Component, inject, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MessageService } from 'primeng/api';
import { BaseFormComponent } from '../base-form.component';
import { RESEND_COOLDOWN_SECONDS, RESEND_KEY } from '../../utils/consts';

@Component({
  selector: 'app-email-verification',
  standalone: false,
  templateUrl: './email-verification.component.html',
  styleUrl: './email-verification.component.scss',
})
export class EmailVerificationComponent extends BaseFormComponent implements OnInit {
  activatedRoute = inject(ActivatedRoute);
  email: string | null = null;
  cooldown = 0;
  intervalId: any;
  messageService = inject(MessageService);

  ngOnInit(): void {
    this.email = this.activatedRoute.snapshot.queryParamMap.get('email');

    const lastSent = localStorage.getItem('lastResendTimestamp');
    if (lastSent) {
      const elapsed = Math.floor((Date.now() - Number(lastSent)) / 1000);
      if (elapsed < 60) {
        this.startCooldown(60 - elapsed);
      }
    }
  }

  protected override buildForm(): FormGroup {
    return this.formBuilder.group({
      code: [],
    });
  }

  onSubmit() {
    const { code } = this.form.value;
    this.securityService.activateAccount(Number(code)).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Success',
          detail: 'Account successfully registered',
        });
        this.router.navigate(['/login']);
      },
      error: (e) => {
        this.error = e.appCode;
      },
    });
  }

  onResend() {
    localStorage.setItem(RESEND_KEY, Date.now().toString());
    this.startCooldown(RESEND_COOLDOWN_SECONDS);
    if (this.email) {
      this.securityService.resendActivationCode(this.email).subscribe({
        next: () =>
          this.messageService.add({
            severity: 'success',
            summary: 'Success',
            detail: 'Verification email resent to ' + this.email,
          }),
        error: () =>
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'Failed to resend verification email. Please try again later',
          }),
      });
    }
  }

  startCooldown(seconds: number) {
    this.cooldown = seconds;

    if (this.intervalId !== null) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }

    this.intervalId = window.setInterval(() => {
      this.cooldown--;
      if (this.cooldown <= 0 && this.intervalId !== null) {
        clearInterval(this.intervalId);
        this.intervalId = null;
      }
    }, 1000);
  }
}
