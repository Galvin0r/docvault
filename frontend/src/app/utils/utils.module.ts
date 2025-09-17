import { NgModule } from "@angular/core";
import { LogoComponent } from "./logo/logo.component";
import { CommonModule } from "@angular/common";
import { GoogleButtonComponent } from './google-button/google-button.component';
import { PrimeNgModule } from "../primeng/primeng.module";
import { ThemeSwitchComponent } from './theme-switch/theme-switch.component';
import { ThemeService } from "./theme.service";
import { OverlayModule } from '@angular/cdk/overlay';

@NgModule({
  declarations: [LogoComponent, GoogleButtonComponent, ThemeSwitchComponent],
  imports: [CommonModule, PrimeNgModule, OverlayModule],
  providers: [ThemeService],
  exports: [LogoComponent, GoogleButtonComponent, ThemeSwitchComponent]
})
export class UtilsModule {}