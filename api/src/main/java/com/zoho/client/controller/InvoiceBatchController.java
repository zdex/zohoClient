// src/main/java/com/zoho/client/controller/InvoiceController.java
package com.zoho.client.controller;


import com.zoho.client.model.InvoiceRequestDto;
import com.zoho.client.model.ParsedTransaction;
import com.zoho.client.model.ProcessRowsRequest;
import com.zoho.client.service.InvoiceBatchService;
import com.zoho.client.service.InvoicePreviewService;
import com.zoho.client.service.ZohoInvoiceService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceBatchController {

    private final InvoiceBatchService batchService;
    private InvoicePreviewService invoicePreviewService;

    public InvoiceBatchController(InvoiceBatchService batchService, InvoicePreviewService invoicePreviewService) {
        this.batchService = batchService;
    }

   /* @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> processSheet(@RequestParam("file") MultipartFile file) throws Exception {
        return batchService.processSheet(file);
    }*/

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<ParsedTransaction> preview(@RequestParam("file") MultipartFile file) throws Exception {
        //return invoicePreviewService.previewSheet(file);
        return batchService.previewSheet(file);
    }

/*    @PostMapping("/process")
    public Map<String, Object> processSelected(@RequestBody List<ParsedTransaction> selected) throws Exception {
        return batchService.processSelectedRows(selected);
    }*/

    @PostMapping("/process")
    public void processSelected(@RequestBody ProcessRowsRequest request) throws Exception {
        batchService.processSelectedRows(request.getRows());
    }


}

