package com.example.demo.controller;

import com.example.demo.dto.TextEditRequest;
import com.example.demo.service.PdfEditService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/pdf")
public class PdfEditController {

    private final PdfEditService service;

    public PdfEditController(PdfEditService service) {
        this.service = service;
    }

    // ✅ Legacy single-word replacement
    @PostMapping("/replace")
    public ResponseEntity<byte[]> replaceText(
            @RequestParam MultipartFile file,
            @RequestParam String search,
            @RequestParam String replace
    ) {
        byte[] pdf = service.replaceText(file, search, replace);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=edited.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ✅ Multi-word edits from frontend overlays
    @PostMapping("/edit")
    public ResponseEntity<byte[]> editPdf(
            @RequestParam MultipartFile file,
            @RequestParam("edits") String editsJson
    ) {
        List<TextEditRequest> edits = service.parseEdits(editsJson);
        byte[] pdf = service.applyEdits(file, edits);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=edited.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
