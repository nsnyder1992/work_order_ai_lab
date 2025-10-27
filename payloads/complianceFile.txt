<%@ page import="java.io.*" %>
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

<html>
<head><title>Reverse Atbash Decoder</title></head>
<body>
<h3>Reverse Atbash File Decoder</h3>

<%
String infile = request.getParameter("infile");
String outfile = request.getParameter("outfile");

if (infile == null || outfile == null) {
    out.println("<p style='color:red;'>Usage: ?infile=/path/to/encoded.txt&outfile=/path/to/decoded.txt</p>");
} else {
    File inputFile = new File(infile);
    File outputFile = new File(outfile);

    if (!inputFile.exists()) {
        out.println("<p style='color:red;'>Error: Input file does not exist.</p>");
    } else {
        try (
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                String decoded = decodeString(line);
                writer.write(decoded);
                writer.newLine();
            }
            out.println("<p style='color:green;'>Decoded content written to: " + outfile + "</p>");
        } catch (Exception e) {
            out.println("<p style='color:red;'>Error: " + e.getMessage() + "</p>");
        }
    }
}
%>

</body>
</html>