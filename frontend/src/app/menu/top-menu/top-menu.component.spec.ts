import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TopMenuComponent } from './top-menu.component';
import { Router, ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { SecurityService } from '../../security/security.service';
import { DialogService } from 'primeng/dynamicdialog';
import { MessageService } from 'primeng/api';
import { Observable, of } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';

describe('TopMenuComponent', () => {
    let component: TopMenuComponent;
    let fixture: ComponentFixture<TopMenuComponent>;
    let router: jasmine.SpyObj<Router>;
    let securityService: jasmine.SpyObj<SecurityService>;
    let dialogService: jasmine.SpyObj<DialogService>;
    let messageService: jasmine.SpyObj<MessageService>;

    beforeEach(async () => {
        router = jasmine.createSpyObj('Router', ['navigate']);
        securityService = jasmine.createSpyObj('SecurityService', ['logout']);
        dialogService = jasmine.createSpyObj('DialogService', ['open']);
        messageService = jasmine.createSpyObj('MessageService', ['add']);

        await TestBed.configureTestingModule({
            declarations: [TopMenuComponent],
            imports: [ReactiveFormsModule],
            providers: [
                { provide: Router, useValue: router },
                { provide: SecurityService, useValue: securityService },
                { provide: DialogService, useValue: dialogService },
                { provide: MessageService, useValue: messageService },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        data: of({ userInfo: { login: 'testuser', email: 'test@example.com' } })
                    }
                }
            ],
            schemas: [NO_ERRORS_SCHEMA]
        })
            .compileComponents();

        fixture = TestBed.createComponent(TopMenuComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should initialize with userInfo from route data', () => {
        expect(component.userInfo).toEqual({ login: 'testuser', email: 'test@example.com' } as any);
    });

    it('should navigate home on home()', () => {
        component.home();
        expect(router.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should navigate home with content query on search', () => {
        component.searchControl.setValue('  solar clinic  ');
        component.search();

        expect(router.navigate).toHaveBeenCalledWith(['/'], {
            queryParams: { content: 'solar clinic' }
        });
    });

    it('should call logout on logout()', (done) => {
        securityService.logout.and.returnValue(new Observable());

        component.logout();

        expect(securityService.logout).toHaveBeenCalled();
        setTimeout(() => {
            done();
        }, 10);
    });

    it('should open upload dialog if user is logged in', () => {
        component.userInfo = { login: 'testuser' } as any;
        component.openUploadDialog();
        expect(dialogService.open).toHaveBeenCalled();
    });

    it('should show error message if user tries to upload while logged out', () => {
        component.userInfo = null;
        component.openUploadDialog();
        expect(messageService.add).toHaveBeenCalledWith(jasmine.objectContaining({
            severity: 'info',
            summary: 'Authentication Required'
        }));
        expect(dialogService.open).not.toHaveBeenCalled();
    });
});