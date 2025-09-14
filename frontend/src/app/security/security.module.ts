import { NgModule } from '@angular/core';
import { LoginComponent } from './login/login.component';
import { SignupComponent } from './signup/signup.component';
import { SecurityRoutingModule } from './security-routing.module';
import { SecurityService } from './security.service';
import { CommonModule } from '@angular/common';

@NgModule({
  declarations: [LoginComponent, SignupComponent],
  imports: [CommonModule, SecurityRoutingModule],
  providers: [SecurityService],
})
export class SecurityModule {}
