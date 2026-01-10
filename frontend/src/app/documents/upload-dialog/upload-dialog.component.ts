import { Component, inject } from '@angular/core';
import { FormBuilder, FormControl, Validators } from '@angular/forms';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { DocumentService } from '../documents.service';
import { Visibility } from '../../groups/groups.model';

@Component({
    selector: 'app-upload-dialog',
    standalone: false,
    templateUrl: './upload-dialog.component.html',
    styleUrl: './upload-dialog.component.scss',
})
export class UploadDialogComponent {
    private fb = inject(FormBuilder);
    private documentService = inject(DocumentService);
    private dialogRef = inject(DynamicDialogRef);

    visibilityOptions = [
        { label: 'Private', value: 'PRIVATE' },
        { label: 'Public', value: 'PUBLIC' }
    ];

    form = this.fb.group({
        title: ['', [Validators.required, Validators.maxLength(255)]],
        description: ['', [Validators.maxLength(1000)]],
        visibility: ['PRIVATE', [Validators.required]],
        file: [null as File | null, [Validators.required]]
    });

    get titleControl() { return this.form.get('title') as FormControl; }
    get descriptionControl() { return this.form.get('description') as FormControl; }
    get visibilityControl() { return this.form.get('visibility') as FormControl; }
    get fileControl() { return this.form.get('file') as FormControl; }

    onFileSelect(event: Event) {
        const input = event.target as HTMLInputElement;
        if (input.files && input.files.length > 0) {
            const file = input.files[0];
            this.fileControl.setValue(file);
            this.fileControl.markAsTouched();

            if (!this.titleControl.value) {
                const filename = file.name;
                const nameWithoutExt = filename.split('.').slice(0, -1).join('.') || filename;
                this.titleControl.setValue(nameWithoutExt);
            }
        }
    }

    submit() {
        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }

        const { title, description, visibility, file } = this.form.value;

        this.documentService.createDraft(title!, description!, visibility as Visibility).subscribe(documentId => {
            this.documentService.uploadFile(documentId, file!);
            this.dialogRef.close();
        });
    }

    cancel() {
        this.dialogRef.close();
    }
}
