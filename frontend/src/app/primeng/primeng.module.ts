import { NgModule } from "@angular/core";
import { CardModule } from "primeng/card";
import { InputTextModule } from 'primeng/inputtext';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { PasswordModule } from 'primeng/password';
import { CheckboxModule } from 'primeng/checkbox';
import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { MenuModule } from 'primeng/menu';
import { TooltipModule } from 'primeng/tooltip';
import { MessageModule } from 'primeng/message';
import { InputOtpModule } from 'primeng/inputotp';
import { ToastModule } from 'primeng/toast';
import { MenubarModule } from 'primeng/menubar';
import { PaginatorModule } from 'primeng/paginator';
import { SelectModule } from 'primeng/select';
import { DataViewModule } from 'primeng/dataview';
import { FloatLabelModule } from 'primeng/floatlabel';
import { DialogModule } from 'primeng/dialog';
import { DynamicDialogModule } from 'primeng/dynamicdialog';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TextareaModule } from 'primeng/textarea';

const primeNgModules = [
  CardModule,
  InputTextModule,
  InputGroupModule,
  InputGroupAddonModule,
  PasswordModule,
  CheckboxModule,
  ButtonModule,
  DividerModule,
  MenuModule,
  TooltipModule,
  MessageModule,
  InputOtpModule,
  ToastModule,
  MenubarModule,
  PaginatorModule,
  SelectModule,
  DataViewModule,
  FloatLabelModule,
  DialogModule,
  DynamicDialogModule,
  SelectButtonModule,
  TextareaModule,
];

@NgModule({
  imports: [primeNgModules],
  exports: [primeNgModules]
})
export class PrimeNgModule {}