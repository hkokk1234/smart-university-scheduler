package com.smartuniversity.scheduler.catalog.infrastructure;

import com.smartuniversity.scheduler.platform.api.ApiException;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.io.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class TimetableFileParserTest {
    final TimetableFileParser parser =new TimetableFileParser();
    MockMultipartFile file(String name, byte[] bytes) { return new MockMultipartFile("file", name, "application/octet-stream", bytes); }
    byte[] bytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream output =new ByteArrayOutputStream(); workbook.write(output); return output.toByteArray();
    }
    @Test void readsBothExcelFormatsPreservesSparseColumnsAndDoesNotEvaluateFormulas() throws Exception {
        for (boolean xlsx : List.of(true, false)) {
            try (Workbook book =xlsx ? new XSSFWorkbook() : new HSSFWorkbook()) {
                Sheet sheet =book.createSheet("Πρόγραμμα");
                Row row =sheet.createRow(0);
                row.createCell(0).setCellValue("Μάθημα");
                row.createCell(2).setCellValue("Ώρα");
                row =sheet.createRow(1);
                row.createCell(0).setCellValue("Αλγόριθμοι");
                Cell time =row.createCell(2); time.setCellValue(9.0 / 24);
                CellStyle style =book.createCellStyle(); style.setDataFormat(book.createDataFormat().getFormat("hh:mm")); time.setCellStyle(style);
                row.createCell(3).setCellFormula("1+1");
                book.createSheet("Empty");
                var result =parser.parse(file(xlsx ? "schedule.xlsx" : "schedule.xls", bytes(book)));
                assertThat(result.tables()).hasSize(1);
                assertThat(result.tables().getFirst().rows().getFirst()).containsExactly("Μάθημα", "", "Ώρα");
                assertThat(result.tables().getFirst().rows().get(1)).containsExactly("Αλγόριθμοι", "", "09:00", "");
                assertThat(result.warnings()).anyMatch(warning -> warning.contains("Formula"));
            }
        }
    }
    @Test void reconstructsRealPdfColumnsAndRetainsPageSelection() throws Exception {
        byte[] bytes;
        try (PDDocument document =new PDDocument()) {
            document.addPage(new PDPage());
            PDPage page =new PDPage(new PDRectangle(1600, 500)); document.addPage(page);
            try (PDPageContentStream content =new PDPageContentStream(document, page)) {
                String[] cells ={"CS101", "Operating Systems", "6", "LECTURE", "A", "Monday", "09:00", "11:00"};
                float[] positions ={30, 150, 420, 500, 650, 730, 900, 1050};
                for (int i =0; i < cells.length; i++) {
                    content.beginText(); content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(positions[i], 400); content.showText(cells[i]); content.endText();
                }
            }
            ByteArrayOutputStream output =new ByteArrayOutputStream(); document.save(output); bytes =output.toByteArray();
        }
        var preview =parser.parse(file("timetable.pdf", bytes));
        assertThat(preview.tables()).hasSize(1);
        assertThat(preview.tables().getFirst().name()).isEqualTo("Page 2");
        assertThat(preview.tables().getFirst().rows().getFirst())
                .containsExactly("CS101", "Operating Systems", "6", "LECTURE", "A", "Monday", "09:00", "11:00");
    }
    @Test void reportsEmptyScannedCorruptUnsupportedAndOversizedInputsClearly() throws Exception {
        assertThatThrownBy(() -> parser.parse(file("schedule.xlsx", new byte[0]))).hasMessageContaining("non-empty");
        assertThatThrownBy(() -> parser.parse(file("schedule.exe", new byte[]{1}))).hasMessageContaining("Supported file types");
        assertThatThrownBy(() -> parser.parse(file("schedule.pdf", new byte[]{1}))).isInstanceOf(ApiException.class).hasMessageContaining("could not be read");
        assertThatThrownBy(() -> parser.parse(file("schedule.xlsx", new byte[5 * 1024 * 1024 + 1]))).hasMessageContaining("5 MB");
        try (PDDocument document =new PDDocument()) {
            document.addPage(new PDPage());
            ByteArrayOutputStream output =new ByteArrayOutputStream(); document.save(output);
            assertThatThrownBy(() -> parser.parse(file("scan.pdf", output.toByteArray()))).hasMessageContaining("OCR");
        }
    }
    @Test void rejectsExcessiveRowsOrColumnsInsteadOfSilentlyTruncating() throws Exception {
        try (Workbook book =new XSSFWorkbook()) {
            Sheet sheet =book.createSheet();
            for (int index =0; index < 501; index++) sheet.createRow(index).createCell(0).setCellValue("Data");
            byte[] data =bytes(book);
            assertThatThrownBy(() -> parser.parse(file("many.xlsx", data))).hasMessageContaining("500");
        }
        try (Workbook book =new XSSFWorkbook()) {
            book.createSheet().createRow(0).createCell(30).setCellValue("Data");
            byte[] data =bytes(book);
            assertThatThrownBy(() -> parser.parse(file("wide.xlsx", data))).hasMessageContaining("30 columns");
        }
    }
}
