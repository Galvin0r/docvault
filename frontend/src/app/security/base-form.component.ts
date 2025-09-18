import { inject } from "@angular/core";
import { AbstractControl, FormBuilder, FormGroup } from "@angular/forms";

export abstract class BaseFormComponent {
  form!: FormGroup;
  submitted = false;
  protected formBuilder = inject(FormBuilder);

  protected abstract buildForm(): FormGroup;

  constructor() {
    this.form = this.buildForm();
  }

  ctrl(name: string): AbstractControl | null {
    return this.form.get(name);
  }

  isValid(name: string): boolean {
    const c = this.ctrl(name);
    return !!c && c.valid && (c.touched || this.submitted);
  }

  isInvalid(name: string): boolean {
    const c = this.ctrl(name);
    return !!c && c.invalid && (c.touched || this.submitted);
  }

  hasError(name: string, validator: string) {
    const c = this.ctrl(name);
    return c?.hasError(validator);
  }

  protected guardSubmit(): boolean {
    this.submitted = true;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return false;
    }
    return true;
  }
}