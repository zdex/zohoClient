import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { OauthCallbackComponent } from './oauth-callback/oauth-callback.component';

export const routes: Routes = [
  { path: 'zoho/callback', component: OauthCallbackComponent },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {

  }
