import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';


export interface ZohoTokenResponse {
  access_token: string;
  refresh_token: string;
  scope: string;
  api_domain: string;
  token_type: string;
  expires_in: number;
}

@Injectable({ providedIn: 'root' })
export class ZohoAuthService {

  private backendBase = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  getAuthUrl(): Observable<string> {
    return this.http.get(this.backendBase + '/auth/url', { responseType: 'text' , withCredentials: true });
  }

  exchangeCode(code: string): Observable<ZohoTokenResponse> {
    return this.http.post<ZohoTokenResponse>(this.backendBase + '/auth/exchange', { code }, { withCredentials: true });
  }

  getCurrentToken(): Observable<ZohoTokenResponse | null> {
    return this.http.get<ZohoTokenResponse | null>(this.backendBase + '/auth/token', { withCredentials: true });
  }
}
