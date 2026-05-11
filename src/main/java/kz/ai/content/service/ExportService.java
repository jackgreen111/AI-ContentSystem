package kz.ai.content.service;
import kz.ai.content.model.ContentGeneration;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
@Service
public class ExportService {
    private static final DateTimeFormatter FMT=DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    public byte[] toTxt(ContentGeneration g){
        String t="AI Content System — Результат\n"+"=".repeat(50)+"\nТема: "+g.getTopic()+"\nТип: "+g.getContentType().getDisplayName()+"\nДата: "+g.getCreatedAt().format(FMT)+"\n"+"-".repeat(50)+"\n\n"+g.getGeneratedText();
        return t.getBytes(StandardCharsets.UTF_8);
    }
    public byte[] toDocx(ContentGeneration g) throws IOException {
        try(XWPFDocument doc=new XWPFDocument();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            XWPFParagraph title=doc.createParagraph(); title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun tr=title.createRun(); tr.setBold(true); tr.setFontSize(16); tr.setText("AI Content System");tr.addBreak();
            addMeta(doc,"Тема:",g.getTopic()); addMeta(doc,"Тип:",g.getContentType().getDisplayName()); addMeta(doc,"Дата:",g.getCreatedAt().format(FMT));
            doc.createParagraph().createRun().setText("─".repeat(60));
            XWPFRun run=doc.createParagraph().createRun();
            String[] lines=g.getGeneratedText().split("\n");
            for(int i=0;i<lines.length;i++){run.setText(lines[i]);if(i<lines.length-1)run.addBreak();}
            doc.write(out); return out.toByteArray();
        }
    }
    public byte[] toPdf(ContentGeneration g) throws IOException {
        try(ByteArrayOutputStream out=new ByteArrayOutputStream()){
            var w=new com.itextpdf.kernel.pdf.PdfWriter(out);
            var pdf=new com.itextpdf.kernel.pdf.PdfDocument(w);
            var doc=new com.itextpdf.layout.Document(pdf);
            doc.add(new com.itextpdf.layout.element.Paragraph("AI Content System").setBold().setFontSize(18).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
            doc.add(new com.itextpdf.layout.element.Paragraph("Тема: "+g.getTopic()).setBold());
            doc.add(new com.itextpdf.layout.element.Paragraph("Дата: "+g.getCreatedAt().format(FMT)));
            doc.add(new com.itextpdf.layout.element.Paragraph(" "));
            doc.add(new com.itextpdf.layout.element.Paragraph(g.getGeneratedText()));
            doc.close(); return out.toByteArray();
        }
    }
    private void addMeta(XWPFDocument doc,String label,String value){XWPFParagraph p=doc.createParagraph();XWPFRun lr=p.createRun();lr.setBold(true);lr.setText(label+" ");p.createRun().setText(value);}
}
