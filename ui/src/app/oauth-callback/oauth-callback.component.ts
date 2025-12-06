import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ZohoAuthService } from '../zoho-auth.service';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-oauth-callback',
  templateUrl: './oauth-callback.html',
  styleUrls: ['./oauth-callback.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
})
export class OauthCallbackComponent implements OnInit {

    private route = inject(ActivatedRoute);
    private router = inject(Router);
    private authService = inject(ZohoAuthService);
    error = '';

    ngOnInit() {
      this.route.queryParams.subscribe(params => {
        const code = params['code'];
        const state = params['state'];

        if (!code) {
          this.error = "No authorization code received!";
          return;
        }

        // send code to backend
        this.authService.exchangeCode(code).subscribe({
          next: () => this.router.navigateByUrl('/success'),
          error: err => this.error = err.message
        });
      });
    }
}
