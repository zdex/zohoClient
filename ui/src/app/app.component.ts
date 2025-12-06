import { Component, OnInit } from '@angular/core';
import { ZohoAuthService, ZohoTokenResponse } from './zoho-auth.service';
 import { JsonPipe, CommonModule, NgIf } from '@angular/common';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  standalone: true,
  imports: [JsonPipe, NgIf, CommonModule],
})
export class AppComponent implements OnInit {

  token: ZohoTokenResponse | null = null;

  constructor(private zohoService: ZohoAuthService) {}

  ngOnInit() {
    // Load current token on page load if present
    this.zohoService.getCurrentToken().subscribe(t => this.token = t);
  }

  connectToZoho() {
    this.zohoService.getAuthUrl().subscribe(url => {
      window.location.href = url;  // redirect to Zoho login
    });
  }
}
