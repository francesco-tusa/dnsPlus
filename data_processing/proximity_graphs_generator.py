import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import os
import argparse
import sys
import numpy as np

# ================= CONFIGURATION =================
INPUT_FILENAME = 'results_closest.csv'

# Professional Paper Styling
sns.set_theme(style="whitegrid")
plt.rcParams.update({
    'font.size': 14, 
    'font.family': 'sans-serif',
    'axes.titlesize': 16,
    'axes.labelsize': 14,
    'legend.fontsize': 12,
    'xtick.labelsize': 12,
    'ytick.labelsize': 12,
    'lines.linewidth': 3,
    'lines.markersize': 9
})
PALETTE = "viridis" 
# =================================================

def setup_paths(batch_id):
    script_dir = os.path.dirname(os.path.abspath(__file__))
    base_dir = script_dir
    
    # Robust file finding
    possible_paths = [
        os.path.join(base_dir, '..', 'output', f"batch_{batch_id}", INPUT_FILENAME),
        os.path.join(base_dir, INPUT_FILENAME),
        INPUT_FILENAME
    ]
    input_path = next((p for p in possible_paths if os.path.exists(p)), None)
    
    if not input_path:
        print(f"Error: Could not find {INPUT_FILENAME}. Checked: {possible_paths}")
        sys.exit(1)
        
    output_dir = os.path.join(base_dir, 'graphs', f"batch_{batch_id}", 'paper_enhanced')
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
        
    return input_path, output_dir

def load_data(filepath):
    df = pd.read_csv(filepath)
    # Map -1 to "Unbounded"
    df['Brake_Label'] = df['brake_limit'].apply(lambda x: "Unbounded" if x == -1 else str(int(x)))
    # Sort key: 1, 2, 4... then 999 (Unbounded)
    df['brake_sort'] = df['brake_limit'].apply(lambda x: 999 if x == -1 else x)
    df = df.sort_values(['brake_sort'])
    return df

def get_best_tradeoff_scale(df):
    """
    Finds a scale (Pubs, Subs) that has MULTIPLE brake limits available.
    This ensures we don't plot a graph with just one dot.
    """
    # Count unique brake limits per scale
    counts = df.groupby(['publishers', 'subscribers'])['brake_limit'].nunique().reset_index()
    
    # Filter for scales with at least 2 different brake settings
    valid = counts[counts['brake_limit'] >= 2].sort_values(['subscribers', 'publishers'], ascending=False)
    
    if valid.empty:
        # Fallback: Just take the largest available
        print("Warning: No scale found with multiple brake limits. Graphs may be sparse.")
        best = df.sort_values(['subscribers', 'publishers'], ascending=False).iloc[0]
        return best['publishers'], best['subscribers']
        
    best_row = valid.iloc[0]
    return best_row['publishers'], best_row['subscribers']

# ==============================================================================
# GROUP A: SCALABILITY (X-Axis = Subscribers)
# ==============================================================================

def plot_scalability_metrics(df, output_dir):
    """Generates the main scalability suite."""
    print("Generating Scalability Suite...")
    
    # Use the largest publisher set for these lines
    max_pubs = df['publishers'].max()
    subset = df[df['publishers'] == max_pubs]
    
    if subset.empty:
        print("Error: No data found for max publishers.")
        return

    # Define the 5 Key Graphs
    metrics = [
        # (Column, Label, Filename, YScale, Title)
        ('avg_stretch', 'Average Stretch (0 is Optimal)', 'Fig1_Scalability_Stretch.pdf', 'linear', 'Routing Quality'),
        ('matching_cost', 'Matching Cost (Ops)', 'Fig2_Scalability_Cost.pdf', 'log', 'Computational Overhead'),
        ('traffic_ratio', 'Traffic Ratio (Traffic/Deliveries)', 'Fig3_Scalability_Traffic.pdf', 'log', 'Network Efficiency'),
        ('recall', 'Recall (Success Rate)', 'Fig4_Scalability_Reliability.pdf', 'linear', 'System Reliability'),
        ('table_size_avg', 'Avg Table Size (Entries)', 'Fig5_Scalability_State.pdf', 'log', 'Memory State')
    ]

    for y_col, y_label, fname, y_scale, title in metrics:
        plt.figure(figsize=(10, 6))
        
        # Use lineplot for all (even Reliability) to show trends
        sns.lineplot(
            data=subset, x='subscribers', y=y_col, 
            hue='Brake_Label', style='Brake_Label', 
            markers=True, dashes=False, palette=PALETTE
        )
        
        plt.xscale('log')
        plt.yscale(y_scale)
        plt.xlabel("Number of Subscribers (Log Scale)", fontweight='bold')
        plt.ylabel(y_label, fontweight='bold')
        plt.title(f"Scalability: {title}\n(Publishers: {int(max_pubs):,})")
        plt.legend(title="Brake Limit", bbox_to_anchor=(1.02, 1), loc='upper left')
        
        plt.tight_layout()
        plt.savefig(os.path.join(output_dir, fname))
        plt.close()

# ==============================================================================
# GROUP B: TRADE-OFF (X-Axis = Cost or Brake)
# ==============================================================================

def plot_tradeoff_metrics(df, output_dir):
    """Generates the efficiency trade-off graphs."""
    print("Generating Trade-off Suite...")
    
    # Find a scale where we can actually compare things
    t_pubs, t_subs = get_best_tradeoff_scale(df)
    print(f" -> Using Scale: Pubs={t_pubs}, Subs={t_subs}")
    
    subset = df[(df['publishers'] == t_pubs) & (df['subscribers'] == t_subs)].copy()
    
    # --- FIG 6: The Sweet Spot (Recall vs Cost) ---
    fig, ax1 = plt.subplots(figsize=(10, 6))
    
    color1 = 'tab:blue'
    ax1.set_xlabel('Brake Limit Configuration', fontweight='bold')
    ax1.set_ylabel('Recall (Higher is Better)', color=color1, fontweight='bold')
    sns.lineplot(data=subset, x='Brake_Label', y='recall', marker='o', ax=ax1, color=color1, sort=False)
    ax1.tick_params(axis='y', labelcolor=color1)
    ax1.set_ylim(0, 1.1)
    ax1.grid(True, alpha=0.3)

    ax2 = ax1.twinx()
    color2 = 'tab:red'
    ax2.set_ylabel('Matching Cost (Lower is Better)', color=color2, fontweight='bold')
    sns.lineplot(data=subset, x='Brake_Label', y='matching_cost', marker='s', ax=ax2, color=color2, linestyle='--', sort=False)
    ax2.tick_params(axis='y', labelcolor=color2)
    ax2.set_yscale('log')

    plt.title(f"Efficiency Trade-off (Sweet Spot)\nScale: {int(t_subs):,} Subs")
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, "Fig6_Tradeoff_SweetSpot.pdf"))
    plt.close()

    # --- FIG 7: Cost of Perfection (Pareto) ---
    # This solves the "One Dot" issue by ensuring we have the subset
    plt.figure(figsize=(9, 6))
    
    # Only plot points where recall is decent (>10%) to avoid noise
    valid_subset = subset[subset['recall'] > 0.1]
    
    if len(valid_subset) < 2:
        print("Warning: Not enough valid data points for Pareto chart. Plotting what we have.")
    
    sns.scatterplot(
        data=valid_subset, x='matching_cost', y='avg_stretch', 
        hue='Brake_Label', style='Brake_Label', 
        palette='viridis', s=200, zorder=5
    )
    
    # Draw a connecting line to show the trend
    sns.lineplot(
        data=valid_subset, x='matching_cost', y='avg_stretch',
        color='grey', alpha=0.5, sort=False, legend=False, zorder=1
    )

    plt.xscale('log')
    plt.xlabel("Overhead (Matching Cost) [Log Scale]", fontweight='bold')
    plt.ylabel("Inaccuracy (Avg Stretch)", fontweight='bold')
    plt.title(f"The Cost of Perfection\n(Low Stretch requires High Cost)")
    plt.grid(True, which="both", ls="-", alpha=0.2)
    plt.legend(title="Brake Limit", bbox_to_anchor=(1.02, 1), loc='upper left')
    
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, "Fig7_Tradeoff_Pareto.pdf"))
    plt.close()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("batch_id", help="Batch ID")
    args = parser.parse_args()

    input_path, output_dir = setup_paths(args.batch_id)
    print(f"Reading: {input_path}")
    print(f"Output:  {output_dir}")
    
    df = load_data(input_path)
    
    plot_scalability_metrics(df, output_dir)
    plot_tradeoff_metrics(df, output_dir)
    
    print("\nProcessing Complete. 7 Figures generated.")

if __name__ == "__main__":
    main()