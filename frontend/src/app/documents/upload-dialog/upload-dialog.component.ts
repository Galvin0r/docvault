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

    titleControl = new FormControl('', [Validators.required, Validators.maxLength(255)]);
    descriptionControl = new FormControl('', [Validators.maxLength(1000)]);
    visibilityControl = new FormControl('PRIVATE', [Validators.required]);
    fileControl = new FormControl<File | null>(null, [Validators.required]);

    form = this.fb.group({
        title: this.titleControl,
        description: this.descriptionControl,
        visibility: this.visibilityControl,
        file: this.fileControl
    });

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
        if (!this.form.valid) {
            return;
        }

        const title = this.titleControl.value!;
        const description = this.descriptionControl.value;
        const visibility = this.visibilityControl.value!;
        const file = this.fileControl.value!;

        this.documentService.createDraft(title, description, visibility as Visibility).subscribe(documentId => {
            this.documentService.uploadFile(documentId, file);
            this.dialogRef.close();
        });
    }

    formatSize(bytes: number): string {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
    }

    cancel() {
        this.dialogRef.close();
    }
}
