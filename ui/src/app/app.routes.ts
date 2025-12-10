import { Routes } from '@angular/router';

import { SuccessComponent } from './success/success.component';
import { OauthCallbackComponent } from './oauth-callback/oauth-callback.component';
import { HomeComponent } from './Home/Home.component';
import { ContactsComponent } from './general/contacts/contacts.component';
import { MilesComponent } from './general/miles/miles.component';
import { InvoiceUploadComponent } from './finance/invoice-upload/invoice-upload.component';


export const routes: Routes = [
  {path: '', component: HomeComponent},
  { path: 'success', component: SuccessComponent },
  { path: 'zoho/callback', component: OauthCallbackComponent },
  { path: 'contacts', component: ContactsComponent },
  { path: 'miles', component: MilesComponent },
  { path: 'upload-invoices', component: InvoiceUploadComponent },
  { path: '**', redirectTo: '', pathMatch: 'full' }
];
