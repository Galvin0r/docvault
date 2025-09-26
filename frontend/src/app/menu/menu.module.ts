import { NgModule } from '@angular/core';
import { MainLayoutComponent } from './main-layout/main-layout.component';
import { TopMenuComponent } from './top-menu/top-menu.component';
import { AuthLayoutComponent } from './auth-layout/auth-layout.component';
import { HomeComponent } from './home/home.component';
import { CommonModule } from '@angular/common';
import { AppRoutingModule } from "../app-routing.module";
import { Menubar } from "primeng/menubar";
import { UtilsModule } from "../utils/utils.module";
import { PrimeNgModule } from '../primeng/primeng.module';

@NgModule({
  declarations: [MainLayoutComponent, TopMenuComponent, AuthLayoutComponent, HomeComponent],
  imports: [CommonModule, AppRoutingModule, Menubar, UtilsModule, PrimeNgModule, UtilsModule],
  exports: [
    MainLayoutComponent, AuthLayoutComponent
  ]
})
export class MenuModule {}
