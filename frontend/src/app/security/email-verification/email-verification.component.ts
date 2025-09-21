import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { SecurityService } from '../security.service';
import { MessageService } from 'primeng/api';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-email-verification',
  standalone: false,
  templateUrl: './email-verification.component.html',
  styleUrl: './email-verification.component.scss',
})
export class EmailVerificationComponent implements OnInit {
  activatedRoute = inject(ActivatedRoute);
  router = inject(Router);
  formBuilder = inject(FormBuilder);
  email: string | null = null;
  cooldown = 0;
  intervalId: any;
  securityService = inject(SecurityService);
  messageService = inject(MessageService);
  isTokenInvalid = false;
  isTokenExpired = false;

  form = this.formBuilder.group({
    code: [],
  });

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

  onSubmit() {
    const { code } = this.form.value;
    this.securityService.activateAccount(Number(code)).subscribe({
      next: () => {
        this.messageService.add({
            severity: 'success',
            summary: 'Success',
            detail: 'Account successfully registered'
          });
          this.router.navigate(['/login']);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 403) {
          const error = String(err.error.error);
          if (error.includes("Invalid activation token")) {
            this.isTokenInvalid = true;
          } else if (error.includes("Activation token has expired")) {
            this.isTokenExpired = true;
          }
        }
      }
    });
  }

  onResend() {
    localStorage.setItem('lastResendTimestamp', Date.now().toString());
    this.startCooldown(60);
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
            detail: 'Failed to resend verification email. Please try again later'
          }),
      });
    }
  }

  startCooldown(seconds: number) {
    this.cooldown = seconds;

    this.intervalId = setInterval(() => {
      this.cooldown--;
      if (this.cooldown <= 0) {
        clearInterval(this.intervalId);
      }
    }, 1000);
  }

  onErrorClose() {
    this.isTokenExpired = false;
    this.isTokenInvalid = false;
  }
}
