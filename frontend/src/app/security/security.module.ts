import { NgModule } from '@angular/core';
import { LoginComponent } from './login/login.component';
import { SignupComponent } from './signup/signup.component';
import { SecurityRoutingModule } from './security-routing.module';
import { SecurityService } from './security.service';
import { CommonModule } from '@angular/common';
import { Card } from 'primeng/card';
import { PrimeNgModule } from '../primeng/primeng.module';
import { UtilsModule } from '../utils/utils.module';
import { ReactiveFormsModule } from '@angular/forms';
import { PasswordRecoveryComponent } from './password-recovery/password-recovery.component';
import { EmailVerificationComponent } from './email-verification/email-verification.component';

@NgModule({
  declarations: [
    LoginComponent,
    SignupComponent,
    PasswordRecoveryComponent,
    EmailVerificationComponent,
  ],
  imports: [
    CommonModule,
    SecurityRoutingModule,
    Card,
    PrimeNgModule,
    UtilsModule,
    ReactiveFormsModule,
  ],
  providers: [SecurityService],
})
export class SecurityModule {}
