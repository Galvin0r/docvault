import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthLayoutComponent } from './auth-layout.component';
import { RouterModule } from '@angular/router';
import { NO_ERRORS_SCHEMA } from '@angular/core';

describe('AuthLayoutComponent', () => {
    let component: AuthLayoutComponent;
    let fixture: ComponentFixture<AuthLayoutComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [AuthLayoutComponent],
            imports: [RouterModule.forRoot([])],
            schemas: [NO_ERRORS_SCHEMA]
        })
            .compileComponents();

        fixture = TestBed.createComponent(AuthLayoutComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});