import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UploadDialogComponent } from './upload-dialog.component';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { Component, forwardRef, Input } from '@angular/core';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { DocumentService } from '../documents.service';
import { of } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { FileSizePipe } from '../../utils/pipes/file-size.pipe';

@Component({
    selector: 'p-selectButton',
    standalone: true,
    template: '',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => PSelectButtonStub),
            multi: true
        }
    ]
})
class PSelectButtonStub implements ControlValueAccessor {
    @Input() options: any;
    @Input() optionLabel: any;
    @Input() optionValue: any;
    @Input() class: any;
    writeValue(obj: any): void { }
    registerOnChange(fn: any): void { }
    registerOnTouched(fn: any): void { }
    setDisabledState?(isDisabled: boolean): void { }
}

describe('UploadDialogComponent', () => {
    let component: UploadDialogComponent;
    let fixture: ComponentFixture<UploadDialogComponent>;
    let documentService: jasmine.SpyObj<DocumentService>;
    let dialogRef: jasmine.SpyObj<DynamicDialogRef>;

    beforeEach(async () => {
        documentService = jasmine.createSpyObj('DocumentService', ['createDraft', 'uploadFile']);
        dialogRef = jasmine.createSpyObj('DynamicDialogRef', ['close']);

        await TestBed.configureTestingModule({
            declarations: [UploadDialogComponent, FileSizePipe],
            imports: [ReactiveFormsModule, PSelectButtonStub],
            providers: [
                { provide: DocumentService, useValue: documentService },
                { provide: DynamicDialogRef, useValue: dialogRef }
            ],
            schemas: [NO_ERRORS_SCHEMA]
        })
            .compileComponents();

        fixture = TestBed.createComponent(UploadDialogComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should be invalid when empty', () => {
        expect(component.form.valid).toBeFalse();
    });

    it('should set title from filename if empty', () => {
        const file = new File([''], 'test-file.txt', { type: 'text/plain' });
        const event = { target: { files: [file] } } as any;

        component.onFileSelect(event);

        expect(component.fileControl.value).toBe(file);
        expect(component.titleControl.value).toBe('test-file');
    });

    it('should call document service on submit', () => {
        const file = new File([''], 'test.txt', { type: 'text/plain' });
        component.form.patchValue({
            title: 'Title',
            visibility: 'PRIVATE',
            file: file
        });

        documentService.createDraft.and.returnValue(of(123));

        component.submit();

        expect(documentService.createDraft).toHaveBeenCalledWith('Title', '', 'PRIVATE');
        expect(documentService.uploadFile).toHaveBeenCalledWith(123, file);
        expect(dialogRef.close).toHaveBeenCalled();
    });

    it('should close on cancel', () => {
        component.cancel();
        expect(dialogRef.close).toHaveBeenCalled();
    });
});