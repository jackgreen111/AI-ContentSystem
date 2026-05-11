package kz.ai.content.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class DocumentReaderService {

    private static final Logger log = LoggerFactory.getLogger(DocumentReaderService.class);

    public String extractText(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase() : "";
        if (name.endsWith(".pdf"))  return extractPdf(file);
        if (name.endsWith(".docx")) return extractDocx(file);
        if (name.endsWith(".txt"))  return new String(file.getBytes());
        throw new IllegalArgumentException("Поддерживаются только PDF, DOCX и TXT файлы");
    }

    private String extractPdf(MultipartFile file) throws IOException {
        // PDFBox 3.x — используем Loader.loadPDF() вместо PDDocument.load()
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            log.info("PDF прочитан: {} страниц, {} символов",
                    doc.getNumberOfPages(), text.length());
            return text;
        }
    }

    private String extractDocx(MultipartFile file) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            String text = extractor.getText();
            log.info("DOCX прочитан: {} символов", text.length());
            return text;
        }
    }
}
