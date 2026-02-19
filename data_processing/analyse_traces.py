import pandas as pd
import argparse
import os
import re

def parse_details(details_str):
    if not isinstance(details_str, str): return None, None, None
    
    # 1. Extract Score
    m_score = re.search(r"Score[:=]\s*\[?([\d\.]+)\]?", details_str)
    score = float(m_score.group(1)) if m_score else None
    
    # 2. Extract Distance
    m_dist = re.search(r"Dist[:=]\s*\[?([\d\.]+)", details_str)
    dist = float(m_dist.group(1)) if m_dist else None
    
    # 3. Extract Target Node Name
    m_target = re.search(r"Selected\s+([A-Za-z0-9_]+)", details_str)
    target = m_target.group(1) if m_target else None
    
    return score, dist, target

def analyze_traces(run_id, base_dir="output"):
    run_path = os.path.join(base_dir, run_id)
    
    # 1. Load Ground Truth
    gt_path = os.path.join(run_path, "ground_truth.csv")
    if not os.path.exists(gt_path):
        print(f"ERROR: Ground truth file not found at {gt_path}")
        return
    gt_df = pd.read_csv(gt_path, header=0)
    gt_df['RequestID'] = gt_df['RequestID'].astype(str)
    
    # 2. Load Simulation Logs
    pub_dir = os.path.join(run_path, "publications")
    if not os.path.exists(pub_dir):
        print(f"ERROR: Publications directory not found at {pub_dir}")
        return
        
    # Ignore summary files to prevent warnings
    sim_files = [os.path.join(pub_dir, f) for f in os.listdir(pub_dir) if f.endswith('.csv') and 'summary' not in f]
    
    sim_data = []
    for f in sim_files:
        df = pd.read_csv(f, header=0, skipinitialspace=True)
        # Filter only relevant routing events using the 'Result' column from CsvMetricWriter.java
        if 'Result' in df.columns:
            # Strip extra whitespace/quotes just in case
            df['Result'] = df['Result'].astype(str).str.strip(' \t\n\r"')
            relevant = df[df['Result'].isin(['FORWARDED', 'DROP_STRATEGY_REJECT'])]
            sim_data.append(relevant)
        else:
            print(f"Warning: 'Result' column not found in {f}")
        
    if not sim_data:
        print("ERROR: No valid simulation logs found.")
        return
        
    sim_df = pd.concat(sim_data, ignore_index=True)
    
    # Map 'TraceID' column: Split "0:3" to just "3" to match Ground Truth RequestID
    sim_df['TraceID'] = sim_df['TraceID'].apply(lambda x: str(x).split(':')[-1] if ':' in str(x) else str(x))
    
    # Sort by TraceID and MsgCount to get the final routing decision per request (deepest node in tree)
    sim_df = sim_df.sort_values(by=['TraceID', 'MsgCount'], ascending=[True, False]).drop_duplicates(subset=['TraceID'], keep='first')
    
    # 3. Merge Ground Truth with Simulation Data
    merged_df = pd.merge(sim_df, gt_df, left_on='TraceID', right_on='RequestID', how='inner')
    
    report_path = os.path.join(run_path, "unified_comparison_report.txt")
    
    with open(report_path, "w") as f:
        header = f"{'ID':<6} | {'Result':<8} | {'GT Provider':<30} | {'Sim Provider':<30} | {'GT Score':<8} | {'Sim Score':<9} | {'U-Stretch':<10} | {'GT Dist':<10} | {'Sim Dist':<10} | {'Dist Gap':<10} | {'Notes'}"
        f.write(header + "\n")
        f.write("-" * 175 + "\n")
        
        total_forwards = 0
        total_drops = 0
        exact_provider_matches = 0
        perfect_dist_matches = 0
        total_utility_stretch = 0.0
        total_dist_gap = 0.0
        
        for _, row in merged_df.iterrows():
            req_id = row['TraceID']
            result = row['Result']
            details = row['DecisionDetails']
            
            gt_provider = row['OptimalProvider']
            gt_score = float(row['Score'])
            gt_dist = float(row['ExactDistance'])
            
            s_score, s_dist, s_target = parse_details(details)
            
            stretch_str = ""
            dist_gap_str = ""
            notes = ""
            
            sim_provider_str = s_target if s_target else "None"
            sim_score_str = f"{s_score:.4f}" if s_score is not None else "N/A"
            gt_score_str = f"{gt_score:.4f}" if gt_score >= 0 else "NO_MATCH"
            
            sim_dist_str = f"{s_dist:.3f}" if s_dist is not None else "N/A"
            gt_dist_str = f"{gt_dist:.3f}" if gt_dist >= 0 else "NO_MATCH"
            
            if result == 'DROP_STRATEGY_REJECT':
                total_drops += 1
                if gt_provider == "NO_MATCH":
                    stretch_str = "0.0000"
                    dist_gap_str = "0.000"
                    notes = "Correct Drop"
                else:
                    stretch_str = "Miss"
                    dist_gap_str = "Miss"
                    notes = f"Failed to route. Best was {gt_provider}"
            else:
                total_forwards += 1
                if gt_provider == "NO_MATCH":
                    stretch_str = "False+"
                    dist_gap_str = "False+"
                    notes = "Routed to impossible provider"
                else:
                    # CALCULATE UTILITY STRETCH
                    if s_score is not None:
                        stretch = s_score - gt_score
                        total_utility_stretch += stretch
                        stretch_str = f"{stretch:+.4f}"
                    else:
                        stretch_str = "Unknown"
                        
                    # CALCULATE DIST GAP
                    if s_dist is not None and gt_dist >= 0:
                        gap = s_dist - gt_dist
                        total_dist_gap += gap
                        dist_gap_str = f"{gap:+.3f}"
                        if abs(gap) < 0.001:
                            perfect_dist_matches += 1
                    else:
                        dist_gap_str = "Unknown"
                        
                    # COMBINED DIAGNOSIS
                    if gt_provider == s_target:
                        exact_provider_matches += 1
                        notes = "Perfect Provider Match"
                    elif s_score is not None and abs(s_score - gt_score) < 0.001:
                        notes = "Equivalent Tie (Different node, same utility)"
                    else:
                        notes = "Suboptimal Route"
            
            res_str = "DROP" if result == 'DROP_STRATEGY_REJECT' else "FORWARD"
            
            line = f"{req_id:<6} | {res_str:<8} | {gt_provider:<30} | {sim_provider_str:<30} | {gt_score_str:<8} | {sim_score_str:<9} | {stretch_str:<10} | {gt_dist_str:<10} | {sim_dist_str:<10} | {dist_gap_str:<10} | {notes}\n"
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
        print(f"Report successfully generated at: {report_path}")

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
        
    analyze_traces(final_run_id, args.base_dir)