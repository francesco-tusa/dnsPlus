import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import os
import argparse
import sys

# ================= CONFIGURATION =================

# Input Filename (Fixed based on your specification)
INPUT_FILENAME = 'results_regional.csv'

# Metric Mapping: CSV Header -> Friendly Graph Title
METRIC_MAPPING = {
    'table_size_avg': 'Average Table Size',
    'pub_events': 'Total Publication Events',
    'avg_comparisons': 'Average Comparisons',
    'sub_updates_sent': 'Subscriber Updates Sent',
    'traffic_ratio': 'Traffic Ratio',
    'traffic_saved': 'Traffic Saved'
}

# The specific knob for Regional experiments
KNOB_COLUMN = 'fpr_threshold'
X_AXIS_COLUMN = 'subscribers'
GROUP_COLUMN = 'publishers'

# =================================================

def setup_paths(batch_id):
    """Calculates input/output paths based on the batch_id."""
    
    # Get the directory where this script is located (data_processing)
    script_dir = os.path.dirname(os.path.abspath(__file__))
    
    # 1. Locate Input CSV
    # Logic: ../output/batch_{id}/results_regional.csv
    batch_folder_name = f"batch_{batch_id}"
    input_path = os.path.join(script_dir, '..', 'output', batch_folder_name, INPUT_FILENAME)
    
    # 2. Define Output Directory
    # Logic: ./graphs/batch_{id}/
    output_root = os.path.join(script_dir, 'graphs', batch_folder_name)
    
    return input_path, output_root

def generate_graphs(batch_id):
    input_path, output_root = setup_paths(batch_id)

    # --- Validation ---
    if not os.path.exists(input_path):
        print(f"CRITICAL ERROR: Input file not found.")
        print(f"Looking for: {input_path}")
        print(f"Ensure that 'output/batch_{batch_id}/{INPUT_FILENAME}' exists.")
        sys.exit(1)

    print(f"Processing Batch ID: {batch_id}")
    print(f"Reading: {input_path}")
    print(f"Writing to: {output_root}")

    # --- Load Data ---
    try:
        df = pd.read_csv(input_path)
        df.columns = df.columns.str.strip() # Clean headers
    except Exception as e:
        print(f"Error reading CSV: {e}")
        sys.exit(1)

    # --- Check Columns ---
    required_cols = [GROUP_COLUMN, X_AXIS_COLUMN, KNOB_COLUMN] + list(METRIC_MAPPING.keys())
    missing = [c for c in required_cols if c not in df.columns]
    if missing:
        print(f"Error: The CSV is missing required columns: {missing}")
        sys.exit(1)

    # --- Plotting Setup ---
    # Use 'paper' context for clean, readable PDF fonts
    sns.set_theme(style="whitegrid", context="paper", font_scale=1.4)
    
    publisher_counts = sorted(df[GROUP_COLUMN].unique())

    # --- Generation Loop ---
    for metric, friendly_title in METRIC_MAPPING.items():
        # Create subfolder for this metric
        metric_dir = os.path.join(output_root, metric)
        os.makedirs(metric_dir, exist_ok=True)
        
        print(f" -> Generating '{metric}' graphs...")

        for pub_count in publisher_counts:
            # 1. Filter Data
            subset = df[df[GROUP_COLUMN] == pub_count].copy()
            if subset.empty: continue
            
            # 2. Sort
            subset.sort_values(by=[X_AXIS_COLUMN, KNOB_COLUMN], inplace=True)

            # 3. Plot
            plt.figure(figsize=(7, 5)) 
            
            ax = sns.lineplot(
                data=subset,
                x=X_AXIS_COLUMN,
                y=metric,
                hue=KNOB_COLUMN,
                style=KNOB_COLUMN,
                markers=True,
                dashes=False,
                palette="viridis",
                linewidth=2.5,
                markersize=8
            )

            # 4. Formatting
            plt.title(f"{friendly_title}\n(Publishers: {pub_count})", fontsize=14, weight='bold')
            plt.xlabel("Number of Subscribers", fontsize=12)
            plt.ylabel(friendly_title, fontsize=12)
            plt.xscale('log') # Log scale for subscribers
            
            # Improve Legend (Outside Top Right)
            plt.legend(title="FPR Threshold", bbox_to_anchor=(1.05, 1), loc='upper left')
            
            # 5. Save
            filename = f"pubs_{pub_count}.pdf"
            filepath = os.path.join(metric_dir, filename)
            
            # bbox_inches='tight' prevents the legend from being cut off
            plt.savefig(filepath, format='pdf', bbox_inches='tight')
            plt.close()

    print(f"\nSuccess! Graphs stored in: {output_root}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate PDF graphs from Regional DNS++ Batch Runs.")
    # We expect just the ID, e.g., "1" or "01"
    parser.add_argument("batch_id", help="The numeric ID of the batch (e.g., '1' for batch_1)")
    
    args = parser.parse_args()
    
    generate_graphs(args.batch_id)
