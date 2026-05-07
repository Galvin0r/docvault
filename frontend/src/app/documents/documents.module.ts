import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { DataViewModule } from 'primeng/dataview';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { DialogModule } from 'primeng/dialog';
import { ProgressBarModule } from 'primeng/progressbar';
import { SelectButtonModule } from 'primeng/selectbutton';
import { DocumentListComponent } from './document-list/document-list.component';
import { SimpleDocumentComponent } from './simple-document/simple-document.component';
import { UploadDialogComponent } from './upload-dialog/upload-dialog.component';
import { UploadProgressComponent } from './upload-progress/upload-progress.component';
import { DocumentInfoDialogComponent } from './document-info-dialog/document-info-dialog.component';
import { DocumentViewComponent } from './document-view/document-view.component';
import { UtilsModule } from '../utils/utils.module';

@NgModule({
    declarations: [
        DocumentListComponent,
        SimpleDocumentComponent,
        UploadDialogComponent,
        UploadProgressComponent,
        DocumentInfoDialogComponent,
        DocumentViewComponent,
    ],
    imports: [
        CommonModule,
        RouterModule,
        FormsModule,
        ReactiveFormsModule,
        DataViewModule,
        CardModule,
        ButtonModule,
        TooltipModule,
        InputTextModule,
        TextareaModule,
        DialogModule,
        ProgressBarModule,
        SelectButtonModule,
        UtilsModule,
    ],
    exports: [
        DocumentListComponent,
        SimpleDocumentComponent,
        UploadDialogComponent,
        UploadProgressComponent,
        DocumentInfoDialogComponent,
        DocumentViewComponent,
    ],
})
export class DocumentsModule { }