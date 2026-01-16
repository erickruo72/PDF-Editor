package com.example.demo.controller;

import com.example.demo.dto.TextEditRequest;
import com.example.demo.pdf.TextLocation;
import com.example.demo.service.PdfEditService;
import com.example.demo.service.PdfService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    private final PdfService pdfService;
    private final PdfEditService pdfEditService;

    public PdfController(PdfService pdfService, PdfEditService pdfEditService) {
        this.pdfService = pdfService;
        this.pdfEditService = pdfEditService;
    }

    // Extract plain text from PDF
    @PostMapping(
            value = "/extract",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public String extractText(@RequestParam("file") MultipartFile file) {
        return pdfService.extractText(file);
    }

    // Extract words with positions for frontend overlays
    @PostMapping(
            value = "/words",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<Map<String, Object>> getWordPositions(@RequestParam("file") MultipartFile file) {
        List<TextLocation> locations = pdfService.extractWordLocations(file);

        return locations.stream().map(loc -> Map.<String, Object>of(
                "page", (Object) loc.page(),
                "x", (Object) loc.x(),
                "y", (Object) loc.y(),
                "width", (Object) loc.width(),
                "height", (Object) loc.height(),
                "text", loc.text()
        )).toList();

    }

    // Apply edits from frontend to PDF
    @PostMapping(
            value = "/edit",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public byte[] editPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("edits") String editsJson
    ) {
        // Convert JSON string to list of TextEditRequest
        List<TextEditRequest> edits = pdfEditService.parseEdits(editsJson);
        return pdfEditService.applyEdits(file, edits);
    }
}
