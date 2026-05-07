import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MainLayoutComponent } from './menu/main-layout/main-layout.component';
import { AuthLayoutComponent } from './menu/auth-layout/auth-layout.component';
import { HomeComponent } from './menu/home/home.component';
import { DocumentViewComponent } from './documents/document-view/document-view.component';
import { currentUserResolver } from './security/security.service';
import { mainLayoutCanMatch } from './utils/consts';

const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    canMatch: [mainLayoutCanMatch],
    resolve: {
      userInfo: currentUserResolver,
    },
    children: [
      {
        path: '',
        pathMatch: 'full',
        component: HomeComponent,
      },
      { 
        path: 'home', 
        redirectTo: '', 
        pathMatch: 'full' 
      },
      {
        path: 'document/:id',
        component: DocumentViewComponent,
      },
      {
        path: '',
        loadChildren: () => import('./groups/groups.module').then((m) => m.GroupsModule),
      }
    ],
  },
  {
    path: '',
    component: AuthLayoutComponent,
    children: [
      {
        path: '',
        loadChildren: () => import('./security/security.module').then((m) => m.SecurityModule),
      },
    ],
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