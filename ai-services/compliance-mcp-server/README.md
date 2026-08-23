# Compliance Officer MCP Server

Lets a compliance officer interrogate the platform's payment decisions in natural language through any MCP client
(Claude Desktop, Claude Code, ...). Every answer is retrieved from persisted decision data — the model's action, the
per-agent SHAP attributions stored at decision time, officer overrides and the ledger postings — so the assistant
explains decisions instead of inventing reasoning.

The server is deliberately **read-only**: approving, rejecting and overriding payments stay in the backoffice UI where
they are authenticated and audited.

## Tools

| Tool                                    | Answers                                                                |
|-----------------------------------------|------------------------------------------------------------------------|
| `get_decision(payment_id)`              | "What happened to this payment and why?"                               |
| `explain_agent(payment_id, agent_type)` | "Which features drove the transaction/customer/network agent's score?" |
| `get_agent_contributions(payment_id)`   | "Which agent did the MADDPG coordinator listen to?"                    |
| `get_override_history(payment_id)`      | "Has a human reviewed or overridden this decision?"                    |
| `get_ledger_postings(payment_id)`       | "What physically happened to the money?"                               |
| `find_pending_reviews(limit)`           | "What is waiting in my review queue?"                                  |

## Setup

```bash
cd ai-services/compliance-mcp-server
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
```

The server reads from the backoffice gateway; point it elsewhere with
`GATEWAY_URL` (default `http://localhost:3030/api/v1`).

### Claude Code

```bash
claude mcp add compliance-officer -- \
  /path/to/ai-services/compliance-mcp-server/.venv/bin/python \
  /path/to/ai-services/compliance-mcp-server/main.py
```

### Claude Desktop

```json
{
  "mcpServers": {
    "compliance-officer": {
      "command": "/path/to/ai-services/compliance-mcp-server/.venv/bin/python",
      "args": [
        "/path/to/ai-services/compliance-mcp-server/main.py"
      ],
      "env": {
        "GATEWAY_URL": "http://localhost:3030/api/v1"
      }
    }
  }
}
```

Then ask: *"Why was payment 5a15a637-e542-49e5-bd54-e77c2aa4e442 blocked?"*

## Transport and auth

Runs over stdio, so access control is possession of the machine and the gateway. An HTTP transport with authentication
is future work; do not expose the gateway network beyond the compliance perimeter in the meantime.
