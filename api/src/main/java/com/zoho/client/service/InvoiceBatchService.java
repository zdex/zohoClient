package com.zoho.client.service;

import com.zoho.client.config.ZohoProperties;
import com.zoho.client.model.InvoiceRequestDto;
import com.zoho.client.model.ParsedTransaction;
import com.zoho.client.model.ZohoTokenResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InvoiceBatchService {

    private final ZohoInvoiceService invoiceService;

    public InvoiceBatchService(ZohoInvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    public Map<String, Object> processSelectedRows(List<ParsedTransaction> rows) throws Exception {

        List<Map<String, Object>> results = new ArrayList<>();
        int count = 0;

        for (ParsedTransaction tx : rows) {

            InvoiceRequestDto dto = new InvoiceRequestDto();
            dto.setCustomerName(tx.customerName);
            dto.setAmount(tx.amount);
            dto.setDate(tx.date);
            dto.setDescription(tx.description);
            dto.setTransactionReference(tx.referenceNumber);

            JsonNode res = invoiceService.createInvoiceWithPayment(dto);

            Map<String, Object> record = new HashMap<>();
            record.put("row", tx.row);
            record.put("invoice_id", res.path("invoice").path("invoice_id").asText());
            record.put("payment_id", res.path("payment").path("payment_id").asText());
            results.add(record);

            count++;
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("count", count);
        summary.put("invoices", results);

        return summary;
    }


    public Map<String, Object> processSheet(MultipartFile file) throws Exception {

        List<Map<String, Object>> results = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        int count = 0;

        for (int i = 1; i <= sheet.getLastRowNum(); i++) { // skip header
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String dateStr = row.getCell(0).getStringCellValue();
            double amount = row.getCell(1).getNumericCellValue();
            String description = row.getCell(2).getStringCellValue();

            LocalDate date = LocalDate.parse(dateStr);

            String customer = extractCustomerName(description);
            String refNumber = extractReferenceNumber(description);

            InvoiceRequestDto dto = new InvoiceRequestDto();
            dto.setCustomerName(customer);
            dto.setAmount(amount);
            dto.setDate(date);
            dto.setDescription(description);
            dto.setTransactionReference(refNumber);  // ← NEW!

            JsonNode response = invoiceService.createInvoiceWithPayment(dto);

            Map<String, Object> record = new HashMap<>();
            record.put("row", i);
            record.put("customer", customer);
            record.put("reference_number", refNumber);
            record.put("invoice_id", response.path("invoice").path("invoice_id").asText());
            record.put("payment_id", response.path("payment").path("payment_id").asText());

            results.add(record);
            count++;
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("count", count);
        summary.put("invoices", results);

        return summary;
    }

    private String extractCustomerName(String description) {
        Pattern p = Pattern.compile("FROM (.*?) ON", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(description);
        if (m.find()) {
            return m.group(1).trim();
        }
        return "Unknown Customer";
    }

    private String extractReferenceNumber(String description) {
        Pattern p = Pattern.compile("REF #\\s*([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(description);
        if (m.find()) {
            return m.group(1).trim();
        }
        return "N/A";
    }

    public List<ParsedTransaction> previewSheet(MultipartFile file) throws Exception {

        List<ParsedTransaction> list = new ArrayList<>();

        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        Set<String> seenRefs = new HashSet<>();
        Set<String> seenDescriptions = new HashSet<>();

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {

            Row row = sheet.getRow(i);
            if (row == null) continue;

            // SKIP COMPLETELY BLANK ROWS
            if (isRowEmpty(row)) continue;

            // Required cells: date, amount, description
            Cell dateCell = row.getCell(0);
            Cell amountCell = row.getCell(1);
            Cell descCell = row.getCell(2);

            // Skip rows missing required values
            if (dateCell == null || amountCell == null || descCell == null) continue;
            if (descCell.getCellType() == CellType.BLANK) continue;

            String descStr = descCell.getStringCellValue();
            if (descStr == null || descStr.trim().isEmpty()) continue;

            ParsedTransaction tx = new ParsedTransaction();

            tx.row = i;
            tx.date = parseDate(dateCell);
            tx.amount = amountCell.getNumericCellValue();
            tx.description = descStr.trim();
            tx.customerName = extractCustomerName(tx.description);
            tx.referenceNumber = extractReferenceNumber(tx.description);

            // Duplicate detection
            boolean dup = false;

            if (seenRefs.contains(tx.referenceNumber)) dup = true;
            if (seenDescriptions.contains(tx.description)) dup = true;

            tx.duplicate = dup;

            seenRefs.add(tx.referenceNumber);
            seenDescriptions.add(tx.description);

            list.add(tx);
        }

        return list;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;

        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                if (cell.getCellType() == CellType.STRING && !cell.getStringCellValue().trim().isEmpty())
                    return false;
                if (cell.getCellType() != CellType.STRING)
                    return false;
            }
        }
        return true;
    }


    private LocalDate parseDate(Cell cell) {

        if (cell == null) return null;

        // Case 1: Excel stores the date as a NUMBER (most common)
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }

        // Case 2: Excel stores date as a TEXT string
        if (cell.getCellType() == CellType.STRING) {
            String text = cell.getStringCellValue().trim();

            // Try MM/dd/yyyy
            DateTimeFormatter mmdd = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            try { return LocalDate.parse(text, mmdd); }
            catch (Exception ignored) {}

            // Try dd/MM/yyyy
            DateTimeFormatter ddmm = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            try { return LocalDate.parse(text, ddmm); }
            catch (Exception ignored) {}

            // Try yyyy-MM-dd (ISO)
            try { return LocalDate.parse(text); }
            catch (Exception ignored) {}
        }

        throw new RuntimeException("Invalid date format: " + cell.toString());
    }

}


