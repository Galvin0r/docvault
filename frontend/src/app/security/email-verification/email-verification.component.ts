import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-email-verification',
  standalone: false,
  templateUrl: './email-verification.component.html',
  styleUrl: './email-verification.component.scss'
})
export class EmailVerificationComponent implements OnInit {
  route = inject(ActivatedRoute);
  formBuilder = inject(FormBuilder);
  email: string | null = null;
  cooldown = 0;
  intervalId: any;

  form = this.formBuilder.group({
    code: []
  });

  ngOnInit(): void {
    this.email = this.route.snapshot.queryParamMap.get('email');

    const lastSent = localStorage.getItem('lastResendTimestamp');
    if (lastSent) {
      const elapsed = Math.floor((Date.now() - Number(lastSent)) / 1000);
      if (elapsed < 60) {
        this.startCooldown(60 - elapsed);
      }
    }
  }

  onSubmit() {

  }

  onResend() {
    localStorage.setItem('lastResendTimestamp', Date.now().toString());
    this.startCooldown(60);
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
}
