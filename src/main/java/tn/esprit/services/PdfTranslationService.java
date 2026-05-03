package tn.esprit.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PdfTranslationService {

    private final GroqAiService groqAiService = new GroqAiService();

    public String extractText(File pdfFile) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    public File translateAndSave(String originalText, String targetLang, String outputPath) throws IOException, InterruptedException {
        String translatedText = callGroqForTranslation(originalText, targetLang);

        Path targetPath = Paths.get(outputPath).toAbsolutePath();
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDFont font = resolveFont(document);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.setLeading(12.0f);
                contentStream.newLineAtOffset(50, 750);

                String[] lines = translatedText.split("\\r?\\n");
                for (String line : lines) {
                    while (line.length() > 90) {
                        contentStream.showText(renderableText(font, line.substring(0, 90)));
                        contentStream.newLine();
                        line = line.substring(90);
                    }
                    contentStream.showText(renderableText(font, line));
                    contentStream.newLine();
                }
                contentStream.endText();
            }
            document.save(targetPath.toFile());
        }

        return targetPath.toFile();
    }

    private String callGroqForTranslation(String text, String targetLang) throws IOException, InterruptedException {
        String truncated = text.length() > 1500 ? text.substring(0, 1500) : text;
        String translated = groqAiService.ask(
                "You are a professional translator. Translate the given text into " + targetLang + ". Return only the translated text.",
                truncated,
                2000);

        if (translated != null && translated.startsWith("### ")) {
            throw new IOException(translated);
        }
        if (translated == null || translated.isBlank()) {
            throw new IOException("Groq returned an empty translation.");
        }
        return translated;
    }

    private String sanitize(String text) {
        return text.replaceAll("[^\\x00-\\x7F]", " ");
    }

    private PDFont resolveFont(PDDocument document) throws IOException {
        File[] candidates = new File[] {
                new File("C:/Windows/Fonts/arial.ttf"),
                new File("C:/Windows/Fonts/tahoma.ttf"),
                new File("C:/Windows/Fonts/segoeui.ttf")
        };

        for (File candidate : candidates) {
            if (candidate.exists()) {
                return PDType0Font.load(document, candidate);
            }
        }

        return PDType1Font.HELVETICA;
    }

    private String renderableText(PDFont font, String text) {
        if (font instanceof PDType1Font) {
            return sanitize(text);
        }
        return shapeArabicIfNeeded(text);
    }

    private String shapeArabicIfNeeded(String text) {
        if (text == null || text.isBlank() || !containsArabic(text)) {
            return text;
        }

        try {
            ArabicShaping shaper = new ArabicShaping(
                    ArabicShaping.LETTERS_SHAPE | ArabicShaping.TEXT_DIRECTION_LOGICAL);
            String shaped = shaper.shape(text);
            return toVisualOrder(shaped);
        } catch (ArabicShapingException e) {
            return toVisualOrder(text);
        }
    }

    private String toVisualOrder(String logicalText) {
        // PDFBox writes text left-to-right; for Arabic we need the *visual* (display) order.
        // ICU's BiDi algorithm handles mixed Arabic/Latin text much better than reversing.
        // Use right-to-left paragraph base for Arabic so words are ordered visually.
        Bidi bidi = new Bidi(logicalText, Bidi.DIRECTION_RIGHT_TO_LEFT);
        return bidi.writeReordered(Bidi.DO_MIRRORING);
    }

    private boolean containsArabic(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= '\u0600' && c <= '\u06FF') ||
                    (c >= '\u0750' && c <= '\u077F') ||
                    (c >= '\u08A0' && c <= '\u08FF') ||
                    (c >= '\uFB50' && c <= '\uFDFF') ||
                    (c >= '\uFE70' && c <= '\uFEFF')) {
                return true;
            }
        }
        return false;
    }
}
