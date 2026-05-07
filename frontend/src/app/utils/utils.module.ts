import { NgModule } from '@angular/core';
import { LogoComponent } from './logo/logo.component';
import { CommonModule } from '@angular/common';
import { GoogleButtonComponent } from './google-button/google-button.component';
import { PrimeNgModule } from '../primeng/primeng.module';
import { ThemeSwitchComponent } from './theme-switch/theme-switch.component';
import { ThemeService } from './theme.service';
import { OverlayModule } from '@angular/cdk/overlay';
import { CodePipe } from './pipes/code.pipe';
import { FileSizePipe } from './pipes/file-size.pipe';
import { FormErrorComponent } from './form-error/form-error.component';
import { PaginatorComponent } from './search/paginator/paginator.component';
import { FormsModule } from '@angular/forms';
import { FileExtensionPipe } from './pipes/file-extension.pipe';

@NgModule({
  declarations: [
    LogoComponent,
    GoogleButtonComponent,
    ThemeSwitchComponent,
    CodePipe,
    FileSizePipe,
    FileExtensionPipe,
    FormErrorComponent,
    PaginatorComponent,
  ],
  imports: [CommonModule, PrimeNgModule, OverlayModule, FormsModule],
  providers: [ThemeService],
  exports: [
    LogoComponent,
    GoogleButtonComponent,
    ThemeSwitchComponent,
    CodePipe,
    FileSizePipe,
    FileExtensionPipe,
    FormErrorComponent,
    PaginatorComponent,
  ],
})
export class UtilsModule { }