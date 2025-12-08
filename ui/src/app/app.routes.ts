import { Routes } from '@angular/router';

import { SuccessComponent } from './success/success.component';
import { OauthCallbackComponent } from './oauth-callback/oauth-callback.component';
import { HomeComponent } from './Home/Home.component';

export const routes: Routes = [
  {path: '', component: HomeComponent},
  { path: 'success', component: SuccessComponent },
  { path: 'zoho/callback', component: OauthCallbackComponent },
  { path: '**', redirectTo: '', pathMatch: 'full' }
];
