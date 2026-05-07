import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { BaseFormComponent } from '../../security/base-form.component';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Group, Visibility, visibilityOptions } from '../groups.model';

@Component({
  selector: 'app-group-add',
  standalone: false,
  templateUrl: './group-add.component.html',
  styleUrl: './group-add.component.scss',
})
export class GroupAddComponent extends BaseFormComponent implements OnInit {
  protected override buildForm(): FormGroup {
    return this.formBuilder.group({
      name: ['', [Validators.required]],
      description: [''],
      visibility: ['PUBLIC', [Validators.required]],
      id: [null],
    });
  }

  ref = inject(DynamicDialogRef);
  private config = inject(DynamicDialogConfig);
  initial: Partial<Group> = this.config.data?.initial ?? {};

  visibilityOptions = visibilityOptions;

  submit() {
    if (!this.guardSubmit()) return;

    this.ref.close(this.form.getRawValue() as Group);
  }

  ngOnInit(): void {
    if (this.initial) this.form.patchValue(this.initial);
  }
}