import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-invoice-upload',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './invoice-upload.component.html',
  styleUrl: './invoice-upload.component.scss'
})
export class InvoiceUploadComponent {

  fileName = '';
  previewData: any[] = [];
  selectedRows: any[] = [];
  previewMode = false;
  processing = false;

  success = false;
  result: any = null;

  page = 1;
  pageSize = 10;

  constructor(private http: HttpClient) {}

  get pagedData() {
    const start = (this.page - 1) * this.pageSize;
    return this.previewData.slice(start, start + this.pageSize);
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (!file) return;

    this.fileName = file.name;
    const formData = new FormData();
    formData.append("file", file);

    this.http.post<any[]>('http://localhost:8080/api/invoices/preview', formData)
      .subscribe(res => {
        this.previewData = res;
        this.selectedRows = res.filter(r => !r.duplicate);
        this.previewMode = true;
      });
  }

  toggleSelection(row: any) {
    if (this.selectedRows.includes(row)) {
      this.selectedRows = this.selectedRows.filter(r => r !== row);
    } else {
      this.selectedRows.push(row);
    }
  }

  createInvoices() {
    this.processing = true;

    this.http.post('http://localhost:8080/api/invoices/batch', this.selectedRows)
      .subscribe(res => {
        this.processing = false;
        this.success = true;
        this.result = res;
      });
  }
}
