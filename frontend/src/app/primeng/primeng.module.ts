import { NgModule } from "@angular/core";
import { CardModule } from "primeng/card";
import { InputTextModule } from 'primeng/inputtext';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { PasswordModule } from 'primeng/password';
import { CheckboxModule } from 'primeng/checkbox';
import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';

const primeNgModules = [
  CardModule,
  InputTextModule,
  InputGroupModule,
  InputGroupAddonModule,
  PasswordModule,
  CheckboxModule,
  ButtonModule,
  DividerModule,
];

@NgModule({
  imports: [primeNgModules],
  exports: [primeNgModules]
})
export class PrimeNgModule {}