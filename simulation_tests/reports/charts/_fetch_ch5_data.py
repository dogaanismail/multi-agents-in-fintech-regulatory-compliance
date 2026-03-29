"""Fetch training convergence data and results.csv stats for Chapter 5 figures."""
import psycopg2
import csv
import json
import os

BASE = '/Users/ismaildogan/IdeaProjects/multi-agents-in-fintech-regulatory-compliance'

# ── 1. Training runs from DB ──────────────────────────────────────────
print("=" * 60)
print("TRAINING RUNS")
print("=" * 60)
try:
    conn = psycopg2.connect(
        host='localhost', port=5433,
        dbname='marl_orchestrator_db',
        user='postgres', password='postgres'
    )
    cur = conn.cursor()
    
    cur.execute("SELECT column_name FROM information_schema.columns WHERE table_name='training_runs' ORDER BY ordinal_position")
    cols = [r[0] for r in cur.fetchall()]
    print(f"Columns: {cols}")
    
    cur.execute("SELECT COUNT(*) FROM training_runs")
    print(f"Total runs: {cur.fetchone()[0]}")
    
    cur.execute("""
        SELECT id, created_at, status, batch_size, buffer_size, 
               critic_loss, actor_losses, training_duration_ms
        FROM training_runs ORDER BY id
    """)
    for row in cur.fetchall():
        print(row)
    
    conn.close()
except Exception as e:
    print(f"DB error: {e}")

# ── 2. Results.csv stats ──────────────────────────────────────────────
print("\n" + "=" * 60)
print("RESULTS.CSV STATS")
print("=" * 60)
results_path = os.path.join(BASE, 'simulation_tests/reports/results.csv')
if os.path.exists(results_path):
    with open(results_path) as f:
        reader = csv.DictReader(f)
        rows = list(reader)
    print(f"Total rows: {len(rows)}")
    print(f"Columns: {list(rows[0].keys())}")
    
    # Orchestrator confidence stats
    confs = [float(r['orchestrator_conf']) for r in rows if r.get('orchestrator_conf')]
    print(f"\nOrchestrator confidence:")
    print(f"  Count: {len(confs)}")
    if confs:
        print(f"  Min: {min(confs):.4f}")
        print(f"  Max: {max(confs):.4f}")
        print(f"  Mean: {sum(confs)/len(confs):.4f}")
    
    # Orchestrator actions
    from collections import Counter
    actions = Counter(r.get('orchestrator_action', '') for r in rows)
    print(f"\nOrchestrator actions: {dict(actions)}")
    
    # Detection results
    det = Counter(r.get('detection_result', '') for r in rows)
    print(f"Detection results: {dict(det)}")
    
    # Fraud labels
    labels = Counter(r.get('fraud_label', '') for r in rows)
    print(f"Fraud labels: {dict(labels)}")
    
    # Latency stats
    lats = [float(r['latency_ms']) for r in rows if r.get('latency_ms')]
    if lats:
        lats.sort()
        p50 = lats[len(lats)//2]
        p95 = lats[int(len(lats)*0.95)]
        print(f"\nLatency: mean={sum(lats)/len(lats):.0f}ms, P50={p50:.0f}ms, P95={p95:.0f}ms")
else:
    print("results.csv not found")

# ── 3. Summary.json ───────────────────────────────────────────────────
print("\n" + "=" * 60)
print("SUMMARY.JSON")
print("=" * 60)
summary_path = os.path.join(BASE, 'simulation_tests/reports/summary.json')
if os.path.exists(summary_path):
    with open(summary_path) as f:
        s = json.load(f)
    print(json.dumps(s, indent=2, default=str))
else:
    print("summary.json not found")
