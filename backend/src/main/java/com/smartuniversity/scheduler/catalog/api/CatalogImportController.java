package com.smartuniversity.scheduler.catalog.api;

import com.smartuniversity.scheduler.catalog.application.CatalogImportService;
import com.smartuniversity.scheduler.catalog.infrastructure.TimetableFileParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/catalog-import")
@Tag(name ="Catalog Import")
@SecurityRequirement(name ="bearerAuth")
public class CatalogImportController {
    private final TimetableFileParser parser;
    private final CatalogImportService importer;
    public CatalogImportController(TimetableFileParser parser, CatalogImportService importer) {
        this.parser =parser; this.importer =importer;
    }
    @PostMapping(value ="/preview", consumes =MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary ="ADMIN: extract editable tables from PDF or Excel without writing to the catalog")
    public ResponseEntity<TimetableFileParser.Preview> preview(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(parser.parse(file));
    }
    @PostMapping("/validate")
    @Operation(summary ="ADMIN: validate mapped meeting rows and identify cell errors without saving")
    public ResponseEntity<CatalogImportReport> validate(@Valid @RequestBody CatalogImportRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(importer.validate(request));
    }
    @PostMapping("/commit")
    @Operation(summary ="ADMIN: revalidate and atomically create courses, groups, sections and meetings")
    public ResponseEntity<CatalogImportReport.Imported> commit(@Valid @RequestBody CatalogImportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(importer.commit(request));
    }
}
