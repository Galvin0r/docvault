import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { HomeComponent } from './home/home.component';
import { LoginComponent } from './login/login.component';
import { TestService } from './test.service';
import {
  HTTP_INTERCEPTORS,
  provideHttpClient,
  withInterceptorsFromDi,
} from '@angular/common/http';
import { AuthInterceptor } from './auth.interceptor';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { providePrimeNG } from 'primeng/config';
import { ButtonModule } from 'primeng/button';
import { SkyPreset } from '../presets';

@NgModule({
  declarations: [AppComponent, HomeComponent, LoginComponent],
  imports: [BrowserModule, AppRoutingModule, ButtonModule],
  providers: [
    TestService,
    provideHttpClient(withInterceptorsFromDi()),
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: SkyPreset,
        options: {
          darkModeSelector: '.app-dark'
        }
      },
    }),
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}