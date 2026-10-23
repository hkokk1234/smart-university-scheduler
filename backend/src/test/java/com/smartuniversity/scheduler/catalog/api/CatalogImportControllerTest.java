package com.smartuniversity.scheduler.catalog.api;

import com.smartuniversity.scheduler.catalog.application.CatalogImportService;
import com.smartuniversity.scheduler.catalog.infrastructure.TimetableFileParser;
import com.smartuniversity.scheduler.platform.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers =CatalogImportController.class, properties ="debug=false")
@Import(SecurityConfiguration.class)
class CatalogImportControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean TimetableFileParser parser;
    @MockitoBean CatalogImportService importer;
    @MockitoBean JwtDecoder decoder;
    final String root ="/api/v1/admin/catalog-import";
    final MockMultipartFile file =new MockMultipartFile("file", "schedule.xlsx", "application/octet-stream", new byte[]{1});
    @Test void anonymousAndStudentsCannotUploadOrCommit() throws Exception {
        mvc.perform(multipart(root + "/preview").file(file)).andExpect(status().isUnauthorized());
        mvc.perform(multipart(root + "/preview").file(file).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isForbidden());
        for (String endpoint : List.of("/validate", "/commit")) {
            mvc.perform(post(root + endpoint).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized());
            mvc.perform(post(root + endpoint).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(parser, importer);
    }
    @Test void administratorCanPreviewWithoutSaving() throws Exception {
        when(parser.parse(any())).thenReturn(new TimetableFileParser.Preview(List.of(
                new TimetableFileParser.Table("Sheet 1", List.of(List.of("Code", "Title")))), List.of()));
        mvc.perform(multipart(root + "/preview").file(file).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.tables[0].rows[0][0]").value("Code"));
        verifyNoInteractions(importer);
    }
    @Test void missingFileAndEmptyBatchReturn400() throws Exception {
        mvc.perform(multipart(root + "/preview").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest());
        for (String endpoint : List.of("/validate", "/commit")) mvc.perform(post(root + endpoint)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"semesterId\":\"" + UUID.randomUUID() + "\",\"rows\":[]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(parser, importer);
    }
    @Test void administratorCanValidateAndPublishMappedRows() throws Exception {
        when(importer.validate(any())).thenReturn(new CatalogImportReport(true, List.of(), 1, 1, 1));
        when(importer.commit(any())).thenReturn(new CatalogImportReport.Imported(1, 1, 1, 1));
        String body ="{\"semesterId\":\"" + UUID.randomUUID() + "\",\"rows\":[{\"courseCode\":\"CS101\"}]}";
        mvc.perform(post(root + "/validate").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(true));
        mvc.perform(post(root + "/commit").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.meetingsCreated").value(1))
                .andExpect(header().string("Cache-Control", "no-store"));
    }
}
