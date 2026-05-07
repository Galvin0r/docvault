import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HomeComponent } from './home.component';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of } from 'rxjs';
import { DocumentService } from '../../documents/documents.service';
import { PrimeNgModule } from '../../primeng/primeng.module';

describe('HomeComponent', () => {
    let component: HomeComponent;
    let fixture: ComponentFixture<HomeComponent>;
    let documentService: jasmine.SpyObj<DocumentService>;

    beforeEach(async () => {
        documentService = jasmine.createSpyObj<DocumentService>('DocumentService', ['search', 'download']);
        documentService.search.and.returnValue(of({ content: [], totalElements: 0, size: 10, number: 0 }));

        await TestBed.configureTestingModule({
            declarations: [HomeComponent],
            imports: [CommonModule, ReactiveFormsModule, PrimeNgModule],
            providers: [
                { provide: DocumentService, useValue: documentService },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        data: of({ userInfo: null }),
                        snapshot: { queryParamMap: convertToParamMap({}) },
                        queryParamMap: of(convertToParamMap({})),
                    }
                },
                { provide: Router, useValue: jasmine.createSpyObj<Router>('Router', ['navigate']) },
            ],
            schemas: [NO_ERRORS_SCHEMA]
        })
            .compileComponents();

        fixture = TestBed.createComponent(HomeComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('detects document format from mime metadata', () => {
        expect(component.fileKind({
            title: 'LLM',
            mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            originalFilename: null,
            contentSnippet: null,
            highlightedContentSnippet: null
        } as any)).toBe('DOC');
    });

    it('detects document format from stub search snippet when metadata is missing', () => {
        expect(component.fileKind({
            title: 'LLM',
            mimeType: null,
            originalFilename: null,
            contentSnippet: 'Stub fragment generated for mime type application/pdf.',
            highlightedContentSnippet: null
        } as any)).toBe('PDF');
    });

    it('does not send semantic search request until content is present', () => {
        documentService.search.calls.reset();

        component.filtersForm.controls.mode.setValue('VECTOR', { emitEvent: false });
        (component as any).fetchResults().subscribe();

        expect(documentService.search).not.toHaveBeenCalled();
        expect(component.results().content).toEqual([]);

        component.filtersForm.controls.content.setValue('language models', { emitEvent: false });
        (component as any).fetchResults().subscribe();

        expect(documentService.search).toHaveBeenCalledOnceWith(jasmine.objectContaining({
            mode: 'VECTOR',
            content: 'language models',
        }));
    });
});