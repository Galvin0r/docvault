import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { LoginComponent } from "./login/login.component";
import { SignupComponent } from "./signup/signup.component";
import { PasswordRecoveryComponent } from "./password-recovery/password-recovery.component";
import { EmailVerificationComponent } from "./email-verification/email-verification.component";
import { InitiatePasswordResetComponent } from "./password-recovery/initiate-password-reset/initiate-password-reset.component";

const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'register',
    component: SignupComponent
  },
  {
    path: 'initiatePasswordReset',
    component: InitiatePasswordResetComponent
  },
  {
    path: 'emailVerification',
    component: EmailVerificationComponent
  },
  {
    path: 'setNewPassword',
    component: PasswordRecoveryComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class SecurityRoutingModule {}