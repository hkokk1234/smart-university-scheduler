package com.smartuniversity.scheduler.catalog.infrastructure;

import com.smartuniversity.scheduler.platform.api.ApiException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.util.*;

@Component
public class TimetableFileParser {
    public static final int MAX_ROWS =500, MAX_COLUMNS =30, MAX_TABLES =20;
    public record Table(String name, List<List<String>> rows) {}
    public record Preview(List<Table> tables, List<String> warnings) {}

    public Preview parse(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
            throw invalid("Choose a non-empty PDF or Excel file up to 5 MB.");
        String name =Objects.toString(file.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".pdf")) return pdf(file.getBytes());
            if (name.endsWith(".xlsx") || name.endsWith(".xls")) return excel(file.getInputStream());
            throw invalid("Supported file types are .pdf, .xlsx and .xls.");
        } catch (ApiException error) { throw error;
        } catch (IOException | RuntimeException error) {
            throw invalid("The file could not be read. Check that it is a valid, unencrypted PDF or Excel workbook.");
        }
    }
    private Preview excel(InputStream input) throws IOException {
        List<Table> tables =new ArrayList<>(); List<String> warnings =new ArrayList<>();
        int count =0;
        // Formula cells are never evaluated. Uploads are parsed only; no external links are fetched.
        try (input; Workbook workbook =WorkbookFactory.create(input)) {
            if (workbook.getNumberOfSheets() > MAX_TABLES) throw invalid("At most 20 worksheets are supported.");
            DataFormatter format =new DataFormatter(Locale.ROOT);
            for (Sheet sheet : workbook) {
                List<List<String>> rows =new ArrayList<>();
                for (Row row : sheet) {
                    if (row.getLastCellNum() > MAX_COLUMNS) throw invalid("At most 30 columns are supported.");
                    List<String> cells =new ArrayList<>();
                    for (int column =0; column < Math.max(0, row.getLastCellNum()); column++) {
                        Cell cell =row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        if (cell !=null && (cell.getCellType() ==CellType.FORMULA || cell.getCellType() ==CellType.ERROR)) {
                            cells.add("");
                            if (warnings.isEmpty()) warnings.add("Formula and error cells were left blank. Enter their values before importing.");
                        } else cells.add(bounded(cell ==null ? "" : format.formatCellValue(cell).trim()));
                    }
                    if (cells.stream().allMatch(String::isBlank)) continue;
                    if (++count > MAX_ROWS) throw invalid("At most 500 non-empty rows are supported per file.");
                    rows.add(List.copyOf(cells));
                }
                if (!rows.isEmpty()) tables.add(new Table(sheet.getSheetName(), List.copyOf(rows)));
            }
        }
        if (tables.isEmpty()) throw invalid("The workbook contains no readable rows.");
        return new Preview(List.copyOf(tables), List.copyOf(warnings));
    }
    private Preview pdf(byte[] bytes) throws IOException {
        List<Table> tables =new ArrayList<>(); int count =0;
        try (PDDocument document =Loader.loadPDF(bytes)) {
            if (!document.getCurrentAccessPermission().canExtractContent()) throw invalid("This PDF does not allow text extraction.");
            if (document.getNumberOfPages() > MAX_TABLES) throw invalid("At most 20 PDF pages are supported.");
            for (int page =1; page <=document.getNumberOfPages(); page++) {
                PositionedText extractor =new PositionedText();
                extractor.setStartPage(page); extractor.setEndPage(page);
                extractor.getText(document);
                List<List<String>> rows =extractor.rows();
                count +=rows.size();
                if (count > MAX_ROWS) throw invalid("At most 500 non-empty rows are supported per file.");
                if (!rows.isEmpty()) tables.add(new Table("Page " + page, rows));
            }
        }
        if (tables.isEmpty()) throw invalid("No readable text was found. Scanned PDFs need OCR first; alternatively upload an Excel file.");
        return new Preview(List.copyOf(tables), List.of(
                "PDF columns are inferred from text positions. Check column mapping, remove headings and correct every row before importing. Scanned PDFs are not supported."));
    }
    private static String bounded(String value) {
        if (value.length() > 500) throw invalid("A cell exceeds 500 characters. Simplify the source file before uploading.");
        return value;
    }
    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IMPORT_FILE", message);
    }
    /** Reconstruct horizontal rows and column gaps without assuming a specific university layout. */
    private static final class PositionedText extends PDFTextStripper {
        private final NavigableMap<Float, List<TextPosition>> lines =new TreeMap<>();
        private int characters;
        PositionedText() throws IOException { super(); }
        @Override protected void processTextPosition(TextPosition text) {
            if (++characters > 300000) throw invalid("This PDF page contains too much text. Split the document before uploading.");
            float y =text.getYDirAdj();
            Float key =lines.floorKey(y + 2);
            if (key ==null || Math.abs(key - y) > 2) key =y;
            lines.computeIfAbsent(key, ignored -> new ArrayList<>()).add(text);
        }
        List<List<String>> rows() {
            List<List<String>> rows =new ArrayList<>();
            for (List<TextPosition> line : lines.values()) {
                line.sort(Comparator.comparing(TextPosition::getXDirAdj));
                StringBuilder text =new StringBuilder(); float end =-1;
                for (TextPosition character : line) {
                    float gap =character.getXDirAdj() - end;
                    if (end >=0 && gap > 12) text.append('\t');
                    else if (end >=0 && gap > 2 && !text.isEmpty() && !Character.isWhitespace(text.charAt(text.length() - 1))) text.append(' ');
                    text.append(character.getUnicode());
                    end =character.getXDirAdj() + character.getWidthDirAdj();
                }
                String content =text.toString().trim();
                if (content.isBlank()) continue;
                List<String> cells =Arrays.stream(content.split("\\t+| {3,}|\\s*\\|\\s*", -1)).map(String::trim).map(TimetableFileParser::bounded).toList();
                if (cells.size() > MAX_COLUMNS) throw invalid("At most 30 columns are supported.");
                rows.add(cells);
            }
            return List.copyOf(rows);
        }
    }
}
