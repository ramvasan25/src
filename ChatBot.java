import java.util.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;

public class ChatBot {
    private static final Scanner sc = new Scanner(System.in);
    private static final int Q_COUNT = 5;

    public static void main(String[] args) {
        System.out.println("Welcome to the Interview Practice ChatBot!");
        String cat = chooseCategory();
        runSession(cat);
    }

    private static void runSession(String cat) {
        List<String> qs = getQuestions(cat);
        List<String> ans = ask(qs);
        String criteria = getCriteria(cat);

        System.out.println("\n--- Evaluation Criteria ---\n" + criteria);
        System.out.println("\n--- Feedback ---");

        for (int i = 0; i < qs.size(); i++) {
            System.out.println("Q" + (i + 1) + ": " + qs.get(i));
            System.out.println("A: " + ans.get(i));
            System.out.println(getFeedback(criteria, qs.get(i), ans.get(i)));
            System.out.println();
        }
    }

    private static String chooseCategory() {
        System.out.println("Pick a category:\n1) HR\n2) Technical\n3) Interpersonal");
        while (true) {
            System.out.print("Choice: ");
            String in = sc.hasNextLine() ? sc.nextLine().trim() : "";
            switch (in) {
                case "1": case "HR": return "HR";
                case "2": case "Technical": return "Technical";
                case "3": case "Interpersonal": return "Interpersonal";
                default: System.out.println("Invalid. Try 1, 2, or 3.");
            }
        }
    }

    private static List<String> getQuestions(String cat) {
        switch (cat) {
            case "HR": return Arrays.asList(
                "Tell me about yourself.",
                "Why do you want this job?",
                "Strengths and weaknesses?",
                "Time you got criticism?",
                "Where in 5 years?");
            case "Technical": return Arrays.asList(
                "Challenging problem you solved?",
                "Describe a system you built.",
                "Debugging approach?",
                "Which data structures and why?",
                "How do you ensure code quality?");
            default: return Arrays.asList(
                "Conflict with coworker?",
                "Leading a team under pressure?",
                "Persuading someone?",
                "Handling feedback you disagree with?",
                "Helping a colleague improve?");
        }
    }

    private static List<String> ask(List<String> qs) {
        List<String> ans = new ArrayList<>();
        for (int i = 0; i < Q_COUNT; i++) {
            System.out.println("\nQ" + (i + 1) + ": " + qs.get(i));
            System.out.print("Your answer: ");
            String a = sc.hasNextLine() ? sc.nextLine().trim() : "";
            ans.add(a.isEmpty() ? "(blank)" : a);
        }
        return ans;
    }

    private static String getCriteria(String cat) {
        String base = "- Clear, structured STAR answers\n- Relevant, specific, professional\n";
        switch (cat) {
            case "Technical": return base + "- Technical depth & trade-offs\n- Testing & architecture\n";
            case "HR": return base + "- Motivation & cultural fit\n";
            default: return base + "- Collaboration & emotional intelligence\n";
        }
    }

    private static String getFeedback(String criteria, String q, String a) {
        String key = System.getenv("OPENAI_API_KEY");
        if (a.equals("(blank)")) return "You left this blank. Try STAR format.";
        if (key != null && !key.isBlank()) {
            try {
                return callOpenAIForFeedback(key, buildPrompt(criteria, q, a));
            } catch (Exception e) {
                return "(LLM failed: " + e.getMessage() + ")\n" + localFeedback(a);
            }
        }
        return localFeedback(a);
    }

    private static String buildPrompt(String crit, String q, String a) {
        return "You are an interview coach. Evaluate answer per criteria.\n"
            + "CRITIQUE (1-3 sentences)\nSUGGESTIONS (bullets)\nTIPS (bullets)\n"
            + "Criteria:\n" + crit + "\nQ: " + q + "\nA: " + a;
    }

    // === Keep this method unchanged ===
    private static String callOpenAIForFeedback(String apiKey, String prompt) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String model = "gpt-3.5-turbo";
        double temperature = 0.7;
        int maxTokens = 200;
        double topP = 1.0;
        double frequencyPenalty = 0.0;
        double presencePenalty = 0.6;
        String stopSeq = "\"Q:\"";

        String jsonBody = "{"
            + "\"model\":\"" + model + "\","
            + "\"messages\":[{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}],"
            + "\"temperature\":" + temperature + ","
            + "\"max_tokens\":" + maxTokens + ","
            + "\"top_p\":" + topP + ","
            + "\"frequency_penalty\":" + frequencyPenalty + ","
            + "\"presence_penalty\":" + presencePenalty + ","
            + "\"stop\":[" + stopSeq + "]"
            + "}";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();

        String assistantText = extractAssistantContent(body);
        if (assistantText == null || assistantText.isBlank()) {
            throw new IOException("No assistant content found; status=" + response.statusCode());
        }
        return assistantText.trim();
    }

    private static String extractAssistantContent(String json) {
        String key = "\"content\":\"";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int start = idx + key.length(), i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) break;
            i++;
        }
        String raw = json.substring(start, i);
        return raw.replaceAll("\\\\n", "\n").replaceAll("\\\\\"", "\"");
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static String localFeedback(String a) {
        return "CRITIQUE: " + (a.length() < 40 ? "Too brief." : "Good, but could be sharper.") + "\n"
            + "SUGGESTIONS:\n- Use STAR\n- Quantify results\nTIPS:\n- Keep under 2 minutes";
    }
}
