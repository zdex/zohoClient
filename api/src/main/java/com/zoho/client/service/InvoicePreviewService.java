package com.zoho.client.service;

import com.zoho.client.cache.PreviewCache;
import com.zoho.client.model.ParsedTransaction;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InvoicePreviewService {

    private final PreviewCache previewCache;

    public InvoicePreviewService(PreviewCache previewCache) {
        this.previewCache = previewCache;
    }

    // ==============================
    // Public API
    // ==============================
    public List<ParsedTransaction> previewSheet(MultipartFile file) throws Exception {

        List<ParsedTransaction> list = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            Set<String> seenRefs = new HashSet<>();
            Set<String> seenDescriptions = new HashSet<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                ParsedTransaction tx = new ParsedTransaction();
                tx.setRow(i);

                tx.setDate(parseDate(row.getCell(0)));
                tx.setAmount(getNumeric(row.getCell(1)));
                tx.setDescription(getString(row.getCell(2)));

                tx.setCustomerName(extractCustomerName(tx.getDescription()));
                tx.setTransactionReference(extractReferenceNumber(tx.getDescription()));

                boolean duplicate = false;

                if (tx.getTransactionReference() != null &&
                        seenRefs.contains(tx.getTransactionReference())) {
                    duplicate = true;
                }

                if (seenDescriptions.contains(tx.getDescription())) {
                    duplicate = true;
                }

                tx.setDuplicate(duplicate);

                if (tx.getTransactionReference() != null) {
                    seenRefs.add(tx.getTransactionReference());
                }
                seenDescriptions.add(tx.getDescription());

                list.add(tx);
            }
        }

        previewCache.store(list); // 🔥 critical

        return list;
    }

    // ==============================
    // Helpers
    // ==============================

    private boolean isRowEmpty(Row row) {
        for (int c = 0; c < 3; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private LocalDate parseDate(Cell cell) {

        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC &&
                DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }

        if (cell.getCellType() == CellType.STRING) {
            String value = cell.getStringCellValue().trim();
            if (value.isEmpty()) return null;

            DateTimeFormatter[] formats = {
                    DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                    DateTimeFormatter.ofPattern("M/d/yyyy"),
                    DateTimeFormatter.ISO_LOCAL_DATE
            };

            for (DateTimeFormatter f : formats) {
                try {
                    return LocalDate.parse(value, f);
                } catch (Exception ignored) {}
            }
        }

        return null;
    }

    private double getNumeric(Cell cell) {
        if (cell == null) return 0.0;

        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }

        if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue().trim());
            } catch (Exception ignored) {}
        }

        return 0.0;
    }

    private String getString(Cell cell) {
        if (cell == null) return "";
        return cell.getCellType() == CellType.STRING
                ? cell.getStringCellValue().trim()
                : "";
    }

    private String extractCustomerName(String description) {

        if (description == null) return "Unknown";

        Pattern p = Pattern.compile(
                "FROM\\s+(.*?)\\s+(ON|REF)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher m = p.matcher(description);
        if (m.find()) {
            return m.group(1).trim();
        }
        return "Unknown";
    }

    private String extractReferenceNumber(String description) {

        if (description == null) return null;

        Pattern p = Pattern.compile(
                "REF\\s*#\\s*([A-Z0-9]+)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher m = p.matcher(description);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }
}
