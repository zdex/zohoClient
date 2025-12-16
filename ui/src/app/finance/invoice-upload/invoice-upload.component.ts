import { ChangeDetectorRef, Component } from '@angular/core';
import { InvoiceService, ParsedTransaction } from '../../services/invoice.service';
import { CommonModule } from '@angular/common';


@Component({
  selector: 'app-invoice-upload',
  templateUrl: './invoice-upload.component.html',
  styleUrls: ['./invoice-upload.component.scss'],
  standalone: true,
  imports: [CommonModule]
})
export class InvoiceUploadComponent {

  // File
  fileName = '';

  // Preview data
  previewData: ParsedTransaction[] = [];
  pagedData: ParsedTransaction[] = [];

  // UI state
  previewMode = false;
  loading = false;
  error = '';

  // Selection
  selectedRows = new Set<number>();
   //selectedRows: any[] = [];

  // Pagination
  page = 1;
  pageSize = 10;

  constructor(private invoiceService: InvoiceService, private cdr: ChangeDetectorRef) {}

  get totalPages(): number {
  return Math.ceil(this.previewData.length / this.pageSize);
}

  // ===============================
  // File selection handler
  // ===============================
  onFileSelected(event: Event, fileInput: HTMLInputElement) {
    const input = event.target as HTMLInputElement;

    if (!input.files || input.files.length === 0) {
      return;
    }

    const file = input.files[0];
    this.fileName = file.name;

    // Reset state
    this.resetState();

    this.loading = true;

  this.invoiceService.previewSheet(file).subscribe({
  next: (data) => {
    console.log('Preview response:', data);

    this.previewData = data;
    this.previewMode = true;
    this.page = 1;
    this.updatePage();

    this.loading = false;

    // 🔥 REQUIRED in zone-less Angular
    this.cdr.detectChanges();
  },
  error: (err) => {
    console.error(err);
    this.error = 'Failed to preview file';
    this.loading = false;

    this.cdr.detectChanges();
  }
});


    // 🔥 critical fix: allow re-selecting same file
    fileInput.value = '';
  }

  // ===============================
  // Pagination
  // ===============================
  updatePage() {
    const start = (this.page - 1) * this.pageSize;
    this.pagedData = this.previewData.slice(start, start + this.pageSize);
  }

  nextPage() {
    if ((this.page * this.pageSize) < this.previewData.length) {
      this.page++;
      this.updatePage();
    }
  }

  prevPage() {
    if (this.page > 1) {
      this.page--;
      this.updatePage();
    }
  }

  // ===============================
  // Row selection
  // ===============================
  toggleRow(tx: ParsedTransaction) {
    if (this.selectedRows.has(tx.row)) {
      this.selectedRows.delete(tx.row);
    } else {
      this.selectedRows.add(tx.row);
    }
  }

  isSelected(tx: ParsedTransaction): boolean {
    return this.selectedRows.has(tx.row);
  }

  selectAllVisible() {
    this.pagedData.forEach(tx => this.selectedRows.add(tx.row));
  }

  clearSelection() {
    this.selectedRows.clear();
  }

  // ===============================
  // Process invoices
  // ===============================
  processSelected() {
    if (this.selectedRows.size === 0) {
      alert('Please select at least one transaction.');
      return;
    }

    const rows = Array.from(this.selectedRows.values());
    this.loading = true;

    this.invoiceService.processSelectedRows(rows).subscribe({
      next: () => {
        alert('Invoices created successfully');
        this.resetState();
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        alert('Failed to create invoices');
        this.loading = false;
      }
    });
  }

  // ===============================
  // Helpers
  // ===============================
  isDuplicate(tx: ParsedTransaction): boolean {
    return tx.duplicate === true;
  }

  resetState() {
    this.previewData = [];
    this.pagedData = [];
    this.selectedRows.clear();
    this.previewMode = false;
    this.error = '';
    this.page = 1;
  }
}
