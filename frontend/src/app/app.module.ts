import { inject, NgModule, provideAppInitializer } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { providePrimeNG } from 'primeng/config';
import { SkyPreset } from '../presets';
import { ThemeService } from './utils/theme.service';
import { UtilsModule } from './utils/utils.module';
import { PrimeNgModule } from './primeng/primeng.module';
import { ConfirmationService, MessageService } from 'primeng/api';
import { InterceptorModule } from './security/interceptors/interceptor.module';
import { MenuModule } from './menu/menu.module';
import { DocumentsModule } from './documents/documents.module';
import { DialogService } from 'primeng/dynamicdialog';

@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    AppRoutingModule,
    UtilsModule,
    PrimeNgModule,
    InterceptorModule,
    MenuModule,
    DocumentsModule
  ],
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
    DialogService,
    ConfirmationService
  ],
  bootstrap: [AppComponent],
})
export class AppModule { }
