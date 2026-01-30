import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import os
import argparse
import sys
import matplotlib.ticker as ticker
import numpy as np

# ================= CONFIGURATION =================
INPUT_FILENAME = 'results_regional.csv'

# Graph Style Settings
sns.set_theme(style="whitegrid")
plt.rcParams.update({'font.size': 12, 'font.family': 'sans-serif'})
LINE_WIDTH = 2.5
MARKER_SIZE = 8
PALETTE = "viridis" 

# =================================================

def setup_paths(batch_id):
    """Calculates input/output paths."""
    script_dir = os.path.dirname(os.path.abspath(__file__))
    batch_folder_name = f"batch_{batch_id}"
    input_path = os.path.join(script_dir, '..', 'output', batch_folder_name, INPUT_FILENAME)
    output_dir = os.path.join(script_dir, 'graphs', batch_folder_name, 'paper_ready')
    
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
        
    return input_path, output_dir

def load_data(filepath):
    """Loads CSV and ensures numeric types."""
    if not os.path.exists(filepath):
        print(f"Error: File not found at {filepath}")
        sys.exit(1)
        
    df = pd.read_csv(filepath)
    
    # Ensure numeric columns
    cols_to_numeric = ['publishers', 'subscribers', 'fpr_threshold', 'table_size_avg', 
                       'core_table_size_avg', 'sub_updates_sent', 'suppression_rate', 
                       'pub_events', 'false_positive_rate', 'false_positives']
    
    for col in cols_to_numeric:
        if col in df.columns:
            df[col] = pd.to_numeric(df[col], errors='coerce')

    df['FPR_Label'] = df['fpr_threshold'].astype(str)
    return df

def plot_dual_axis_tradeoff(df, output_dir, pub_sub_pairs):
    """
    PLOT 1 Variant: Efficiency vs. Accuracy Trade-off (Linear X-Axis).
    """
    print("Generating Plot 1 Variants: Efficiency vs Accuracy Trade-off (Linear X)...")
    
    for pubs, subs in pub_sub_pairs:
        # Filter Data
        subset = df[(df['publishers'] == pubs) & (df['subscribers'] == subs)].sort_values('fpr_threshold')
        
        if subset.empty:
            continue

        fig, ax1 = plt.subplots(figsize=(10, 6))

        # --- Linear X-Axis Configuration ---
        x_col = 'fpr_threshold' 
        
        # Plot 1: Traffic (Left Axis)
        color1 = 'tab:blue'
        ax1.set_xlabel('FPR Threshold (Aggregation Level)', fontweight='bold')
        ax1.set_ylabel('Control Traffic (Updates Sent)', color=color1, fontweight='bold')
        
        sns.lineplot(data=subset, x=x_col, y='sub_updates_sent', marker='o', ax=ax1, color=color1, linewidth=LINE_WIDTH)
        
        ax1.tick_params(axis='y', labelcolor=color1)
        ax1.set_yscale('log') 
        ax1.grid(True, which="both", ls="-", alpha=0.3)
        
        # Explicit Ticks
        experimental_ticks = [0.0, 0.1, 0.25, 0.5, 1.0]
        ax1.set_xticks(experimental_ticks)

        # Plot 2: Accuracy (Right Axis)
        ax2 = ax1.twinx() 
        color2 = 'tab:red'
        ax2.set_ylabel('False Positive Rate (%)', color=color2, fontweight='bold')
        
        sns.lineplot(data=subset, x=x_col, y='false_positive_rate', marker='s', ax=ax2, color=color2, linewidth=LINE_WIDTH, linestyle='--')
        
        ax2.tick_params(axis='y', labelcolor=color2)
        
        # Adjust Y-Limit
        max_fp = subset['false_positive_rate'].max()
        if pd.notna(max_fp) and max_fp < 5.0:
            ax2.set_ylim(0, 5.0) 
        elif pd.notna(max_fp):
            ax2.set_ylim(0, max_fp * 1.2)

        plt.title(f"Trade-off: Efficiency vs. Accuracy\n(Subs: {int(subs):,}, Pubs: {int(pubs):,})")
        
        filename = f"Fig1_Tradeoff_P{int(pubs)}_S{int(subs)}_Linear.pdf"
        plt.savefig(os.path.join(output_dir, filename), format='pdf', bbox_inches='tight')
        plt.close()

def plot_control_plane_scaling(df, output_dir, fixed_pubs):
    """
    PLOTS 2, 3, 4: Scalability of Control Plane.
    Split Fig 3b for clarity.
    """
    print(f"Generating Control Plane Plots (Fixed Pubs={fixed_pubs})...")
    
    subset = df[df['publishers'] == fixed_pubs].copy()
    if subset.empty:
        return

    subset['FPR_Label'] = subset['fpr_threshold'].astype(str)

    metrics = [
        ('sub_updates_sent', 'Control Traffic (Updates Sent)', 'log', 'Fig2_Scalability_Traffic.pdf'),
        ('table_size_avg', 'Avg Broker Table Size (Global)', 'linear', 'Fig3a_Global_TableSize.pdf'),
        ('core_table_size_avg', 'Avg Broker Table Size (Core Only)', 'linear', 'Fig3b_Core_TableSize.pdf'), 
        ('suppression_rate', 'Suppression Efficiency (0-1.0)', 'linear', 'Fig4_Suppression_Rate.pdf')
    ]

    for y_col, y_label, y_scale, filename in metrics:
        if y_col not in subset.columns or subset[y_col].isna().all():
            continue

        # --- SPECIAL HANDLING FOR FIG 3b ---
        if y_col == 'core_table_size_avg':
            # 1. Baseline (FPR = 0.0)
            baseline_subset = subset[subset['fpr_threshold'] == 0.0]
            if not baseline_subset.empty:
                plt.figure(figsize=(8, 6))
                sns.lineplot(data=baseline_subset, x='subscribers', y=y_col, hue='FPR_Label', style='FPR_Label', markers=True, palette=PALETTE, linewidth=LINE_WIDTH, markersize=MARKER_SIZE)
                plt.xscale('log')
                plt.yscale('linear') 
                plt.xlabel("Number of Subscribers", fontweight='bold')
                plt.ylabel(y_label, fontweight='bold')
                plt.title(f"{y_label} - Baseline (Exact Match)\n(Fixed Publishers: {int(fixed_pubs):,})")
                plt.legend(title="FPR Threshold", bbox_to_anchor=(1.05, 1), loc='upper left')
                plt.savefig(os.path.join(output_dir, "Fig3b_Core_TableSize_Baseline.pdf"), format='pdf', bbox_inches='tight')
                plt.close()

            # 2. Aggregated (FPR > 0.0)
            agg_subset = subset[subset['fpr_threshold'] > 0.0]
            if not agg_subset.empty:
                plt.figure(figsize=(8, 6))
                sns.lineplot(data=agg_subset, x='subscribers', y=y_col, hue='FPR_Label', style='FPR_Label', markers=True, palette=PALETTE, linewidth=LINE_WIDTH, markersize=MARKER_SIZE)
                plt.xscale('log')
                plt.yscale('linear')
                plt.xlabel("Number of Subscribers", fontweight='bold')
                plt.ylabel(y_label, fontweight='bold')
                plt.title(f"{y_label} - Aggregated\n(Fixed Publishers: {int(fixed_pubs):,})")
                plt.legend(title="FPR Threshold", bbox_to_anchor=(1.05, 1), loc='upper left')
                plt.savefig(os.path.join(output_dir, "Fig3b_Core_TableSize_Aggregated.pdf"), format='pdf', bbox_inches='tight')
                plt.close()
            continue 

        # --- STANDARD PLOTTING ---
        plt.figure(figsize=(8, 6))
        sns.lineplot(data=subset, x='subscribers', y=y_col, hue='FPR_Label', style='FPR_Label', markers=True, palette=PALETTE, linewidth=LINE_WIDTH, markersize=MARKER_SIZE)
        plt.xscale('log')
        if y_scale == 'log':
            plt.yscale('log')
            
        plt.xlabel("Number of Subscribers", fontweight='bold')
        plt.ylabel(y_label, fontweight='bold')
        plt.title(f"{y_label} vs. Scale\n(Fixed Publishers: {int(fixed_pubs):,})")
        plt.legend(title="FPR Threshold", bbox_to_anchor=(1.05, 1), loc='upper left')
        plt.savefig(os.path.join(output_dir, filename), format='pdf', bbox_inches='tight')
        plt.close()

def plot_stability_heatmap(df, output_dir, fixed_fpr=0.25):
    """
    ALTERNATIVE 1: Heatmap of Routing Stability.
    """
    print(f"Generating Routing Stability Heatmap (FPR={fixed_fpr})...")
    
    subset = df[df['fpr_threshold'] == fixed_fpr].copy()
    if subset.empty: return

    # Pivot to create matrix: Index=Subs, Columns=Pubs, Values=FP Rate
    pivot_table = subset.pivot(index='subscribers', columns='publishers', values='false_positive_rate')
    pivot_table = pivot_table.sort_index(ascending=False)

    plt.figure(figsize=(10, 8))
    sns.heatmap(pivot_table, annot=True, fmt=".1f", cmap="RdYlGn_r", 
                cbar_kws={'label': 'False Positive Rate (%)'},
                linewidths=.5)

    plt.title(f"Routing Stability Landscape (FPR={fixed_fpr})\nFalse Positive Rate (%)", fontweight='bold')
    plt.xlabel("Number of Publishers")
    plt.ylabel("Number of Subscribers")
    
    plt.savefig(os.path.join(output_dir, "Fig6b_Routing_Stability_Heatmap.pdf"), format='pdf', bbox_inches='tight')
    plt.close()

def plot_data_plane_filtered(df, output_dir, fixed_fpr=0.25):
    """
    ALTERNATIVE 2: Line Graph with Filtering.
    Updated to >= 10 to include the lower tier of data.
    """
    print(f"Generating Filtered Data Plane Plots (Fixed FPR={fixed_fpr})...")
    
    subset = df[df['fpr_threshold'] == fixed_fpr].copy()
    if subset.empty: return

    # --- FILTERING STEP ---
    # Adjusted to 10 to reveal the gap between 10 and 1000
    subset = subset[subset['publishers'] >= 10]
    
    subset['Subs_Label'] = subset['subscribers'].apply(lambda x: f"{int(x):,}")
    subset = subset.sort_values('subscribers')

    metrics = [
        ('pub_events', 'Forwarding Load (Events)', 'log', 'Fig5_DataPlane_Load.pdf'),
        ('false_positive_rate', 'Routing Error (False Positive %)', 'linear', 'Fig6_Routing_Stability_Clean.pdf')
    ]

    for y_col, y_label, y_scale, filename in metrics:
        plt.figure(figsize=(8, 6))
        sns.lineplot(data=subset, x='publishers', y=y_col, hue='Subs_Label', style='Subs_Label', markers=True, palette="rocket_r", linewidth=LINE_WIDTH, markersize=MARKER_SIZE)
        plt.xscale('log')
        if y_scale == 'log': plt.yscale('log')
        
        plt.xlabel("Number of Publishers (>= 10)", fontweight='bold')
        plt.ylabel(y_label, fontweight='bold')
        plt.title(f"{y_label} (Statistically Significant Range)\n(Fixed FPR: {fixed_fpr})")
        plt.legend(title="Subscribers", bbox_to_anchor=(1.05, 1), loc='upper left')
        plt.savefig(os.path.join(output_dir, filename), format='pdf', bbox_inches='tight')
        plt.close()

def plot_data_plane_inverted(df, output_dir, fixed_fpr=0.25):
    """
    NEW PLOT: Inverted version of Fig 5.
    X-Axis: Subscribers
    Lines: Publishers
    """
    print(f"Generating Inverted Data Plane Plots (Fixed FPR={fixed_fpr})...")
    
    subset = df[df['fpr_threshold'] == fixed_fpr].copy()
    if subset.empty: return

    # --- FILTERING STEP ---
    # Adjusted to 10 to reveal the gap between 10 and 1000
    subset = subset[subset['publishers'] >= 10]
    
    subset['Pubs_Label'] = subset['publishers'].apply(lambda x: f"{int(x):,}")
    subset = subset.sort_values('publishers')

    metrics = [
        ('pub_events', 'Forwarding Load (Events)', 'log', 'Fig5b_DataPlane_Load_Inverted.pdf'),
        ('false_positive_rate', 'Routing Error (False Positive %)', 'linear', 'Fig6b_Routing_Stability_Inverted.pdf')
    ]

    for y_col, y_label, y_scale, filename in metrics:
        plt.figure(figsize=(8, 6))
        # Hue = Publishers, X = Subscribers
        sns.lineplot(data=subset, x='subscribers', y=y_col, hue='Pubs_Label', style='Pubs_Label', markers=True, palette="viridis", linewidth=LINE_WIDTH, markersize=MARKER_SIZE)
        plt.xscale('log')
        if y_scale == 'log': plt.yscale('log')
        
        plt.xlabel("Number of Subscribers", fontweight='bold')
        plt.ylabel(y_label, fontweight='bold')
        plt.title(f"{y_label} (Inverted View)\n(Fixed FPR: {fixed_fpr})")
        plt.legend(title="Publishers", bbox_to_anchor=(1.05, 1), loc='upper left')
        plt.savefig(os.path.join(output_dir, filename), format='pdf', bbox_inches='tight')
        plt.close()

def main():
    parser = argparse.ArgumentParser(description="Generate Paper-Ready Graphs for DNS++")
    parser.add_argument("batch_id", help="The numeric ID of the batch")
    parser.add_argument("--fixed_pubs", type=int, default=None)
    args = parser.parse_args()

    input_path, output_dir = setup_paths(args.batch_id)
    df = load_data(input_path)
    
    if args.fixed_pubs is None:
        args.fixed_pubs = 100000 if not df[df['publishers'] == 100000].empty else df['publishers'].max()

    # Fig 1 Variants
    candidates = [
        (1000, 10000),       
        (100000, 100000),    
        (1000000, 1000000),  
        (1000000, 10000000)  
    ]
    plot_dual_axis_tradeoff(df, output_dir, candidates)
    
    # Fig 2, 3, 4
    plot_control_plane_scaling(df, output_dir, args.fixed_pubs)
    
    # Fig 5, 6 (Filtered >= 10 + Heatmap)
    plot_data_plane_filtered(df, output_dir, fixed_fpr=0.25)
    plot_data_plane_inverted(df, output_dir, fixed_fpr=0.25) # <--- Added Call
    plot_stability_heatmap(df, output_dir, fixed_fpr=0.25)

    print("\nProcessing Complete.")

if __name__ == "__main__":
    main()