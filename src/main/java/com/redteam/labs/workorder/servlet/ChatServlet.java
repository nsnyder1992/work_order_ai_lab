package com.redteam.labs.workorder.servlet;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import com.redteam.labs.workorder.dao.WorkOrderDAO;
import com.redteam.labs.workorder.model.User;
import com.redteam.labs.workorder.util.AiTokenQuotaManager;

@WebServlet("/jsp/chat")
public class ChatServlet extends HttpServlet
{
    private static final long serialVersionUID = -1678313650906063929L;

    private static final AiTokenQuotaManager tokenQuotaManager = new AiTokenQuotaManager();
    
    private static Properties appProperties = new Properties();
    
    @Override
    public void init() throws ServletException
    {
        super.init();
        
        try(FileInputStream fis = new FileInputStream(getServletContext().getRealPath("/WEB-INF/classes/app.properties"))) {
            if (fis.available() == 0) {
                throw new ServletException("context.txt file is empty or not found.");
            }
            
            appProperties.load(fis);
            
            String keystorePath = "/WEB-INF/classes/keystore.p12";
            String keystorePassword = appProperties.getProperty("keystore.password", "changeit");
            
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis2 = new FileInputStream(getServletContext().getRealPath(keystorePath))) {
                keystore.load(fis2, keystorePassword.toCharArray());
            } 
            
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keystore, keystorePassword.toCharArray()); // password for private key
            
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null); // Use default system CA certs
            
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            
        } catch (Exception e) {
            throw new ServletException("Error reading intialization files.", e);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {

        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
        
        if (user == null)
        {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not authenticated.");
            return;
        }
        
        String question = req.getParameter("question");
        question = replaceSpecialCharacters(question).trim();
        if (!question.matches(ALLOWED_CHARACTERS)) {
            throw new IllegalArgumentException("Invalid input provided.");
        }
        
        if (question == null || question.isEmpty())
        {
            resp.getWriter().write("No question provided.");
            return;
        }

        
        @SuppressWarnings("unchecked")
        List<String> chatHistory = (List<String>) session.getAttribute("chatHistory");
        if (chatHistory == null)
        {
            chatHistory = new ArrayList<>();
        }

        StringBuilder systemPromptBuilder = new StringBuilder();
        try (InputStream inputStream = req.getServletContext().getResourceAsStream("/WEB-INF/classes/context.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream)))
        {
            if (inputStream == null)
            {
                throw new IOException("context.json file not found in resources.");
            }
            String line;
            while ((line = reader.readLine()) != null)
            {
                systemPromptBuilder.append(line);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        if (systemPromptBuilder.length() == 0)
        {
            resp.getWriter().write("Error reading context.txt.");
            return;
        }
        
        String systemPrompt = systemPromptBuilder.toString();
        String systemWorkOrders = WorkOrderDAO.getWorkOrdersAsPromptText(user.getId());

        String userWorkorders = WorkOrderDAO.getWorkOrdersByUserIdAsPromptText(user.getId());
        
        String chatHistoryString = chatHistory.stream().map(entry -> entry.toString()).collect(Collectors.joining("\n"));

        systemPrompt = systemPrompt.replace("{{sys_work_orders}}", systemWorkOrders).replace("{{user_work_orders}}",
                userWorkorders).replace("{{name}}", user.getUsername()).replace("{{user_id}}", "" + user.getId()).replace(
                        "{{role}}", user.getRole()).replace("{{user_prompt}}", question).replace("{{remaining_tokens}}",
                                String.valueOf(tokenQuotaManager.getRemainingQuota(user.getUsername())));

        String response = "";

        int promptTokens = estimateTokens(question.toString()); // You can estimate this roughly or use a tokenizer
        if (!tokenQuotaManager.canUseTokens(user.getUsername(), promptTokens))
            response = "You've exceeded your AI usage quota for today.";
        else if (estimateTokens(systemPrompt) > 2000)
            response = "Your prompt is too long. Please shorten it to less than 2000 tokens.";
        else if (question.length() > 1000)
            response = "Your question is too long. Please limit it to 1000 characters.";
        else
        {
            systemPrompt.replace("{{chat_history}}", chatHistoryString);
            if (appProperties.getProperty("ollama.chat.enabled", "true").equalsIgnoreCase("false"))
            {
                response = "Chat functionality is currently disabled.";
            }
            else if (appProperties.getProperty("ollama.chat.mode", "default").equalsIgnoreCase("default"))
            {
                // If workorder mode is enabled, we can use the chat history and system prompt
                response = chat(systemPrompt);
            }
            else if (appProperties.getProperty("ollama.chat.mode", "default").equalsIgnoreCase("http"))
            {
                HttpURLConnection connection = null;
                try
                {
                    String chatUrl = appProperties.getProperty("ollama.chat.url", "http://localhost:11434/chat");
                    if (chatUrl == null || chatUrl.isEmpty())
                    {
                        response = "Chat URL is not configured.";
                        return;
                    }
                    
                    if (chatUrl.startsWith("https://")) {
                        HttpsURLConnection httpsConnection = (HttpsURLConnection) new URL(chatUrl).openConnection();                        
                        connection = httpsConnection;
                    } else {
                        connection = (HttpURLConnection) new URL(chatUrl).openConnection();

                    }
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Authorization", "Bearer " + System.getenv("AGENT_AUTH_TOKEN"));
                    connection.setRequestProperty("Content-Type", "application/json; utf-8");
                    connection.setRequestProperty("Accept", "application/json");

//                    JSONObject json = new JSONObject();
//                    json.put("user_id", user.getId());
//                    json.put("role", user.getRole());
//                    json.put("name", user.getUsername());
//                    json.put("remaining_tokens", tokenQuotaManager.getRemainingQuota(user.getUsername()));
//                    json.put("user_work_orders", userWorkorders);
//                    json.put("system_work_orders", systemWorkOrders);
//                    json.put("prompt", question);
//
//                    String jsonInputString = json.toString();
//                    
//                    try (OutputStream os = connection.getOutputStream();
//                            OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
//                           writer.write(jsonInputString);
//                           writer.flush();
//                       }
//
//                    
//                    try(InputStream inputStream = connection.getInputStream();
//                            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream)))
//                    {
//                        StringBuilder responseBuilder = new StringBuilder();
//                        String line;
//                        while ((line = reader.readLine()) != null)
//                        {
//                            responseBuilder.append(line);
//                        }
//                        response = replaceEscapedCharacters(responseBuilder.toString().trim());
//                    }
                }
                catch (Exception e)
                {
                    response = "Error communicating with chat service: " + e.getMessage();
                    
                    if (connection != null) {
                        int responseCode = connection.getResponseCode();
                        if (responseCode >= 400) {
                            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                                String errorLine;
                                StringBuilder errorResponse = new StringBuilder();
                                while ((errorLine = errorReader.readLine()) != null) {
                                    errorResponse.append(errorLine);
                                }
                                response += " - Error details: " + errorResponse.toString();
                            } catch (IOException ioException) {
                                response += " - Unable to read error details.";
                            }
                        }
                    }
                }
            }
            else 
            {
                response = chat(systemPrompt);
            }

        }

        tokenQuotaManager.recordUsage(user.getUsername(), promptTokens);

        StringBuilder chatEntryBuilder = new StringBuilder();
        chatEntryBuilder.append("- User: ").append(question).append(" | Assitant: ").append(response).append(
                " | Timestamp: ").append(System.currentTimeMillis());

        chatHistory.add(chatEntryBuilder.toString());
        session.setAttribute("chatHistory", chatHistory);

        resp.setContentType("text/plain");
        resp.getWriter().write(response);
    }

    public static int estimateTokens(String text)
    {
        return text.length() / 4; // Approximation
    }

    private static final String ALLOWED_CHARACTERS = "^[a-zA-Z0-9 \\[\\]\\_]+$";
    
    public static String replaceSpecialCharacters(String input) {
        return input.replaceAll("\\[", "[open_bracket]")
                    .replaceAll("\\]", "[close_bracket]")
                    .replaceAll("!", "[exclamation]")
                    .replaceAll("\\?", "[question]")
                    .replaceAll("\\.", "[dot]")
                    .replaceAll(",", "[comma]")
                    .replaceAll("'", "[apostrophe]")
                    .replaceAll("\"", "[quote]")
                    .replaceAll(";", "[semicolon]")
                    .replaceAll(":", "[colon]")
                    .replaceAll("\\|", "[pipe]")
                    .replaceAll("&", "[ampersand]")
                    .replaceAll("<", "[less_than]")
                    .replaceAll(">", "[greater_than]")
                    .replaceAll("\\(", "[open_parenthesis]")
                    .replaceAll("\\)", "[close_parenthesis]")
                    .replaceAll("\\{", "[open_curly_brace]")
                    .replaceAll("\\}", "[close_curly_brace]")
                    .replaceAll("\\$", "[dollar]")
                    .replaceAll("\\*", "[asterisk]")
                    .replaceAll("\\^", "[caret]")
                    .replaceAll("~", "[tilde]")
                    .replaceAll("`", "[backtick]")
                    .replaceAll("\\\\", "[backslash]")
                    .replaceAll("/", "[forward_slash]")
                    .replaceAll("[^a-zA-Z0-9\\[\\] \\\\_]", "[unknown_character]"); // Replace any other special characters with a placeholder
    }
    
    public static String replaceEscapedCharacters(String input) {
        return input.replaceAll("\\[open_bracket\\]", "[")
                .replaceAll("\\[close_bracket\\]", "]")
                .replaceAll("\\[exclamation\\]", "!")
                .replaceAll("\\[question\\]", "?")
                .replaceAll("\\[dot\\]", ".")
                .replaceAll("\\[comma\\]", ",")
                .replaceAll("\\[apostrophe\\]", "'")
                .replaceAll("\\[quote\\]", "\"")
                .replaceAll("\\[semicolon\\]", ";")
                .replaceAll("\\[colon\\]", ":")
                .replaceAll("\\[pipe\\]", "|")
                .replaceAll("\\[ampersand\\]", "&")
                .replaceAll("\\[less_than\\]", "<")
                .replaceAll("\\[greater_than\\]", ">")
                .replaceAll("\\[open_parenthesis\\]", "(")
                .replaceAll("\\[close_parenthesis\\]", ")")
                .replaceAll("\\[open_curly_brace\\]", "{")
                .replaceAll("\\[close_curly_brace\\]", "}")
                .replaceAll("\\[dollar\\]", "\\$")
                .replaceAll("\\[asterisk\\]", "\\*")
                .replaceAll("\\[caret\\]", "\\^")
                .replaceAll("\\[tilde\\]", "~")
                .replaceAll("\\[backtick\\]", "`")
                .replaceAll("\\[backslash\\]", "\\\\")
                .replaceAll("\\[forward_slash\\]", "/")
                .replaceAll("\\[unknown_character\\]", " "); // Reverse placeholder replacement
    }
    
    static String chat(String prompt) throws IOException
    {        
        // Assuming ollama is installed and available in the system PATH
        ProcessBuilder pb = new ProcessBuilder("ollama", "run", "mistral", "--hidethinking");
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Write input to Ollama (prompt)
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream())))
        {
            writer.write("'"+ prompt.replace("'", "\'") + "'\n"); // Must end with newline to trigger processing
            writer.flush();
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null)
        {
            response.append(line).append("\n");
        }

        line = stripAnsi(response.toString()).trim();
        return replaceEscapedCharacters(line);
    }

    public static String stripAnsi(String input)
    {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // Step 1: Remove all ANSI escape sequences
        String result = input.replaceAll("\\u001B\\[[0-9;]*[a-zA-Z]", "") // Standard ANSI sequences
                             .replaceAll("\\u001B\\][^\\u0007]*\\u0007", "") // OSC sequences
                             .replaceAll("\\u001B\\[\\?[0-9]+[hl]", "") // VT100 sequences
                             .replaceAll("\\u001B\\[[0-9;]*[@-~]", "") // All CSI sequences
                             .replaceAll("\\e\\[[0-9;]*[a-zA-Z]", ""); // Alternative escape notation
        
        // Step 2: Remove all Unicode Braille pattern characters (spinners)
        // This covers the entire Braille Patterns Unicode block (U+2800-U+28FF)
        result = result.replaceAll("[\\u2800-\\u28FF]", "");
        
        // Step 3: Remove Unicode replacement characters and other problematic chars
        result = result.replaceAll("[\\uFFFD\\uFEFF]", "") // Replacement char and BOM
                      .replaceAll("[\\u0000-\\u001F\\u007F-\\u009F]", "") // All control chars
                      .replaceAll("\\r+", "") // Carriage returns
                      .replaceAll("[\u200B-\u200D\uFEFF]", ""); // Zero-width chars and BOM
        
        // Step 4: Remove common malformed UTF-8 sequences and spinner-like characters
        result = result.replaceAll("â\\s*[™¹¸¼´¦§‡Ï]", "") // Common UTF-8 encoding issues
                      .replaceAll("[™¹¸¼´¦§‡Ï]", "") // Direct character removal
                      .replaceAll("â\\s+", "") // Remove â followed by spaces
                      .replaceAll("â", ""); // Remove any remaining â characters
        
        // Step 5: Remove any remaining non-printable or problematic characters at the start
        result = result.replaceAll("^[^a-zA-Z0-9\\s]*", ""); // Remove non-alphanumeric chars from start
        
        // Step 6: Clean up whitespace
        result = result.replaceAll("\\s+", " ") // Multiple whitespace to single space
                      .trim();
        
        // Simple whitelist approach: only keep letters, numbers, punctuation, spaces, [], and {}
        // This removes all ANSI codes, spinner characters, and encoding artifacts
        result = result.replaceAll("[^a-zA-Z0-9\\s\\p{Punct}\\[\\]\\{\\}]", "");
        
        // Clean up excessive whitespace
        result = result.replaceAll("\\s+", " ").trim();
        
        // If result is empty or very short, return it as-is
        if (result.length() <= 10) {
            return result;
        }
        
        // Step 7: Extract actual content - look for lines with substantial text
        if (result.length() > 0) {
            String[] lines = result.split("\\n");
            for (String line : lines) {
                line = line.trim();
                // Look for lines that start with actual words (not symbols) and have substantial content
                if (line.length() > 10 && line.matches("^[a-zA-Z].*[a-zA-Z]{3,}.*")) {
                    return line;
                }
            }
        }
        
        return result;
    }

}
