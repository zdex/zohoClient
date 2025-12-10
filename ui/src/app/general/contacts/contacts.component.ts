import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-contacts',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './contacts.component.html',
  styleUrls: ['./contacts.component.scss']
})
export class ContactsComponent {

  contacts: any[] = [];
  uploading = false;

  constructor(private http: HttpClient) {}

  fetchContacts() {
    this.http.get<any[]>('http://localhost:8080/api/contacts')
      .subscribe({
        next: data => this.contacts = data,
        error: err => console.error(err)
      });
  }

  uploadCsv(event: any) {
    const file = event.target.files?.[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    this.uploading = true;

    this.http.post('http://localhost:8080/api/contacts/upload', formData)
      .subscribe({
        next: res => {
          alert("Contacts uploaded successfully!");
          this.uploading = false;
        },
        error: err => {
          console.error(err);
          this.uploading = false;
        }
      });
  }
}
