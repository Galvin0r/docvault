import { Component, inject } from '@angular/core';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { BaseFormComponent } from '../../security/base-form.component';
import { FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-group-add-user',
  standalone: false,
  templateUrl: './group-add-user.component.html',
  styleUrl: './group-add-user.component.scss'
})
export class GroupAddUserComponent extends BaseFormComponent {
  ref = inject(DynamicDialogRef);

  protected override buildForm(): FormGroup {
    return this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  submit() {
    if (!this.guardSubmit()) return;

    this.ref.close(this.form.getRawValue() as string);
  }
}