import ssl
import sqlite3
from flask import Flask, request, jsonify
from datetime import datetime
from langchain.agents import initialize_agent, AgentType
from langchain_ollama import OllamaLLM
from langchain.tools import Tool
from langchain.memory.chat_memory import BaseChatMemory
from langchain.schema import messages_from_dict, messages_to_dict, SystemMessage
from langchain.memory import ConversationBufferMemory
from langchain_core.chat_history import InMemoryChatMessageHistory
import requests
import os
import json

# --- Config ---
AUTH_TOKEN = os.environ.get("AGENT_AUTH_TOKEN", "secret-token")

user_memories = {}

def get_memory(user_id):
    if user_id not in user_memories:
        user_memories[user_id] = ConversationBufferMemory(
            chat_memory=InMemoryChatMessageHistory(),
            return_messages=True
        )
    return user_memories[user_id]
# --- Tools ---
def read_user_work_orders(user_id: int) -> str:
    headers = {"Authorization": f"Bearer {AUTH_TOKEN}"}
    response = requests.get("http://localhost:9040/api/workorders/", headers=headers, json={"user_id": user_id})
    return response.text if response.ok else f"Failed to read work orders: {response.text}"

def write_work_order(work_order_data: str) -> str:
    headers = {"Authorization": f"Bearer {AUTH_TOKEN}"}
    response = requests.post("http://localhost:9040/api/workorders/", headers=headers, json={"data": work_order_data})
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
    
    print(request.headers)  # Debugging line to see headers
    print(request)
    data = request.get_json()
    print(f"Received data: {data}")  # Debugging line to see incoming data
    
    user_id = data.get("user_id")
    role = data.get("role", "user")
    sys_work_orders = data.get("sys_work_orders", None)
    remainingTokens = data.get("remainingTokens", 0)
    user_work_orders = data.get("user_work_orders", None)
    prompt = data.get("prompt")

    if not user_id or not prompt:
        return jsonify({"error": "user_id and prompt are required"}), 400
    
    memory = get_memory(user_id)

    sytem_prompt = None
    with open("context.txt", "r", encoding="utf-8", errors="ignore") as f:
        system_prompt_content = f.read().strip()
        if system_prompt_content:
            system_prompt_content = system_prompt_content.replace("{{sys_work_orders}}", sys_work_orders or "No system work orders found.").replace("{{remainingTokens}}", str(remainingTokens)).replace("{{user_work_orders}}", user_work_orders or "No user work orders found.").replace("{{user_id}}", str(user_id)).replace("{{role}}", role)
                                        
            system_prompt = SystemMessage(content=system_prompt_content)

    memory.chat_memory.messages.append(system_prompt.dict())
    llm = OllamaLLM(temperature=0, model="mistral")

    agent = initialize_agent(
        tools=[read_tool, write_tool],
        llm=llm,
        agent=AgentType.ZERO_SHOT_REACT_DESCRIPTION,
        verbose=False,
        memory=memory,
        
    )

    response = agent.invoke({"input": prompt})
    
    # Attempt to safely get text
    if hasattr(response, "content"):
        text = response.content
    elif isinstance(response, dict):
        # Try keys likely to hold text
        text = response.get("output") or response.get("text") or str(response)
    else:
        text = str(response)
    
    return jsonify({"response": text})

# --- HTTPS Setup ---
if __name__ == "__main__":
    print("✅ Agent server running on https://localhost:5000")
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.load_cert_chain(certfile="cert.pem", keyfile="key.pem")
    app.run(host="0.0.0.0", port=5000, ssl_context=context)
