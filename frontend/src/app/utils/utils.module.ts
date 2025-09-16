import { NgModule } from "@angular/core";
import { LogoComponent } from "./logo/logo.component";
import { CommonModule } from "@angular/common";
import { GoogleButtonComponent } from './google-button/google-button.component';
import { Button } from "primeng/button";
import { PrimeNgModule } from "../primeng/primeng.module";

@NgModule({
  declarations: [LogoComponent, GoogleButtonComponent],
  imports: [CommonModule, Button, PrimeNgModule],
  exports: [LogoComponent, GoogleButtonComponent]
})
export class UtilsModule {}