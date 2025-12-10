package com.zoho.client.controller;

import com.zoho.client.service.ZohoContactsService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
public class ContactsController {

    private final ZohoContactsService contactsService;

    public ContactsController(ZohoContactsService contactsService) {
        this.contactsService = contactsService;
    }

    @GetMapping
    public List<Object> getAllContacts() {
        return contactsService.fetchContacts();
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadCsv(@RequestParam("file") MultipartFile file) {
        contactsService.uploadContactsCsv(file);
        return "Contacts uploaded successfully";
    }
}
