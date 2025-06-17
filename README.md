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

## 📎 Disclaimer
This project is for educational and internal demo purposes only. Do not expose it to the public internet.

## Setup
To get started you will need to download and install Ollama from here: https://ollama.com/

Then open a command prompt and run the following command

```bash
ollama pull mistral
```

After that build this project:

```bash
mvn clean package
```

Then drop the war file at `target/work-ai.war` into a tomcat webapps directory and start hacking :)


## Agentic AI APP (Beta)

Run the following command in resources:

```bash
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes
```

Then to run the app:

```bash
python -m work-order-agent.py
```

Make sure your tomcat is already up and running, and you have a user signed up. Then you can send the following to create a workorder after you change `{user_id}` for your user id and `{secret-token}` for your token:-H "Content-Type: application/json"

```bash
curl -k -X POST https://localhost:5000/chat \
  -H "Authorization: Bearer {secret-token}" \
  -H "Content-Type: application/json" \
  -d '{"user_id": "{user_id}", "prompt": "Create a new work order for a broken motor."}'
```
