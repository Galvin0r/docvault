import { inject, NgModule, provideAppInitializer } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { providePrimeNG } from 'primeng/config';
import { SkyPreset } from '../presets';
import { HomeComponent } from './home/home.component';
import { ThemeService } from './utils/theme.service';
import { UtilsModule } from './utils/utils.module';
import { PrimeNgModule } from './primeng/primeng.module';
import { MessageService } from 'primeng/api';
import { InterceptorModule } from './security/interceptors/interceptor.module';

@NgModule({
  declarations: [AppComponent, HomeComponent],
  imports: [BrowserModule, AppRoutingModule, UtilsModule, PrimeNgModule, InterceptorModule],
  providers: [
    provideHttpClient(withInterceptorsFromDi()),
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: SkyPreset,
        options: {
          darkModeSelector: '.app-dark',
        },
      },
    }),
    provideAppInitializer(() => inject(ThemeService).init()),
    MessageService,
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
