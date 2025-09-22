import { NgModule } from '@angular/core';
import { LogoComponent } from './logo/logo.component';
import { CommonModule } from '@angular/common';
import { GoogleButtonComponent } from './google-button/google-button.component';
import { PrimeNgModule } from '../primeng/primeng.module';
import { ThemeSwitchComponent } from './theme-switch/theme-switch.component';
import { ThemeService } from './theme.service';
import { OverlayModule } from '@angular/cdk/overlay';
import { CodePipe } from './pipes/code.pipe';
import { FormErrorComponent } from './form-error/form-error.component';

@NgModule({
  declarations: [LogoComponent, GoogleButtonComponent, ThemeSwitchComponent, CodePipe, FormErrorComponent],
  imports: [CommonModule, PrimeNgModule, OverlayModule],
  providers: [ThemeService],
  exports: [LogoComponent, GoogleButtonComponent, ThemeSwitchComponent, CodePipe, FormErrorComponent],
})
export class UtilsModule {}
