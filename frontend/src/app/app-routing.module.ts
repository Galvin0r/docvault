import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MainLayoutComponent } from './menu/main-layout/main-layout.component';
import { AuthLayoutComponent } from './menu/auth-layout/auth-layout.component';
import { HomeComponent } from './menu/home/home.component';
import { userResolver } from './security/security.service';

const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    resolve: {
      userInfo: userResolver,
    },
    children: [
      {
        path: '',
        pathMatch: 'full',
        component: HomeComponent,
      },
    ],
  },
  {
    path: '',
    component: AuthLayoutComponent,
    loadChildren: () => import('./security/security.module').then((m) => m.SecurityModule),
  },
  {
    path: '**',
    redirectTo: '',
  },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
