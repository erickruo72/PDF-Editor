package com.example.demo.service;

import com.example.demo.pdf.PositionTextStripper;
import com.example.demo.pdf.TextLocation;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class PdfService {

    // Extract plain text from PDF
    public String extractText(MultipartFile file) {
        if (file.isEmpty()) throw new RuntimeException("File is empty");

        try (PDDocument document = Loader.loadPDF(file.getInputStream().readAllBytes())) {
            if (document.isEncrypted()) throw new RuntimeException("Encrypted PDFs are not supported");

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.trim().isEmpty())
                throw new RuntimeException("PDF contains no searchable text");

            return text;
        } catch (Exception e) {
            throw new RuntimeException("Invalid or unreadable PDF", e);
        }
    }

    // Extract word positions for frontend overlay editing
    public List<TextLocation> extractWordLocations(MultipartFile file) {
        if (file.isEmpty()) throw new RuntimeException("File is empty");

        try (PDDocument document = Loader.loadPDF(file.getInputStream().readAllBytes())) {
            if (document.isEncrypted()) throw new RuntimeException("Encrypted PDFs are not supported");

            PositionTextStripper stripper = new PositionTextStripper();
            stripper.getText(document);

            return stripper.getLocations().stream()
                    .filter(loc -> loc.text().trim().length() > 0)
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("Failed to extract word positions", e);
        }
    }
}
