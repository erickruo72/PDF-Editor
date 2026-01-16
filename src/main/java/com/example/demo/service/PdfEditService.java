package com.example.demo.service;

import com.example.demo.dto.TextEditRequest;
import com.example.demo.pdf.PositionTextStripper;
import com.example.demo.pdf.TextLocation;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PdfEditService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ✅ Single-word compatibility method
    public byte[] replaceText(MultipartFile file, String search, String replace) {
        try {
            PositionTextStripper stripper = new PositionTextStripper();
            stripper.getText(Loader.loadPDF(file.getInputStream().readAllBytes()));

            List<TextLocation> matches = stripper.getLocations().stream()
                    .filter(t -> t.text().equals(search))
                    .toList();

            List<TextEditRequest> edits = matches.stream()
                    .map(loc -> new TextEditRequest(loc.page(), loc.x(), loc.y(), replace, loc.height()))
                    .toList();

            return applyEdits(file, edits);
        } catch (Exception e) {
            throw new RuntimeException("PDF single-word replacement failed", e);
        }
    }

    // ✅ Apply multiple edits
    public byte[] applyEdits(MultipartFile file, List<TextEditRequest> edits) {
        try (PDDocument document = Loader.loadPDF(file.getInputStream().readAllBytes())) {

            for (TextEditRequest edit : edits) {
                var page = document.getPage(edit.page());

                try (PDPageContentStream cs = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true)) {

                    // Cover old text
                    cs.setNonStrokingColor(Color.WHITE);
                    cs.addRect(edit.x(), edit.y() - edit.fontSize(), edit.text().length() * edit.fontSize() * 0.6f, edit.fontSize() + 2);
                    cs.fill();

                    // Write new text
                    PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                    cs.beginText();
                    cs.setNonStrokingColor(Color.BLACK);
                    cs.setFont(font, edit.fontSize());
                    cs.newLineAtOffset(edit.x(), edit.y());
                    cs.showText(edit.text());
                    cs.endText();
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("PDF edit failed", e);
        }
    }

    // ✅ Parse JSON string from frontend into list of edits
    public List<TextEditRequest> parseEdits(String editsJson) {
        try {
            return objectMapper.readValue(
                    editsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, TextEditRequest.class)
            );
        } catch (Exception e) {
            throw new RuntimeException("Invalid edits JSON", e);
        }
    }
}
