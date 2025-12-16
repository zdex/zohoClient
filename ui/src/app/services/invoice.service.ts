import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ParsedTransaction {
  row: number;
  date: string;
  amount: number;
  description: string;
  customerName: string;
  referenceNumber: string;
  duplicate: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class InvoiceService {

  private readonly baseUrl = 'http://localhost:8080/api/invoices';

  constructor(private http: HttpClient) {}

  // 🔹 Preview Excel file (multipart/form-data)
  previewSheet(file: File): Observable<ParsedTransaction[]> {
    const formData = new FormData();
    formData.append('file', file, file.name); // MUST be 'file'

    return this.http.post<ParsedTransaction[]>(
      `${this.baseUrl}/preview`,
      formData
      // ❌ DO NOT set Content-Type header
    );
  }

  // 🔹 Process selected rows (create invoices)
  processSelectedRows(rows: number[]): Observable<any> {
    return this.http.post(
      `${this.baseUrl}/process`,
      { rows }   // JSON body
    );
  }

  // 🔹 Fetch invoices by reference number
  getInvoicesByReference(ref: string, page = 1, perPage = 20): Observable<any> {
    return this.http.get(
      `${this.baseUrl}/search`,
      {
        params: {
          reference_number: ref,
          page: page.toString(),
          per_page: perPage.toString()
        }
      }
    );
  }
}
