import ssl
import sqlite3
from flask import Flask, request, jsonify
from datetime import datetime
from langchain.agents import initialize_agent, AgentType
from langchain_community.llms import Ollama
from langchain.tools import Tool
from langchain.memory.chat_memory import BaseChatMemory
from langchain.schema import messages_from_dict, messages_to_dict, SystemMessage, BaseMessage
import requests
import os
import json

# --- Config ---
DB_PATH = "chat_history.db"
AUTH_TOKEN = os.environ.get("AGENT_AUTH_TOKEN", "secret-token")

# --- Database Setup ---
conn = sqlite3.connect(DB_PATH, check_same_thread=False)
c = conn.cursor()
c.execute("""
CREATE TABLE IF NOT EXISTS chat_history (
    user_id TEXT,
    role TEXT,
    message TEXT,
    timestamp TEXT DEFAULT (datetime('now'))
)
""")
conn.commit()

def save_message(user_id, role, message):
    timestamp = datetime.utcnow().isoformat()
    c.execute("""
        INSERT INTO chat_history (user_id, role, message, timestamp)
        VALUES (?, ?, ?, ?)
    """, (user_id, role, message, timestamp))
    conn.commit()

def load_recent_messages(user_id, limit=5):
    c.execute("""
        SELECT role, message FROM chat_history
        WHERE user_id = ?
        ORDER BY timestamp DESC
        LIMIT ?
    """, (user_id, limit * 2))
    rows = reversed(c.fetchall())  # reverse to oldest->newest
    return [{"type": "human" if role == "user" else "ai", "data": message} for role, message in rows]

# --- Custom Memory Class ---
class SQLiteChatMemory(BaseChatMemory):
    def __init__(self, user_id: str):
        super().__init__()
        self.user_id = user_id

    @property
    def memory_variables(self):
        return ["chat_history"]

    def load_memory_variables(self, inputs):
        messages = load_recent_messages(self.user_id)
        return {"chat_history": messages_from_dict(messages)}

    def save_context(self, inputs, outputs):
        save_message(self.user_id, "user", inputs["input"])
        save_message(self.user_id, "ai", outputs["output"])

    def clear(self):
        c.execute("DELETE FROM chat_history WHERE user_id = ?", (self.user_id,))
        conn.commit()

# --- Tools ---
def read_user_work_orders(user_id: int) -> str:
    headers = {"Authorization": f"Bearer {AUTH_TOKEN}"}
    response = requests.get("http://localhost:8080/api/workorders/", headers=headers, json={"user_id": user_id})
    return response.text if response.ok else f"Failed to read work orders: {response.text}"

def write_work_order(work_order_data: str) -> str:
    headers = {"Authorization": f"Bearer {AUTH_TOKEN}"}
    response = requests.post("http://localhost:8080/api/workorders/", headers=headers, json={"data": work_order_data})
    return "Work order created." if response.ok else f"Failed to create work order: {response.text}"

read_tool = Tool(name="Read Work Orders", func=read_user_work_orders, description="Reads work orders from backend using user_id.")
write_tool = Tool(name="Create Work Order", func=write_work_order, description="Creates a work order from input.")

# --- Flask App ---
app = Flask(__name__)

@app.route("/chat", methods=["POST"])
def chat():
    auth = request.headers.get("Authorization", "")
    if not auth.startswith("Bearer ") or auth.split(" ")[1] != AUTH_TOKEN:
        return jsonify({"error": "Unauthorized"}), 401

    data = request.get_json()
    user_id = data.get("user_id")
    role = data.get("role", "user")
    sys_work_orders = data.get("sys_work_orders", None)
    remainingTokens = data.get("remainingTokens", 0)
    user_work_orders = data.get("user_work_orders", None)
    prompt = data.get("prompt")

    if not user_id or not prompt:
        return jsonify({"error": "user_id and prompt are required"}), 400

    memory = SQLiteChatMemory(user_id=user_id)
    sytem_prompt = None
    with open("context.txt", "r") as f:
        system_prompt_content = f.read().strip()
        if system_prompt_content:
            system_prompt_content = system_prompt_content.replace("{{sys_work_orders}}", sys_work_orders or "No system work orders found.").replace("{{remainingTokens}}", str(remainingTokens)).replace("{{user_work_orders}}", user_work_orders or "No user work orders found.").replace("{{user_id}}", user_id).replace("{{role}}", role)
                                        
            system_prompt = SystemMessage(content=system_prompt_content)

    memory.chat_memory.messages.append(system_prompt)
    llm = Ollama(temperature=0, model="mistral", max_tokens=512)

    agent = initialize_agent(
        tools=[read_tool, write_tool],
        llm=llm,
        agent=AgentType.ZERO_SHOT_REACT_DESCRIPTION,
        verbose=False,
        memory=memory,
        
    )

    response = agent.run(prompt)
    return jsonify({"response": response})

# --- HTTPS Setup ---
if __name__ == "__main__":
    print("✅ Agent server running on https://localhost:5000")
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.load_cert_chain(certfile="cert.pem", keyfile="key.pem")
    app.run(host="0.0.0.0", port=5000, ssl_context=context)
