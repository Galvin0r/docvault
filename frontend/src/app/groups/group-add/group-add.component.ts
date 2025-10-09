import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { BaseFormComponent } from '../../security/base-form.component';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { Group, Visibility } from '../groups.model';

@Component({
  selector: 'app-group-add',
  standalone: false,
  templateUrl: './group-add.component.html',
  styleUrl: './group-add.component.scss',
})
export class GroupAddComponent extends BaseFormComponent {
  protected override buildForm(): FormGroup {
    return this.formBuilder.group({
      name: ['', [Validators.required]],
      description: [''],
      visibility: [ 'PUBLIC', [Validators.required]],
    });
  }
  ref = inject(DynamicDialogRef);
  visibilityOptions = [
    { label: 'Public', value: 'PUBLIC' as Visibility },
    { label: 'Private', value: 'PRIVATE' as Visibility },
    { label: 'Request only', value: 'REQUEST_ONLY' as Visibility },
  ];

  submit() {
    if (!this.guardSubmit()) return;

    this.ref.close(this.form.getRawValue() as Group);
  }
}
