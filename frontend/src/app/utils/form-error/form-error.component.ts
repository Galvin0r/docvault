import { Component, input, output } from '@angular/core';
import { httpErrors, HttpErrorCode } from '../consts';

@Component({
  selector: 'app-form-error',
  standalone: false,
  templateUrl: './form-error.component.html',
  styleUrl: './form-error.component.scss'
})
export class FormErrorComponent {
  error = input.required<HttpErrorCode | null | undefined>();
  onClose = output();
  httpErrorCodes = httpErrors;
}