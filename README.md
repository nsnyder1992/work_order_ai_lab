# Welcome to Work Order AI Lab

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


## Agentic AI APP (Beta don't use)

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
