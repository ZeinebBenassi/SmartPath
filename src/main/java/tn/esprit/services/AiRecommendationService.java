package tn.esprit.services;

import tn.esprit.entity.Grade;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AiRecommendationService {

    private final GroqAiService groqAiService = new GroqAiService();

    public String getRecommendations(List<Grade> grades) throws IOException, InterruptedException {
        if (grades == null || grades.isEmpty()) {
            return "No quiz results were found. Complete a quiz first so the AI can analyze your performance.";
        }

        List<Grade> orderedGrades = grades.stream()
                .sorted(Comparator.comparingDouble(Grade::getScore))
                .collect(Collectors.toList());

        List<Grade> focusGrades = orderedGrades.size() > 5
                ? orderedGrades.subList(0, 5)
                : orderedGrades;

        double average = grades.stream().mapToDouble(Grade::getScore).average().orElse(0.0);

        StringBuilder prompt = new StringBuilder("I am a student reviewing my latest quiz results.\n");
        prompt.append("Average score: ")
                .append(String.format(Locale.US, "%.2f", average))
                .append("\n");
        prompt.append("Quiz result entries:\n");
        for (Grade grade : grades) {
            prompt.append("- ")
                    .append(grade.getSubject())
                    .append(": ")
                    .append(grade.getScore())
                    .append("\n");
        }

        prompt.append("\nFocus your advice on the weakest entries: ");
        prompt.append(focusGrades.stream()
                .map(g -> g.getSubject() + " (" + g.getScore() + ")")
                .collect(Collectors.joining(", ")));
        prompt.append(".\nProvide:\n");
        prompt.append("1. A short summary of the student's performance.\n");
        prompt.append("2. Practical study advice for the weakest areas.\n");
        prompt.append("3. A concise weekly improvement plan.\n");
        prompt.append("4. A section called 'YouTube Resources' with 3-5 search links or exact search queries relevant to the weakest areas.\n");
        prompt.append("5. A section called 'Udemy Courses' with 3-5 course keywords/titles to search.\n");
        prompt.append("6. A section called 'Coursera Courses' with 3-5 course keywords/titles to search.\n");
        prompt.append("Return only the recommendation text in Markdown.");

        String recommendation = groqAiService.ask(
                "You are an expert academic advisor who writes concise and actionable feedback.",
                prompt.toString(),
                900);

        saveToFile(recommendation);
        return recommendation;
    }

    private void saveToFile(String content) throws IOException {
        String fileName = "recommendations_" + System.currentTimeMillis() + ".txt";
        String filePath = Paths.get(System.getProperty("user.home"), fileName).toString();
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }
}
