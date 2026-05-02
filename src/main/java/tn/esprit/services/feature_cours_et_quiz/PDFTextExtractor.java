package tn.esprit.services.feature_cours_et_quiz;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;

public class PDFTextExtractor {

    public static String extractText(String filePath) throws IOException {
        if (filePath == null || filePath.isBlank()) return "";
        
        // Remove "__PDF__:" prefix if present (used in internal storage)
        if (filePath.startsWith("__PDF__: ")) {
            filePath = filePath.substring(9);
        } else if (filePath.startsWith("__PDF__:")) {
            filePath = filePath.substring(8);
        }

        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Fichier PDF introuvable : " + filePath);
        }

        try (PDDocument document = PDDocument.load(file)) {
            if (document.isEncrypted()) {
                return "Le document est chiffré et ne peut pas être lu par l'IA.";
            }
            PDFTextStripper stripper = new PDFTextStripper();
            // Limit to first 5 pages to avoid context window issues or long processing
            stripper.setEndPage(Math.min(5, document.getNumberOfPages()));
            return stripper.getText(document);
        }
    }
}
