import java.util.*;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/*
 * ChatBot
 * - Simple CLI interview practice tool.
 * - Prompts user to choose a category, asks questions, collects answers,
 *   and provides feedback (LLM if API key present, otherwise heuristic).
 */
public class ChatBot {
    // === I/O & Constants ===
    private static final Scanner scanner = new Scanner(System.in);
    private static final int QUESTIONS_PER_SESSION = 5;

    public static void main(String[] args) {
        System.out.println("Welcome to the Interview Practice ChatBot!");

        // Always prompt the user for the interview category (do not read from args)
        String category = chooseCategory();
        System.out.println("You selected: " + category);

        // Start one practice session for the selected category
        runSession(category);
    }

    // Run a single practice session end-to-end for a category
    private static void runSession(String category) {
        // --- Prepare questions and collect answers ---
        List<String> questions = getQuestionsForCategory(category);
        List<String> answers = askQuestions(questions);

        // --- Generate evaluation criteria and feedback ---
        String criteria = generateCriteria(category, questions);
        List<String> feedback = generateFeedback(criteria, questions, answers);

        System.out.println("\n--- Evaluation Criteria ---");
        System.out.println(criteria);

        System.out.println("\n--- Feedback On Your Answers ---");
        for (int i = 0; i < questions.size(); i++) {
            System.out.println("Q" + (i + 1) + ": " + questions.get(i));
            System.out.println("A: " + answers.get(i));
            System.out.println(feedback.get(i));
            System.out.println();
        }

        System.out.println("Practice session complete. You can re-run the program to try a different category.");
    }

    private static String chooseCategory() {
        // Prompt the user to select one of three categories
        System.out.println("Which interview would you like to prepare for? (enter number)");
        System.out.println("1) HR");
        System.out.println("2) Technical");
        System.out.println("3) Interpersonal (Behavioral)");

        while (true) {
            System.out.print("Choice: ");
            String input;
            if (scanner.hasNextLine()) {
                input = scanner.nextLine().trim();
            } else {
                System.out.println();
                System.out.println("No input detected — defaulting to 'Technical'.");
                return "Technical";
            }
            switch (input) {
                case "1":
                case "HR":
                case "hr":
                    return "HR";
                case "2":
                case "Technical":
                case "technical":
                    return "Technical";
                case "3":
                case "Interpersonal":
                case "interpersonal":
                case "Behavioral":
                case "behavioral":
                    return "Interpersonal";
                default:
                    System.out.println("Invalid choice. Please enter 1, 2, or 3.");
            }
        }
    }

    private static List<String> getQuestionsForCategory(String category) {
        // Return a small fixed list of 5 questions depending on the category
        List<String> qs = new ArrayList<>();
        switch (category) {
            case "HR":
                qs.add("Tell me about yourself and your background.");
                qs.add("Why do you want to work for our company?");
                qs.add("What are your strengths and weaknesses?");
                qs.add("Tell us about a time you received constructive criticism.");
                qs.add("Where do you see yourself in 5 years?");
                break;
            case "Technical":
                qs.add("Explain a challenging technical problem you solved.");
                qs.add("Describe the architecture of a project you built.");
                qs.add("How do you approach debugging a complex issue?");
                qs.add("Which data structures would you use for X (explain why)?");
                qs.add("How do you ensure code quality and testing?");
                break;
            case "Interpersonal":
                qs.add("Describe a conflict you had with a coworker and how you resolved it.");
                qs.add("Tell me about a time you led a team under pressure.");
                qs.add("Give an example of when you had to persuade someone to accept your idea.");
                qs.add("How do you handle feedback that you disagree with?");
                qs.add("Tell me about a time you helped a colleague improve.");
                break;
            default:
                qs.add("Tell me about yourself.");
                qs.add("Why this company?");
                qs.add("Strengths and weaknesses?");
                qs.add("Describe a past challenge.");
                qs.add("Where in 5 years?");
        }
        return qs;
    }

    private static List<String> askQuestions(List<String> questions) {
        // Ask each question via console and collect user answers
        List<String> answers = new ArrayList<>();
        System.out.println("\nI will ask " + QUESTIONS_PER_SESSION + " questions. Please answer each one (press Enter when done).\n");
        for (int i = 0; i < questions.size(); i++) {
            System.out.println();
            System.out.println("Question " + (i + 1) + ": " + questions.get(i));
            System.out.print("Your answer: ");
            String ans;
            if (scanner.hasNextLine()) {
                ans = scanner.nextLine().trim();
            } else {
                System.out.println();
                ans = "";
            }
            if (ans.isEmpty()) {
                System.out.println("(You submitted an empty answer; you can enter a short response next time.)");
            }
            answers.add(ans);
        }
        return answers;
    }

    private static String generateCriteria(String category, List<String> questions) {
        // Build a human-readable set of evaluation criteria for the session
        StringBuilder sb = new StringBuilder();
        sb.append("Category: ").append(category).append("\n");
        sb.append("General evaluation criteria:\n");
        sb.append("- Clarity and structure of response (use of STAR: Situation, Task, Action, Result).\n");
        sb.append("- Relevance to the question and role.\n");
        sb.append("- Specific examples and measurable outcomes.\n");
        sb.append("- Professional tone and confidence.\n");
        if (category.equals("Technical")) {
            sb.append("- Technical depth and correctness; mention trade-offs.\n");
            sb.append("- Understanding of architecture, algorithms, and testing.\n");
        } else if (category.equals("HR")) {
            sb.append("- Cultural fit and motivation; alignment with company values.\n");
        } else if (category.equals("Interpersonal")) {
            sb.append("- Emotional intelligence, collaboration, and learning.\n");
        }
        return sb.toString();
    }

    private static List<String> generateFeedback(String criteria, List<String> questions, List<String> answers) {
        // Generate feedback for each answer. If an OpenAI API key is provided
        // via the OPENAI_API_KEY environment variable, attempt an LLM call.
        List<String> feedback = new ArrayList<>();
        String apiKey = System.getenv("OPENAI_API_KEY");
        boolean hasApiKey = apiKey != null && !apiKey.isBlank();

        for (int i = 0; i < questions.size(); i++) {
            String ans = answers.get(i);
            if (ans == null || ans.trim().isEmpty()) {
                feedback.add("You left this answer blank. Try to give a short structured response using an example.");
                continue;
            }

            if (hasApiKey) {
                String prompt = buildPrompt(criteria, questions.get(i), ans);
                try {
                    String modelFeedback = callOpenAIForFeedback(apiKey, prompt);
                    feedback.add(modelFeedback);
                    continue;
                } catch (Exception e) {
                    feedback.add("(LLM call failed: " + e.getMessage() + ") " + localHeuristicFeedback(questions.get(i), ans));
                    continue;
                }
            }

            feedback.add(localHeuristicFeedback(questions.get(i), ans));
        }
        return feedback;
    }

    private static String buildPrompt(String criteria, String question, String answer) {
        // Build a concise instruction prompt to send to an LLM.
        // The prompt requests specific labeled sections to make parsing deterministic.
        StringBuilder p = new StringBuilder();
        p.append("You are an expert interview coach. Evaluate the candidate's answer below according to the provided criteria. Return only the following labeled plain-text sections (NO JSON):\n");
        p.append("- CRITIQUE: 1-3 short sentences about strengths and weaknesses.\n");
        p.append("- SUGGESTIONS: 2-4 specific suggestions each starting with '- '.\n");
        p.append("- TIPS: 1-3 short tactical tips in bullets.\n");
        p.append("CRITIQUE:, SUGGESTIONS:, TIPS:.\n");
        // p.append("Only include those labeled sections; avoid extra preamble.\n");
        p.append("Criteria:\n");
        p.append(criteria).append("\n");
        p.append("Question: ").append(question).append("\n");
        p.append("Candidate Answer: ").append(answer).append("\n");
        return p.toString();
    }

    private static String callOpenAIForFeedback(String apiKey, String prompt) throws Exception {
        // Make a minimal Chat Completions API call and return the assistant's text.
        HttpClient client = HttpClient.newHttpClient();

        String jsonBody = "{\"model\":\"gpt-3.5-turbo\",\"messages\":[{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}],\"max_tokens\":350}";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();

        // Extract the assistant's reply from the returned JSON (simple parser below)
        String assistantText = extractAssistantContent(body);
        if (assistantText == null || assistantText.isBlank()) {
            throw new IOException("No assistant content found in response; status=" + response.statusCode());
        }
        return assistantText.trim();
    }

    private static String extractAssistantContent(String json) {
        // Lightweight extraction of the first "content":"..." value.
        // Note: this is not a full JSON parser but works for the expected API shape.
        if (json == null) return null;
        String key = "\"content\":\"";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int start = idx + key.length();
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '"') {
                // count preceding backslashes
                int back = 0; int j = i - 1;
                while (j >= start && json.charAt(j) == '\\') { back++; j--; }
                if (back % 2 == 0) break; // not escaped
            }
            i++;
        }
        if (i >= json.length()) return null;
        String raw = json.substring(start, i);
        // Unescape common sequences for readability
        String unescaped = raw.replaceAll("\\\\n", "\n").replaceAll("\\\\r", "\r").replaceAll("\\\"", "\"").replaceAll("\\\\\\\\", "\\");
        return unescaped;
    }

    private static String escapeJson(String s) {
        // Escape backslashes, quotes and newlines for embedding in JSON
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static String localHeuristicFeedback(String question, String ans) {
        // Simple, local heuristic feedback for when LLM is unavailable
        if (ans == null || ans.trim().isEmpty()) return "You left this blank. Try to provide a concise STAR example next time.";
        StringBuilder sb = new StringBuilder();
        sb.append("CRITIQUE:\n");
        sb.append("- ");
        sb.append(ans.length() < 40 ? "Answer is brief and lacks specifics." : "Answer contains some specifics but could be more outcome-focused.");
        sb.append("\n");
        sb.append("SUGGESTIONS:\n");
        sb.append("- Use STAR: clearly state Situation and Task early.\n");
        sb.append("- Quantify results when possible (numbers, % improvements).\n");
        sb.append("TIPS:\n");
        sb.append("- Keep it under 2 minutes when speaking.\n");
        // Note: numeric SCORES and CONFIDENCE fields were removed per user request
        return sb.toString();
    }
}
