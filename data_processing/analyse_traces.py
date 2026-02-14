import pandas as pd
import argparse
import os
import re

def parse_details(details_str):
    if not isinstance(details_str, str): return None, None
    # Matches Score: 0.35 or Score=[0.35]
    m_score = re.search(r"Score[:=]\s*\[?([\d\.]+)\]?", details_str)
    score = float(m_score.group(1)) if m_score else None
    # Matches Dist: 45.0 or Dist=[45.0]
    m_dist = re.search(r"Dist[:=]\s*\[?([\d\.]+)", details_str)
    dist = float(m_dist.group(1)) if m_dist else None
    return score, dist

def analyze_traces(run_id, base_dir="output"):
    run_path = os.path.join(base_dir, run_id)
    
    # 1. Load Ground Truth
    gt_path = os.path.join(run_path, "ground_truth.csv")
    if not os.path.exists(gt_path):
        print(f"File not found: {gt_path}")
        return
    gt_df = pd.read_csv(gt_path, header=0)
    
    # Force ID to string for merging
    gt_df['RequestID'] = gt_df['RequestID'].astype(str)
    
    # 2. Load Simulation Logs
    pub_dir = os.path.join(run_path, "publications")
    sim_files = [os.path.join(pub_dir, f) for f in os.listdir(pub_dir) if f.endswith(".csv")]
    if not sim_files:
        print("No simulation logs found.")
        return
    
    sim_df = pd.concat([pd.read_csv(f) for f in sim_files])

    # Normalize and force ID to string
    sim_df['TraceID'] = sim_df['TraceID'].apply(
        lambda x: str(x).split(":")[-1] if ":" in str(x) else str(x)
    )
    
    # 3. Analyze ALL Outcomes (Deliveries & Drops)
    outcomes = sim_df[sim_df['Result'].isin(['FORWARDED', 'DROP_STRATEGY_REJECT'])].copy()
    
    # Perform the merge on matching types
    merged = pd.merge(gt_df, outcomes, left_on="RequestID", right_on="TraceID", how="inner")
    
    if merged.empty:
        print("\nNo overlapping TraceIDs found between Ground Truth and Outcomes.")
        return

    # Sort by ID to make it easier to read
    merged['ID_int'] = merged['RequestID'].astype(int)
    merged = merged.sort_values('ID_int')

    # 4. Generate Output Report
    report_path = os.path.join(run_path, "comparison_report.txt")
    
    with open(report_path, "w") as f:
        header = f"{'ID':<6} | {'Result':<10} | {'GT Dist':<10} | {'Sim Dist':<10} | {'Gap':<12} | {'Notes'}\n"
        header += "-" * 110 + "\n"
        f.write(header)
        
        processed_dedup = set()
        total_forwards = 0
        total_drops = 0
        perfect_matches = 0
        
        for _, row in merged.iterrows():
            req_id = row['RequestID']
            result = row['Result']
            
            _, s_dist = parse_details(row['DecisionDetails'])
            g_dist = row['ExactDistance']
            
            # Deduplication: Avoid printing multi-hop forwards pointing to the exact same target distance
            dedup_key = f"{req_id}_{result}_{s_dist}"
            if dedup_key in processed_dedup:
                continue
            processed_dedup.add(dedup_key)
            
            # Handle Notes Extraction
            if result == 'FORWARDED':
                total_forwards += 1
                # Extract the provider name if available
                prov_match = re.search(r"Selected\s([^\s\(]+)", str(row['DecisionDetails']))
                notes = f"Target: {prov_match.group(1)}" if prov_match else "Delivered successfully"
            else:
                total_drops += 1
                r_match = re.search(r"Reason=\[([^\]]+)\]", str(row['DecisionDetails']))
                notes = "Reject: " + (r_match.group(1) if r_match else "Unknown constraint")

            # Distance Logic
            if g_dist == -1.0:
                gt_str = "NO_MATCH"
                if result == 'DROP_STRATEGY_REJECT':
                    gap_str = "N/A (Correct)"
                else:
                    gap_str = "N/A (False+)"
            else:
                gt_str = f"{g_dist:<10.3f}"
                if s_dist is not None:
                    gap = s_dist - g_dist
                    gap_str = f"{gap:<+12.6f}"
                    # Check for perfect match (epsilon tolerance for floating point)
                    if abs(gap) < 0.001 and result == 'FORWARDED':
                        perfect_matches += 1
                else:
                    gap_str = "Unknown"
            
            sim_str = f"{s_dist:<10.3f}" if s_dist is not None else "None"
            res_str = "DROP" if result == 'DROP_STRATEGY_REJECT' else "FORWARD"
            
            line = f"{req_id:<6} | {res_str:<10} | {gt_str:<10} | {sim_str:<10} | {gap_str:<12} | {notes}\n"
            f.write(line)
            
        summary = "\n" + "="*50 + "\n"
        summary += f"Total Forward Events Analyzed: {total_forwards}\n"
        summary += f"Total Drop Events Analyzed:    {total_drops}\n"
        summary += f"Perfect Distance Matches:      {perfect_matches} (Gap ~ 0.000)\n"
        summary += "="*50 + "\n"
        
        f.write(summary)
        
        print(f"Report successfully generated at: {report_path}")
        print(summary)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--run_id", required=True, help="Timestamp folder name in output/")
    args = parser.parse_args()
    analyze_traces(args.run_id)