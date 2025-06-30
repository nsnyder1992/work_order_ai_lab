# Welcome to Work Order AI Lab

This is a vulnerable web application designed for red team training and AI security research. It's an internal-facing app built to simulate common web security flaws and demonstrate how AI assistants can be manipulated in insecure environments.

## 🚧 Status
Work in Progress — core features are built, and several vulnerabilities have been intentionally included for demonstration. Future updates will polish the admin panel and expand AI agent behavior.

## 💡 Purpose
This project was built as a security demo for:

- Internal red team education
- Prompt injection and AI misuse testing
- Showcasing insecure patterns in legacy apps (e.g. JSP + Java Servlets)
- Demonstrating how offensive security tools can integrate with LLMs

## ⚙️ Tech Stack
- Java (JSP, Servlets) + Tomcat
- Python (AI agent CLI integration)
- SQLite (with intentionally weak MD5 credential hashing)
- Frontend: basic HTML/CSS
- Security: deliberately vulnerable patterns (SSTI, LFI, insecure file upload, etc.)

## 🧪 Demo Flow
1. User signs up
2. Work Orders can be created/viewed, with file uploads
3. AI Assistant (right-hand panel) is trained on the DB and attempts to help users troubleshoot
4. Admin Panel is hidden behind a password hash the AI assistant knows (crackable via CUPP + John the Ripper, and social engineering about page)
5. Vulnerabilities include:
- SSTI via templated inputs
- LFI in file viewer
- Prompt injection via AI assistant
- Exposure of SSH keys via RCE chain

## 🔐 Offensive Security Notes
- Exploit paths are intended to be educational
- No external dependencies required — can be run locally (see setup below to install local LLM)
- Designed to support automated testing via Python scripting

## 🧠 Future Work
- File Viewer (2 stage File Upload to bypass WAF -> LFI -> RCE)
- Integrate LangChain agent for smarter AI actions
- Expand the RCE and SSRF vectors
- Add user activity logging and detection simulation
- Better protect against other Known vulnerabilities. (LLM Output XSS, LLM Input RCE, etc)

## 📎 Disclaimer
This project is for educational and internal demo purposes only. Do not expose it to the public internet.

## ⚙️ Setup Instructions

### 1. Install Ollama

To us the AI functionality in the app you'll need [Ollama]([https://pages.github.com/](https://ollama.com/)). Download and install it for your platform, then pull the Mistral model:

```bash
ollama pull mistral
```

### 2. Build the Java Web App

Clone the repo and build the project using Maven:

```bash
mvn clean package
```
Deploy the generated WAR file to your Tomcat server:
```bash
cp target/work-ai.war /path/to/tomcat/webapps/
```
Start Tomcat, then access the app in your browser. Make sure a user account is created via the signup page.

### 3. Optional: Start the Agentic AI App (Beta)

Navigate to the resources/ directory and generate a local SSL certificate:

```bash
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes
```

Start the Python agent server:

```bash
python -m work-order-agent.py
```

### 4. Create a Work Order via the AI Agent

You can now interact with the AI agent using `curl`. Replace `{user_id}` and `{secret-token}` with your actual values:

```bash
curl -k -X POST https://localhost:5000/chat \
  -H "Authorization: Bearer {secret-token}" \
  -H "Content-Type: application/json" \
  -d '{"user_id": "{user_id}", "prompt": "Create a new work order for a broken motor."}'
```

## 🛠️ Notes
- The AI agent requires HTTPS for token-based auth — hence the self-signed certificate.
- Ensure the user account is created before sending prompts via the /chat endpoint.
- The agent uses the Mistral model locally via Ollama to process and respond to prompts.
