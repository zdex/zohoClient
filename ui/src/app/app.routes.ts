import { Routes } from '@angular/router';

import { SuccessComponent } from './success/success.component';
import { OauthCallbackComponent } from './oauth-callback/oauth-callback.component';

export const routes: Routes = [
   { path: '', component: SuccessComponent },
  { path: 'zoho/callback', component: OauthCallbackComponent },
  { path: '**', redirectTo: '' }
];
