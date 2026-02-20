import pandas as pd
import argparse
import os
import re

# ==========================================
# COMMON PARSER FUNCTIONS
# ==========================================
def parse_details(details_str):
    """Extracts routing telemetry from the FaaS simulation logs."""
    if not isinstance(details_str, str): return None, None, None, None
    
    # 1. Extract Score (Handle numeric or INF)
    m_score = re.search(r"Score[:=]\s*\[?([\d\.]+|INF)\]?", details_str)
    score = None
    if m_score and m_score.group(1) != 'INF':
        score = float(m_score.group(1))
    
    # 2. Extract Distance
    m_dist = re.search(r"Dist[:=]\s*\[?([\d\.]+)", details_str)
    dist = float(m_dist.group(1)) if m_dist else None
    
    # 3. Extract Target Node Name
    m_target = re.search(r"Selected\s+(.*?)\s+\(Score", details_str)
    target = m_target.group(1) if m_target else None
    
    if not target:
        m_attempt = re.search(r"BestAttempt=\[(.*?)\]", details_str)
        target = m_attempt.group(1) if m_attempt else None
        
    # 4. Extract Drop Reason (if present)
    m_reason = re.search(r"Reason=\[(.*?)\]", details_str)
    reason = m_reason.group(1) if m_reason else None
    
    if not reason and "No Spatial/Content Match" in details_str:
        reason = "No Spatial/Content Match"
        
    return score, dist, target, reason

def format_state(state):
    """Helper to cleanly align the massive MetricHyperCube strings."""
    if pd.isna(state) or str(state).strip() == "None":
        return "None"
    state_str = str(state).strip()
    if " | QoS[" in state_str:
        parts = state_str.split(" | QoS[")
        # Ensure the spatial bounds align nicely before printing the QoS bounds
        return f"{parts[0]:<36} | QoS[{parts[1]}"
    return state_str


# ==========================================
# SUBSCRIPTION LIFECYCLE (CONTROL PLANE)
# ==========================================
def analyze_subscriptions(run_id, base_dir="output"):
    run_path = os.path.join(base_dir, run_id)
    sub_dir = os.path.join(run_path, "subscriptions")
    
    if not os.path.exists(sub_dir):
        print(f"Skipping Subscriptions: Directory not found at {sub_dir}")
        return
        
    sub_files = [os.path.join(sub_dir, f) for f in os.listdir(sub_dir) if f.endswith('.csv') and 'summary' not in f]
    if not sub_files: 
        print("No subscription CSVs found.")
        return
    
    print("Analyzing Subscription Lifecycles & Broker State Evolutions...")
    
    # FIX 1: Prevent "NA" from becoming NaN in Subscriptions
    df_list = [pd.read_csv(f, header=0, skipinitialspace=True, keep_default_na=False) for f in sub_files]
    df = pd.concat(df_list, ignore_index=True)
    
    # Clean up column names in case of whitespace
    df.columns = [c.strip() for c in df.columns]
    
    # Preserve the original CSV row index to ensure chronological processing for Brokers
    df['GlobalOrder'] = df.index
    
    # ---------------------------------------------------------
    # REPORT 1: TRACE-CENTRIC (The Journey of the Offer)
    # ---------------------------------------------------------
    df_trace = df.sort_values(by=['TraceID', 'Seq'], ascending=[True, True])
    trace_report_path = os.path.join(run_path, "subscription_trace_lifecycle.txt")
    
    with open(trace_report_path, "w") as f:
        f.write("=== FAAS MARKETPLACE: SUBSCRIPTION TRACE LIFECYCLE ===\n")
        f.write("This report tracks how physical FaaS node capabilities bubble up the continuum,\n")
        f.write("showing exactly how the topological states merged hop-by-hop.\n")
        f.write("="*100 + "\n\n")
        
        grouped = df_trace.groupby('TraceID')
        for trace_id, group in grouped:
            f.write(f"Trace {trace_id}:\n")
            
            for _, row in group.iterrows():
                seq = row['Seq']
                node = row['ProcessingNode']
                result = row['Result'].strip()
                
                inc = format_state(row.get('IncomingState', 'None'))
                exs = format_state(row.get('ExistingState', 'None'))
                res = format_state(row.get('ResultingState', 'None'))
                
                if result == "SENT":
                    f.write(f"  [{seq}] {node:<22} : {result:<8} -> Base: {inc}\n")
                else:
                    action_tag = f"[{result}]"
                    f.write(f"  [{seq}] {node:<22} : {action_tag}\n")
                    f.write(f"         In  : {inc}\n")
                    if exs != "None":
                        f.write(f"         Hit : {exs}\n")
                    f.write(f"         Out : {res}\n")
            f.write("-" * 100 + "\n")
            
    print(f"-> Trace-Centric Report generated at:  {trace_report_path}")

    # ---------------------------------------------------------
    # REPORT 2: BROKER-CENTRIC (The Evolution of the Routing Table)
    # ---------------------------------------------------------
    # Filter out SENT events as they represent provider hardware origins, not Broker routing states
    df_broker = df[df['Result'] != 'SENT'].sort_values(by=['GlobalOrder'])
    broker_report_path = os.path.join(run_path, "broker_routing_evolution_report.txt")
    
    with open(broker_report_path, "w") as f:
        f.write("=== FAAS MARKETPLACE: BROKER ROUTING TABLE EVOLUTION ===\n")
        f.write("This report isolates each broker to reconstruct the chronological sequence\n")
        f.write("of ADDED and EXPANDED mathematical operations applied to its routing table.\n")
        f.write("="*100 + "\n\n")
        
        # Sort broker names alphabetically for easier lookup
        for broker_node, group in df_broker.groupby('ProcessingNode'):
            f.write(f"Broker: {broker_node}\n")
            f.write(f"Total Advertisements Processed: {len(group)}\n\n")
            
            event_counter = 1
            for _, row in group.iterrows():
                result = row['Result'].strip()
                inc = format_state(row.get('IncomingState', 'None'))
                exs = format_state(row.get('ExistingState', 'None'))
                res = format_state(row.get('ResultingState', 'None'))
                
                f.write(f"  Event {event_counter} (from Trace {row['TraceID']} - Sent by: {row['ReceivedFrom']})\n")
                f.write(f"    Action   : {result}\n")
                f.write(f"    Incoming : {inc}\n")
                f.write(f"    Existing : {exs}\n")
                f.write(f"    Resulting: {res}\n\n")
                
                event_counter += 1
            f.write("-" * 100 + "\n\n")
            
    print(f"-> Broker-Centric Report generated at: {broker_report_path}")


# ==========================================
# PUBLICATION ROUTING (DATA PLANE)
# ==========================================
def analyze_publications(run_id, base_dir="output"):
    run_path = os.path.join(base_dir, run_id)
    
    # 1. Load Ground Truth
    gt_path = os.path.join(run_path, "ground_truth.csv")
    if not os.path.exists(gt_path):
        print(f"Skipping Publications: Ground truth file not found at {gt_path}")
        return
        
    # FIX 2: Prevent "NA" from becoming NaN in Ground Truth Targets/Sources
    gt_df = pd.read_csv(gt_path, header=0, keep_default_na=False)
    gt_df['RequestID'] = gt_df['RequestID'].astype(str)
    
    # 2. Load Simulation Logs
    pub_dir = os.path.join(run_path, "publications")
    if not os.path.exists(pub_dir):
        print(f"ERROR: Publications directory not found at {pub_dir}")
        return
        
    sim_files = [os.path.join(pub_dir, f) for f in os.listdir(pub_dir) if f.endswith('.csv') and 'summary' not in f]
    
    sim_data = []
    for f in sim_files:
        # FIX 3: Prevent "NA" from becoming NaN in Publication ProcessingNodes/Results
        df = pd.read_csv(f, header=0, skipinitialspace=True, keep_default_na=False)
        if 'Result' in df.columns:
            df['Result'] = df['Result'].astype(str).str.strip(' \t\n\r"')
            relevant = df[(df['Result'] == 'FORWARDED') | (df['Result'].str.startswith('DROP'))]
            sim_data.append(relevant)
        else:
            print(f"Warning: 'Result' column not found in {f}")
        
    if not sim_data:
        print("ERROR: No valid simulation logs found.")
        return
        
    sim_df = pd.concat(sim_data, ignore_index=True)
    
    # Map 'TraceID' column: Split "0:3" to just "3"
    sim_df['TraceID'] = sim_df['TraceID'].apply(lambda x: str(x).split(':')[-1] if ':' in str(x) else str(x))
    
    # Sort by TraceID and MsgCount to get the final routing decision
    sim_df = sim_df.sort_values(by=['TraceID', 'MsgCount'], ascending=[True, False]).drop_duplicates(subset=['TraceID'], keep='first')
    
    # 3. Merge Ground Truth with Simulation Data
    merged_df = pd.merge(sim_df, gt_df, left_on='TraceID', right_on='RequestID', how='inner')
    
    report_path = os.path.join(run_path, "publication_routing_report.txt")
    
    with open(report_path, "w") as f:
        f.write("=== FAAS MARKETPLACE: PUBLICATION ROUTING (WORKLOAD ORCHESTRATION) ===\n")
        f.write("This report evaluates how accurately the DNS++ pub/sub tree routes client\n")
        f.write("ServiceRequests (Publications) compared to the omniscient Ground Truth.\n")
        f.write("=" * 175 + "\n\n")
        
        header = f"{'ID':<6} | {'Result':<8} | {'GT Provider':<30} | {'Sim Provider':<30} | {'GT Score':<8} | {'Sim Score':<9} | {'U-Stretch':<10} | {'GT Dist':<10} | {'Sim Dist':<10} | {'Dist Gap':<10} | {'Notes'}"
        f.write(header + "\n")
        f.write("-" * 175 + "\n")
        
        total_forwards, total_drops = 0, 0
        exact_provider_matches, perfect_dist_matches = 0, 0
        total_utility_stretch, total_dist_gap = 0.0, 0.0
        
        for _, row in merged_df.iterrows():
            req_id = row['TraceID']
            result = row['Result']
            details = row['DecisionDetails']
            
            raw_gt_provider = str(row['OptimalProvider'])
            # Since keep_default_na=False turns everything into strings, safely extract floats:
            gt_score_raw = row['Score']
            gt_score = float(gt_score_raw) if gt_score_raw != '' else -1.0
            
            gt_dist_raw = row['ExactDistance']
            gt_dist = float(gt_dist_raw) if gt_dist_raw != '' else -1.0
            
            s_score, s_dist, s_target, s_reason = parse_details(details)
            
            stretch_str, dist_gap_str, notes = "", "", ""
            
            is_gt_no_match = "NO_MATCH" in raw_gt_provider
            display_gt_provider = "NO_MATCH" if is_gt_no_match else raw_gt_provider
            sim_provider_str = s_target if s_target else "None"
            
            if s_score is not None: sim_score_str = f"{s_score:.4f}"
            elif "INF" in str(details): sim_score_str = "INF"
            else: sim_score_str = "N/A"
                
            gt_score_str = f"{gt_score:.4f}" if gt_score >= 0 else "NO_MATCH"
            sim_dist_str = f"{s_dist:.3f}" if s_dist is not None else "N/A"
            gt_dist_str = f"{gt_dist:.3f}" if gt_dist >= 0 else "NO_MATCH"
            
            sim_reason_str = f" | Sim Reason: {s_reason}" if s_reason else ""
            
            if result.startswith('DROP'):
                total_drops += 1
                if is_gt_no_match:
                    stretch_str, dist_gap_str = "0.0000", "0.000"
                    notes = f"Correct Drop. GT Reason: {raw_gt_provider}{sim_reason_str}"
                else:
                    stretch_str, dist_gap_str = "Miss", "Miss"
                    notes = f"Failed to route. Best was {display_gt_provider}{sim_reason_str}"
            else:
                total_forwards += 1
                if is_gt_no_match:
                    stretch_str, dist_gap_str = "False+", "False+"
                    notes = f"Routed to impossible provider. GT Reason: {raw_gt_provider}{sim_reason_str}"
                else:
                    if s_score is not None:
                        stretch = s_score - gt_score
                        total_utility_stretch += stretch
                        stretch_str = f"{stretch:+.4f}"
                    else: stretch_str = "Unknown"
                        
                    if s_dist is not None and gt_dist >= 0:
                        gap = s_dist - gt_dist
                        total_dist_gap += gap
                        dist_gap_str = f"{gap:+.3f}"
                        if abs(gap) < 0.001: perfect_dist_matches += 1
                    else: dist_gap_str = "Unknown"
                        
                    if display_gt_provider == s_target:
                        exact_provider_matches += 1
                        notes = "Perfect Provider Match"
                    elif s_score is not None and abs(s_score - gt_score) < 0.001:
                        notes = "Equivalent Tie (Different node, same utility)"
                    else:
                        notes = "Suboptimal Route"
            
            res_str = "DROP" if result.startswith('DROP') else "FORWARD"
            line = f"{req_id:<6} | {res_str:<8} | {display_gt_provider:<30} | {sim_provider_str:<30} | {gt_score_str:<8} | {sim_score_str:<9} | {stretch_str:<10} | {gt_dist_str:<10} | {sim_dist_str:<10} | {dist_gap_str:<10} | {notes}\n"
            f.write(line)
            
        summary = "\n" + "="*70 + "\n"
        summary += "=== MULTI-OBJECTIVE ORCHESTRATION SUMMARY ===\n"
        summary += f"Total Valid Forwards:    {total_forwards}\n"
        summary += f"Total Drops:             {total_drops}\n"
        
        if total_forwards > 0:
            summary += f"\n--- Accuracy Metrics ---\n"
            summary += f"Exact Provider Matches:  {exact_provider_matches} ({(exact_provider_matches/total_forwards)*100:.1f}% Accuracy)\n"
            summary += f"Perfect Dist Matches:    {perfect_dist_matches} (Gap ~ 0.000)\n"
            
            summary += f"\n--- Degradation (Stretch/Gap) ---\n"
            summary += f"Average Utility Stretch: +{(total_utility_stretch/total_forwards):.4f} Penalty points\n"
            summary += f"Average Distance Gap:    +{(total_dist_gap/total_forwards):.3f} Distance units\n"
        
        summary += "="*70 + "\n"
        
        f.write(summary)
        print(summary)
        print(f"-> Publication Report generated at:    {report_path}")

# ==========================================
# COMMAND LINE EXECUTION BLOCK
# ==========================================
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Analyze FaaS Marketplace Traces")
    parser.add_argument("run_id_pos", type=str, nargs='?', default=None, help="The name of the run folder")
    parser.add_argument("--run_id", type=str, help="The name of the run folder (e.g., --run_id 123)")
    parser.add_argument("--base_dir", type=str, default="output", help="The base output directory")
    
    args = parser.parse_args()
    
    final_run_id = args.run_id if args.run_id else args.run_id_pos
    if not final_run_id:
        print("ERROR: Please specify a run folder (e.g., --run_id 1771484116589)")
        exit(1)
        
    print(f"Starting Analysis for Run ID: {final_run_id}")
    
    # 1. Run the Control Plane (Subscription) Analysis
    analyze_subscriptions(final_run_id, args.base_dir)
    print("-" * 40)
    
    # 2. Run the Data Plane (Publication) Analysis
    analyze_publications(final_run_id, args.base_dir)
    print("Analysis Complete.")