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
    """Robust path finding."""
    script_dir = os.path.dirname(os.path.abspath(__file__))
    base_dir = script_dir
    
    possible_paths = [
        os.path.join(base_dir, '..', 'output', f"batch_{batch_id}", INPUT_FILENAME),
        os.path.join(base_dir, INPUT_FILENAME),
        INPUT_FILENAME
    ]
    input_path = next((p for p in possible_paths if os.path.exists(p)), None)
    
    if not input_path:
        print(f"Error: Could not find {INPUT_FILENAME}")
        sys.exit(1)
        
    output_dir = os.path.join(base_dir, 'graphs', f"batch_{batch_id}", 'paper_complete_suite')
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
        
    return input_path, output_dir

def load_data(filepath):
    df = pd.read_csv(filepath)
    
    # 1. Format Brake Labels (-1 -> "Unbounded")
    df['Brake_Label'] = df['brake_limit'].apply(lambda x: "Unbounded" if x == -1 else str(int(x)))
    df['brake_sort'] = df['brake_limit'].apply(lambda x: 999 if x == -1 else x)
    
    # 2. Format Human-Readable Labels (1000000 -> "1M")
    def human_format(num):
        num = float(num)
        if num >= 1_000_000: return f"{int(num/1_000_000)}M"
        if num >= 1_000: return f"{int(num/1_000)}k"
        return str(int(num))

    df['Subs_Label'] = df['subscribers'].apply(human_format)
    df['Pubs_Label'] = df['publishers'].apply(human_format)
    
    return df.sort_values(['brake_sort', 'subscribers', 'publishers'])

def get_robust_max_scale(df):
    """Finds the largest scale that actually has data for Trade-off plots."""
    counts = df.groupby(['publishers', 'subscribers'])['brake_limit'].nunique().reset_index()
    valid_scales = counts[counts['brake_limit'] >= 2].sort_values(['subscribers', 'publishers'], ascending=False)
    
    if valid_scales.empty:
        return df['publishers'].max(), df['subscribers'].max() # Fallback
        
    best_row = valid_scales.iloc[0]
    return best_row['publishers'], best_row['subscribers']

# ==============================================================================
# GROUP 1: SUBSCRIBER SCALABILITY (X-Axis = Subscribers)
# ==============================================================================

def plot_subscriber_x_graphs(df, output_dir):
    print("Generating Group 1: Subscriber Scalability (5 Graphs)...")
    
    # -- Subset 1: Max Publishers (For Stretch/Cost/Recall) --
    max_pubs = df['publishers'].max()
    subset_max_pubs = df[df['publishers'] == max_pubs]
    
    if not subset_max_pubs.empty:
        # Fig 1a: Stretch vs Subscribers
        plt.figure(figsize=(10, 6))
        sns.lineplot(data=subset_max_pubs, x='subscribers', y='avg_stretch', hue='Brake_Label', style='Brake_Label', markers=True, palette=PALETTE)
        plt.xscale('log')
        plt.xlabel("Number of Subscribers", fontweight='bold')
        plt.ylabel("Average Stretch (km)", fontweight='bold')
        plt.title(f"Scalability: Accuracy vs Subscribers\n(Fixed Pubs: {int(max_pubs):,})")
        plt.legend(title="Brake Limit")
        plt.tight_layout()
        plt.savefig(os.path.join(output_dir, "Fig1a_SubX_Stretch.pdf"))
        plt.close()

        # Fig 1b: Cost vs Subscribers
        plt.figure(figsize=(10, 6))
        sns.lineplot(data=subset_max_pubs, x='subscribers', y='matching_cost', hue='Brake_Label', style='Brake_Label', markers=True, palette='magma')
        plt.xscale('log')
        plt.yscale('log')
        plt.xlabel("Number of Subscribers", fontweight='bold')
        plt.ylabel("Matching Cost", fontweight='bold')
        plt.title(f"Scalability: Overhead vs Subscribers\n(Fixed Pubs: {int(max_pubs):,})")
        plt.legend(title="Brake Limit")
        plt.tight_layout()
        plt.savefig(os.path.join(output_dir, "Fig1b_SubX_Cost.pdf"))
        plt.close()

        # Fig 1c: Recall vs Subscribers
        plt.figure(figsize=(10, 6))
        sns.lineplot(data=subset_max_pubs, x='subscribers', y='recall', hue='Brake_Label', style='Brake_Label', markers=True, palette='Reds')
        plt.xscale('log')
        plt.ylim(0, 1.1)
        plt.xlabel("Number of Subscribers", fontweight='bold')
        plt.ylabel("Recall (Success Rate)", fontweight='bold')
        plt.title(f"Scalability: Reliability vs Subscribers\n(Fixed Pubs: {int(max_pubs):,})")
        plt.legend(title="Brake Limit")
        plt.tight_layout()
        plt.savefig(os.path.join(output_dir, "Fig1c_SubX_Recall.pdf"))
        plt.close()

    # -- Subset 2: Unbounded Brake (For Table Size/Traffic Ratio comparison with Regional) --
    subset_unbounded = df[df['brake_limit'] == -1]
    
    if not subset_unbounded.empty:
        # Fig 1d: Table Size vs Subscribers (Hue=Pubs)
        plt.figure(figsize=(10, 6))
        sns.lineplot(data=subset_unbounded, x='subscribers', y='table_size_avg', hue='Pubs_Label', markers=True, palette="rocket_r")
        plt.xscale('log')
        plt.yscale('log')
        plt.xlabel("Number of Subscribers", fontweight='bold')
        plt.ylabel("Avg Table Size", fontweight='bold')
        plt.title("State Overhead: Table Size\n(Brake: Unbounded)")
        plt.legend(title="Publishers")
        plt.tight_layout()
        plt.savefig(os.path.join(output_dir, "Fig1d_SubX_TableSize.pdf"))
        plt.close()

        # Fig 1e: Traffic Ratio vs Subscribers (Hue=Pubs)
        plt.figure(figsize=(10, 6))
        sns.lineplot(data=subset_unbounded, x='subscribers', y='traffic_ratio', hue='Pubs_Label', markers=True, palette="rocket_r")
        plt.xscale('log')
        plt.yscale('log')
        plt.xlabel("Number of Subscribers", fontweight='bold')
        plt.ylabel("Traffic Ratio", fontweight='bold')
        plt.title("Network Efficiency: Traffic Ratio\n(Brake: Unbounded)")
        plt.legend(title="Publishers")
        plt.tight_layout()
        plt.savefig(os.path.join(output_dir, "Fig1e_SubX_TrafficRatio.pdf"))
        plt.close()

# ==============================================================================
# GROUP 2: PUBLISHER DENSITY (X-Axis = Publishers)
# ==============================================================================

def plot_publisher_x_graphs(df, output_dir):
    print("Generating Group 2: Publisher Density (3 Graphs)...")
    
    # -- Subset 1: Max Subscribers --
    max_subs = df['subscribers'].max()
    subset_max_subs = df[df['subscribers'] == max_subs]

    if not subset_max_subs.empty:
        # Fig 2a: Stretch vs Publishers
        plt.figure(figsize=(10, 6))
        sns.lineplot(data=subset_max_subs, x='publishers', y='avg_stretch', hue='Brake_Label', style='Brake_Label', markers=True, palette=PALETTE)
        plt.xscale('log')
        plt.xlabel("Number of Publishers", fontweight='bold')
        plt.ylabel("Avg Stretch (km)", fontweight='bold')
        plt.title(f"Density Impact: Accuracy\n(Fixed Subs: {int(max_subs):,})")
        plt.legend(title="Brake Limit")
        plt.tight_layout()
        plt.savefig(os.path.join(output_dir, "Fig2a_PubX_Stretch.pdf"))
        plt.close()

        # Fig 2b: Cost vs Publishers
        plt.figure(figsize=(10, 6))
        sns.lineplot(data=subset_max_subs, x='publishers', y='matching_cost', hue='Brake_Label', style='Brake_Label', markers=True, palette='magma')
        plt.xscale('log')
        plt.yscale('log')
        plt.xlabel("Number of Publishers", fontweight='bold')
        plt.ylabel("Matching Cost", fontweight='bold')
        plt.title(f"Density Impact: Overhead\n(Fixed Subs: {int(max_subs):,})")
        plt.legend(title="Brake Limit")
        plt.tight_layout()
        plt.savefig(os.path.join(output_dir, "Fig2b_PubX_Cost.pdf"))
        plt.close()

    # -- Subset 2: Unbounded Brake --
    subset_unbounded = df[df['brake_limit'] == -1].copy()
    if not subset_unbounded.empty:
        # Fig 2c: Traffic Ratio vs Publishers (Hue=Subs)
        plt.figure(figsize=(10, 6))
        sns.lineplot(data=subset_unbounded, x='publishers', y='traffic_ratio', hue='Subs_Label', palette="crest", marker="o")
        plt.xscale('log')
        plt.yscale('log')
        plt.xlabel("Number of Publishers", fontweight='bold')
        plt.ylabel("Traffic Ratio", fontweight='bold')
        plt.title("Network Efficiency vs Density\n(Brake: Unbounded)")
        
        # Sort legend numerically
        handles, labels = plt.gca().get_legend_handles_labels()
        # Simple sort helper (converts 1M -> 1000000 for sorting)
        def sort_key(s): 
            return float(s.replace('M','000000').replace('k','000'))
        
        # Combine, sort, unzip
        hl = sorted(zip(handles, labels), key=lambda x: sort_key(x[1]))
        handles2, labels2 = zip(*hl)
        plt.legend(handles2, labels2, title="Subscribers")
        
        plt.tight_layout()
        plt.savefig(os.path.join(output_dir, "Fig2c_PubX_TrafficRatio.pdf"))
        plt.close()

# ==============================================================================
# GROUP 3: TRADE-OFFS (At Specific Scale)
# ==============================================================================

def plot_tradeoff_graphs(df, output_dir):
    print("Generating Group 3: Trade-offs (2 Graphs)...")
    
    t_pubs, t_subs = get_robust_max_scale(df)
    subset = df[(df['publishers'] == t_pubs) & (df['subscribers'] == t_subs)].sort_values('brake_sort')
    
    if subset.empty or len(subset) < 2:
        print(" -> Skipping Trade-offs (Not enough comparison data)")
        return
        
    print(f" -> Using Scale: {int(t_pubs)} Pubs, {int(t_subs)} Subs")

    # Fig 3a: Sweet Spot (Recall vs Cost)
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
    plt.title(f"Efficiency Trade-off\n(Scale: {int(t_subs):,} Subs)")
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, "Fig3a_Tradeoff_SweetSpot.pdf"))
    plt.close()

    # Fig 3b: Pareto (Stretch vs Cost)
    valid_pareto = subset[subset['recall'] > 0.1]
    if len(valid_pareto) >= 2:
        plt.figure(figsize=(9, 6))
        sns.scatterplot(data=valid_pareto, x='matching_cost', y='avg_stretch', hue='Brake_Label', style='Brake_Label', palette='viridis', s=200, zorder=5)
        sns.lineplot(data=valid_pareto, x='matching_cost', y='avg_stretch', color='grey', alpha=0.5, sort=False, legend=False, zorder=1)
        plt.xscale('log')
        plt.xlabel("Overhead (Matching Cost)", fontweight='bold')
        plt.ylabel("Inaccuracy (Avg Stretch km)", fontweight='bold')
        plt.title("The Cost of Perfection")
        plt.grid(True, which="both", ls="-", alpha=0.2)
        plt.legend(title="Brake Limit")
        plt.tight_layout()
        plt.savefig(os.path.join(output_dir, "Fig3b_Tradeoff_Pareto.pdf"))
        plt.close()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("batch_id", help="Batch ID")
    args = parser.parse_args()

    input_path, output_dir = setup_paths(args.batch_id)
    print(f"Reading: {input_path}")
    print(f"Output:  {output_dir}")
    
    df = load_data(input_path)
    
    plot_subscriber_x_graphs(df, output_dir)
    plot_publisher_x_graphs(df, output_dir)
    plot_tradeoff_graphs(df, output_dir)
    
    print("\nProcessing Complete. All figures generated.")

if __name__ == "__main__":
    main()