package com.redteam.labs.workorder.filters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import com.redteam.labs.workorder.model.User;
import com.redteam.labs.workorder.util.PayloadDecoderUtils;
import com.redteam.labs.workorder.util.WebPayloadMonitor;

import org.apache.commons.text.similarity.LevenshteinDistance;

@WebFilter("/jsp/*") // Apply filter to all requests
public class WAFPayloadFilter implements javax.servlet.Filter
{

    private static final LevenshteinDistance distance = new LevenshteinDistance();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        // Initialization logic if needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        User user = (User) httpRequest.getSession().getAttribute("user");
        String username = (user != null) ? user.getUsername() : "anonymous";

        if (WebPayloadMonitor.isLocked(username))
        {
            HttpSession session = (HttpSession) httpRequest.getSession();
            session.invalidate();
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Account locked due to malicious activity.");
            return;
        }
        
        String contentType = httpRequest.getContentType();
        if (contentType == null)
            contentType = "unknonwn"; // Default to unknown if not set

        switch (contentType)
        {
        case "multipart/form-data":

            for (Part part : httpRequest.getParts())
            {
                String input = getFormField(part);
                if (input != null && containsMaliciousPayload(input))
                {
                    String errorMessage = WebPayloadMonitor.registerAttackAttempt(username);
                    httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, errorMessage);
                    return;
                }
            }

        default:
            Map<String, String[]> parameterMap = httpRequest.getParameterMap();
            for(Entry<String, String[]> entry : parameterMap.entrySet())
            {
                String key = entry.getKey();
                String[] values = entry.getValue();
                for (String value : values)
                {
                    if (containsMaliciousPayload(value))
                    {
                        String errorMessage = WebPayloadMonitor.registerAttackAttempt(username);
                        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, errorMessage);
                        return;
                    }
                }
            }
        }

        // Clean input, reset WAF strike count if last attempt was 5 minutes ago
        WebPayloadMonitor.reset(username);
        httpResponse.setHeader("X-WAF-Status", "OK");

        chain.doFilter(request, response);
    }

    private String getFormField(Part part) throws IOException
    {
        InputStream is = part.getInputStream();
        return new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n")).trim();
    }

    // Known web attack payload signatures
    private static final List<String> DANGEROUS_PATTERNS = Arrays.asList(
            // XSS
            "<script>", "javascript:", "onerror=", "onload=", "scr<script>ipt", "onerr%6Fr", "onerr&#111;r", "a%6C%65rt", "a&#108;lert", 
            
            // LFI
            "../../", "/etc/passwd", "file://", "%2e%2e%2f", "../../", "..\\", "%2e%2e%2f", "%2e%2e%5c", 
            "%252e%252e%252f", "/etc/passwd", "/proc/self/environ", "boot.ini",

            // RFI patterns
            "http://", "https://", "file://", "php://", "data://", "ftp://", "expect://",
            
            // RCE
            "Runtime.getRuntime", "ProcessBuilder", 
            
            // JS RCE
            "eval(", "exec(", 
            
            // common reverse shell tools
            "wget ", "curl ", "nc ", "bash -i", "netcat", "127.0.0.1", 
            
            // JSP webshells
            "out.println", "java.lang.Runtime", "<jsp:include", "<jsp:forward", "pageContext.getRequest()",
            "<jsp:", "base64_decode(", 
            "System.exit(", "ObjectInputStream", "ObjectOutputStream",     
            "<% Runtime.getRuntime().exec %>", "<% request.getParameter %>", "<jsp:include page=", "<jsp:forward page=",
            "<%= out.println %>", 
            "<% new java.io.File %>", 
            "<% new java.net.Socket %>", "<% new java.util.Scanner %>",
            "<% javax.script.ScriptEngineManager %>", "<% org.apache.commons.io.IOUtils %>", "<% org.springframework.expression.SpelExpressionParser %>",
            
            
            // SQL injection patterns
            "union select", "or 1=1", "and 1=1", "select * from", "drop table", "information_schema",
            "insert into", "update set", "delete from", "xp_cmdshell", "sp_executesql", "waitfor delay",
            "sqlite_master", "pragma", "attach database", "limit", "offset");

    // Precompiled regexes for flexible detection
    private static final List<Pattern> REGEX_PATTERNS = Arrays.asList(
            // XSS
            Pattern.compile("(?i)<script.*?>.*?</script>"), Pattern.compile("(?i)on\\w+=['\"].*?['\"]"),
            Pattern.compile("(?i)javascript:.*"), Pattern.compile("(?i)document\\.cookie"),
            Pattern.compile("(?i)<script\\s+.*?>.*?</script>"), // Inline script with attributes
            Pattern.compile("(?i)style=['\"].*expression\\(.*\\).*['\"]"), // CSS expression
            Pattern.compile("(?i)%3C.*?script.*?%3E"), // Encoded script tag
            Pattern.compile("(?i)(alert|document\\.write|window\\.location)"), // JavaScript keywords

            // SQLi
            Pattern.compile("(?i)union.*select"), Pattern.compile("(?i)(or|and)\\s+\\d+=\\d+"),
            Pattern.compile("(?i)select.*from.*"), Pattern.compile("(?i)drop\\s+table"), Pattern.compile("(?i)information_schema"),
            Pattern.compile("(?i)char\\s*\\("), // Character-based payloads
            Pattern.compile("(?i)0x[0-9a-f]+"), // Hexadecimal payloads
            Pattern.compile("(?i)cast\\s*\\("), // Type casting
            Pattern.compile("(?i)convert\\s*\\("), // Data conversion
            Pattern.compile("(?i)group\\s+by\\s+.*having"), // Grouping with conditions
            Pattern.compile("(?i)waitfor\\s+delay"), // Time-based SQL injection
            Pattern.compile("(?i)xp_cmdshell"), // SQL Server command execution
            Pattern.compile("(?i)exec\\s+xp_"), // Extended stored procedures
            Pattern.compile("(?i)sp_executesql"), // Dynamic SQL execution
            Pattern.compile("(?i)select\\s+.*\\s+from\\s+.*\\s+where\\s+.*=.*"), // Complex SELECT statements
            Pattern.compile("(?i)insert\\s+into\\s+.*\\s+values\\s+.*"), // INSERT statements
            Pattern.compile("(?i)update\\s+.*\\s+set\\s+.*=.*"), // UPDATE statements
            Pattern.compile("(?i)delete\\s+from\\s+.*\\s+where\\s+.*=.*"), // DELETE statements
            

            // SQLite-specific patterns
            Pattern.compile("(?i)sqlite_master"), // SQLite metadata table
            Pattern.compile("(?i)pragma\\s+.*"), // SQLite settings
            Pattern.compile("(?i)attach\\s+database\\s+.*"), // Attach database
            Pattern.compile("(?i)limit\\s+\\d+"), // LIMIT clause
            Pattern.compile("(?i)offset\\s+\\d+"), // OFFSET clause
            Pattern.compile("(?i)select\\s+.*\\s+from\\s+sqlite_master"), // Querying SQLite metadata
            Pattern.compile("(?i)select\\s+.*\\s+from\\s+sqlite_temp_master"), // Querying temporary metadata
            
            // LFI / RFI
            Pattern.compile("\\.\\./"), Pattern.compile("(?i)/etc/passwd"), 
            Pattern.compile("(?i)(http|https)://.*\\.jsp"), // Remote PHP file inclusion
            Pattern.compile("(?i)(http|https)://.*\\.php"), // Remote PHP file inclusion
            Pattern.compile("(?i)(http|https)://.*\\.txt"), // Remote text file inclusion
            Pattern.compile("(?i)(http|https)://.*\\.log"), // Remote log file inclusion
            Pattern.compile("(?i)(http|https)://.*\\.ini"), // Remote configuration file inclusion
            Pattern.compile("(?i)(http|https)://.*\\.html"), // Remote HTML file inclusion
            Pattern.compile("(?i)file:\\/\\/.*"), // File protocol inclusion
            Pattern.compile("(?i)php:\\/\\/.*"), // PHP stream inclusion
            Pattern.compile("(?i)data:\\/\\/.*"), // Data URI inclusion
            Pattern.compile("(?i)ftp:\\/\\/.*"), // FTP protocol inclusion
            Pattern.compile("(?i)expect:\\/\\/.*"), // Expect protocol inclusion
            
            // Command injection
            Pattern.compile("(?i);\\s*(ls|cat|whoami|nc|curl|wget|bash)"), // Command separator with common commands
            Pattern.compile("(?i)\\|\\s*(ls|nc|bash|curl)"), // Pipe operator with commands
            Pattern.compile("(?i)&\\s*(ls|cat|whoami|nc|curl|wget|bash)"), // Background operator with commands
            Pattern.compile("(?i)\\$\\(.*\\)"), // Command substitution
            Pattern.compile("(?i)`.*`"), // Backtick command execution
            Pattern.compile("(?i)exec\\s*\\("), // Exec function
            Pattern.compile("(?i)python\\s+-c"), // Python inline execution
            Pattern.compile("(?i)bash\\s+-c"), // Bash inline execution
            Pattern.compile("(?i)sh\\s+-c"), // Shell inline execution
            Pattern.compile("(?i)rm\\s+-rf"), // Recursive file deletion
            Pattern.compile("(?i)mkfifo"), // Named pipe creation
            Pattern.compile("(?i)nc\\s+-e"), // Netcat command execution
            Pattern.compile("(?i)curl\\s+-o"), // Curl file download
            Pattern.compile("(?i)wget\\s+-O"), // Wget file download
            Pattern.compile("(?i)scp\\s+.*"), // Secure copy command
            Pattern.compile("(?i)ssh\\s+.*"), // SSH command
            Pattern.compile("(?i)telnet\\s+.*"), // Telnet command
            Pattern.compile("(?i)ping\\s+.*"), // Ping command
            Pattern.compile("(?i)traceroute\\s+.*"), // Traceroute command
            Pattern.compile("(?i)nslookup\\s+.*"), // Nslookup command
            Pattern.compile("(?i)dig\\s+.*"), // Dig command
            Pattern.compile("(?i)ifconfig"), // Network configuration
            Pattern.compile("(?i)ipconfig"), // Windows network configuration
            Pattern.compile("(?i)netstat"), // Network statistics
            Pattern.compile("(?i)whoami"), // User identity
            Pattern.compile("(?i)uname\\s+-a"), // System information
            Pattern.compile("(?i)ps\\s+-ef"), // Process listing
            Pattern.compile("(?i)kill\\s+-9"), // Process termination
            Pattern.compile("(?i)chmod\\s+.*"), // File permission modification
            Pattern.compile("(?i)chown\\s+.*"), // File ownership modification
            Pattern.compile("(?i)touch\\s+.*"), // File creation
            Pattern.compile("(?i)echo\\s+.*"), // Echo command
            Pattern.compile("(?i)pwd"), // Print working directory
            
            // RCE
            Pattern.compile("(?i)system\\("), 
            Pattern.compile("(?i)eval\\("), 
            Pattern.compile("(?i)exec\\("),
            Pattern.compile("(?i)python\\s+-c"), Pattern.compile("(?i)base64_decode\\("),
            Pattern.compile("(?i)Runtime\\.getRuntime\\(\\)\\.exec\\("), // Runtime execution
            Pattern.compile("(?i)ProcessBuilder\\s*\\("), // ProcessBuilder execution
            Pattern.compile("(?i)java\\.lang\\.Runtime"), // Runtime class reference
            Pattern.compile("(?i)java\\.lang\\.ProcessBuilder"), // ProcessBuilder class reference
            Pattern.compile("(?i)javax\\.script\\.ScriptEngineManager"), // Script engine for dynamic code execution
            Pattern.compile("(?i)groovy\\.lang\\.GroovyShell"), // Groovy shell execution
            Pattern.compile("(?i)org\\.apache\\.commons\\.io\\.IOUtils"), // Apache Commons IO for file manipulation
            Pattern.compile("(?i)org\\.springframework\\.expression\\.SpelExpressionParser"), // Spring EL injection
            Pattern.compile("(?i)java\\.nio\\.file\\.Files"), // File manipulation
            Pattern.compile("(?i)java\\.util\\.Scanner"), // Scanner for input streams
            Pattern.compile("(?i)java\\.net\\.URL\\.openConnection"), // URL connection for remote access
            Pattern.compile("(?i)java\\.net\\.Socket"), // Socket creation
            Pattern.compile("(?i)java\\.rmi\\.Naming\\.lookup"), // RMI lookup
            Pattern.compile("(?i)javax\\.management\\.MBeanServerConnection"), // JMX connection
            Pattern.compile("(?i)org\\.apache\\.tomcat\\.util\\.http\\.fileupload\\.DiskFileItem"), // File upload manipulation
            Pattern.compile("(?i)org\\.apache\\.tomcat\\.util\\.http\\.fileupload\\.FileItem"), // File upload manipulation
            Pattern.compile("(?i)org\\.springframework\\.web\\.multipart\\.MultipartFile"), // Spring MultipartFile
            Pattern.compile("(?i)java\\.io\\.ObjectInputStream"), // Deserialization attack
            Pattern.compile("(?i)java\\.io\\.ObjectOutputStream"), // Deserialization attack
            Pattern.compile("(?i)java\\.beans\\.XMLDecoder"), // XML deserialization
            Pattern.compile("(?i)java\\.beans\\.XMLEncoder"), // XML serialization
            Pattern.compile("(?i)org\\.yaml\\.snakeyaml\\.Yaml"), // YAML deserialization
            Pattern.compile("(?i)org\\.codehaus\\.jackson\\.map\\.ObjectMapper"), // Jackson deserialization
            Pattern.compile("(?i)com\\.fasterxml\\.jackson\\.databind\\.ObjectMapper"), // FasterXML Jackson deserialization
            Pattern.compile("(?i)org\\.apache\\.commons\\.collections\\.Transformer"), // Commons Collections deserialization
            Pattern.compile("(?i)org\\.apache\\.commons\\.collections\\.functors\\.InvokerTransformer"), // Commons Collections transformer
            Pattern.compile("(?i)org\\.apache\\.commons\\.collections\\.functors\\.ChainedTransformer"), // Commons Collections chained transformer
            Pattern.compile("(?i)org\\.apache\\.commons\\.collections\\.functors\\.ConstantTransformer"), // Commons Collections constant transformer
            Pattern.compile("(?i)org\\.apache\\.commons\\.collections\\.functors\\.InstantiateTransformer"), // Commons Collections instantiate transformer
            Pattern.compile("(?i)org\\.apache\\.commons\\.collections\\.functors\\.InvokerTransformer"), // Commons Collections invoker transformer
            Pattern.compile("(?i)org\\.apache\\.commons\\.collections\\.functors\\.LazyMap"), // Commons Collections lazy map
            Pattern.compile("(?i)org\\.apache\\.commons\\.collections\\.functors\\.MapEntryTransformer"), // Commons Collections map entry transformer
            Pattern.compile("(?i)org\\.apache\\.commons\\.collections\\.functors\\.PredicateTransformer"), // Commons Collections predicate transformer
            Pattern.compile("(?i)org\\.apache\\.commons\\.collections\\.functors\\.TransformerPredicate"), // Commons Collections transformer predicate
            
            // JSP webshell detection
            Pattern.compile("(?i)<%.*Runtime\\.getRuntime\\(\\).*%>"), // Runtime execution
            Pattern.compile("(?i)<%.*request\\.getParameter\\(.*\\).*%>"), // Parameter-based execution
            Pattern.compile("(?i)<jsp:(include|forward).*?page\\s*=.*?>"), // JSP directives
            Pattern.compile("(?i)pageContext\\.getRequest\\(\\)"), // Page context manipulation
            Pattern.compile("(?i)<%.*out\\.println\\(.*\\).*%>"), // Output printing
            Pattern.compile("(?i)<%.*new\\s+java\\.io\\.File\\(.*\\).*%>"), // File manipulation
            Pattern.compile("(?i)<%.*new\\s+java\\.net\\.Socket\\(.*\\).*%>"), // Socket creation
            Pattern.compile("(?i)<%.*new\\s+java\\.util\\.Scanner\\(.*\\).*%>"), // Scanner usage
            Pattern.compile("(?i)<%.*javax\\.script\\.ScriptEngineManager\\(.*\\).*%>"), // Script engine execution
            Pattern.compile("(?i)<%.*org\\.apache\\.commons\\.io\\.IOUtils\\(.*\\).*%>"), // Apache Commons IO usage
            Pattern.compile("(?i)<%.*org\\.springframework\\.expression\\.SpelExpressionParser\\(.*\\).*%>") // Spring EL injection
            );

    // Levenshtein similarity threshold
    private static final int LEVENSHTEIN_THRESHOLD = 2;

    public static boolean containsMaliciousPayload(String input)
    {
        
        String deccodedInput = PayloadDecoderUtils.urlDecode(input);
        String normalized = normalize(deccodedInput);
        
        if (detect(normalized))
            return true;
        
        deccodedInput = PayloadDecoderUtils.doubleUrlDecode(input);
        normalized = normalize(deccodedInput);

        if (detect(normalized))
            return true;
        
        deccodedInput = PayloadDecoderUtils.base64Decode(input);
        normalized = normalize(deccodedInput);
        if (detect(normalized))
            return true;
        
        deccodedInput = PayloadDecoderUtils.hexDecode(input);
        normalized = normalize(deccodedInput);
        if (detect(normalized))
            return true;
        
        deccodedInput = PayloadDecoderUtils.unicodeDecode(input);
        normalized = normalize(deccodedInput);
        if (detect(normalized))
            return true;
        
        deccodedInput = PayloadDecoderUtils.htmlEntityDecode(input);
        normalized = normalize(deccodedInput);
        if (detect(normalized))
            return true;
        
        deccodedInput = PayloadDecoderUtils.decodeDecimalHtmlEntity(input);
        normalized = normalize(deccodedInput);
        if (detect(normalized))
            return true;
        
        deccodedInput = PayloadDecoderUtils.decodeHexHtmlEntity(input);
        normalized = normalize(deccodedInput);
        if (detect(normalized))
            return true;
        
        deccodedInput = PayloadDecoderUtils.rot13(input);
        normalized = normalize(deccodedInput);
        if (detect(normalized))
            return true;
        
        deccodedInput = PayloadDecoderUtils.rot13(input);
        normalized = normalize(deccodedInput);
        if (detect(normalized))
            return true;
        
        // If no dangerous patterns or regex matches found
        return false;
    }
    
    
    private static boolean detect(String input) {
        if (detectPattern(input) || detectRegex(input))
            return true;
        
        return false;
    }
    
    private static boolean detectPattern(String input) {
        if (input == null || input.isEmpty())
            return false;
        
        for (String pattern : DANGEROUS_PATTERNS)
        {
            if (input.contains(pattern.toLowerCase()))
                return true;

            // Levenshtein fuzzy match
            if (distance.apply(input, pattern.toLowerCase()) <= LEVENSHTEIN_THRESHOLD)
                return true;
        }
        return false;
    }

    private static boolean detectRegex(String input) {
        // Check regex-based evasions
        for (Pattern pattern : REGEX_PATTERNS)
        {
            if (pattern.matcher(input).find())
                return true;
        }

        return false;
    }
    
    // Normalize input: lowercase, remove escape characters, decode basic entities
    private static String normalize(String input)
    {
        String normalized = input.toLowerCase();
        normalized = normalized.replaceAll("\\\\", ""); // remove escape slashes
        normalized = normalized.replaceAll("%20", " "); // decode space
        normalized = normalized.replaceAll("&lt;", "<");
        normalized = normalized.replaceAll("&gt;", ">");
        normalized = normalized.replaceAll("&quot;", "\"");
        normalized = normalized.replaceAll("&#x27;", "'");
        normalized = normalized.replaceAll("&#x2F;", "/");
        return normalized;
    }

}
