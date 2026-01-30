import os
import csv
import re
import argparse
import sys

def parse_log_file(file_path):
    """
    Parses a single DNS++ simulation log file.
    """
    metrics = {
        'experiment_id': None,
        'publishers': None,
        'subscribers': None,
        'fpr_threshold': None,
        'table_size_avg': None,
        'core_table_size_avg': None, 
        'pub_events': None,
        'avg_comparisons': None,
        'sub_updates_sent': None,
        'traffic_ratio': None,
        'traffic_saved': None,
        'suppression_rate': None,
        'false_positives': None,
        'false_positive_rate': None,
        'total_brokers': None,       
        'total_leaf_brokers': None   
    }

    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

            # --- 1. Extract Experiment/Run ID ---
            match_id = re.search(r"Run ID\s+:\s+(\d+)", content)
            if match_id:
                metrics['experiment_id'] = match_id.group(1)
            else:
                match_filename = re.search(r"simulation_log_(\d+)", os.path.basename(file_path))
                if match_filename:
                    metrics['experiment_id'] = match_filename.group(1)
            
            # Threshold
            match_thresh = re.search(r"Smart Threshold\s+:\s+([\d\.]+)", content)
            if match_thresh:
                metrics['fpr_threshold'] = float(match_thresh.group(1))
            else:
                match_thresh_cfg = re.search(r"broker\.smartThreshold\s*=\s*([\d\.]+)", content)
                if match_thresh_cfg:
                     metrics['fpr_threshold'] = float(match_thresh_cfg.group(1))

            # Subscribers
            match_subs = re.search(r"Total Subscribers\s+:\s+(\d+)", content)
            if match_subs:
                metrics['subscribers'] = int(match_subs.group(1))

            # Publishers
            match_pubs = re.search(r"Collected\s+\d+\s+subscribers\s+and\s+(\d+)\s+publishers", content)
            if match_pubs:
                metrics['publishers'] = int(match_pubs.group(1))
            else:
                match_pubs_2 = re.search(r"Publisher Placement Complete:\s+(\d+)\s+created", content)
                if match_pubs_2:
                    metrics['publishers'] = int(match_pubs_2.group(1))
            
            # --- Broker Counts ---
            match_total_brokers = re.search(r"Total Brokers\s*:\s*(\d+)", content)
            if match_total_brokers:
                metrics['total_brokers'] = int(match_total_brokers.group(1))
                
            match_leaf_brokers = re.search(r"Total Leaf Brokers\s*:\s*(\d+)", content)
            if match_leaf_brokers:
                metrics['total_leaf_brokers'] = int(match_leaf_brokers.group(1))

            # --- 3. Extract Performance Metrics ---
            
            # Table Size (Global Average)
            match_table = re.search(r"Input Table Size \(Min / Avg / Max\)\s+:\s+\d+\s+/\s+([\d\.]+)\s+/\s+\d+", content)
            if match_table:
                metrics['table_size_avg'] = float(match_table.group(1))

            # --- Calculate Core Table Size (REVERTED FORMULA) ---
            # Architectural Definition: Leaf Brokers typically only hold entries from their
            # local subscribers (Split Horizon). They do NOT hold an entry from the Parent.
            # Therefore: TotalSystemEntries = (CoreEntries) + (SubscriberEntries)
            if (metrics['table_size_avg'] is not None and 
                metrics['total_brokers'] and 
                metrics['total_leaf_brokers'] and 
                metrics['subscribers'] is not None):
                
                core_brokers = metrics['total_brokers'] - metrics['total_leaf_brokers']
                
                if core_brokers > 0:
                    total_system_entries = metrics['table_size_avg'] * float(metrics['total_brokers'])
                    
                    # Simply subtract the subscribers (who reside at the leaves)
                    core_entries = total_system_entries - float(metrics['subscribers'])
                    
                    core_entries = max(0.0, core_entries)
                    metrics['core_table_size_avg'] = core_entries / float(core_brokers)
                else:
                    metrics['core_table_size_avg'] = 0.0

            # Total Forwarding Events
            match_pub_events = re.search(r"Total Forwarding Events \(Traffic\)\s+:\s+([\d,]+)", content)
            if match_pub_events:
                metrics['pub_events'] = int(match_pub_events.group(1).replace(',', ''))

            # Avg Comparisons
            match_comparisons = re.search(r"Avg Comparisons per Message\s+:\s+([\d\.]+)", content)
            if match_comparisons:
                metrics['avg_comparisons'] = float(match_comparisons.group(1))

            # Total Output Entries
            match_sub_updates = re.search(r"Total Output Entries \(Propagated\)\s+:\s+([\d,]+)", content)
            if match_sub_updates:
                metrics['sub_updates_sent'] = int(match_sub_updates.group(1).replace(',', ''))

            # Traffic Ratio
            match_traffic_ratio = re.search(r"Traffic Ratio \(Events per Delivery\)\s+:\s+([\d\.]+)", content)
            if match_traffic_ratio:
                metrics['traffic_ratio'] = float(match_traffic_ratio.group(1))

            # Traffic Saved & Suppression Rate
            total_input = 0
            match_total_input = re.search(r"Total Input Entries \(Received\)\s+:\s+([\d,]+)", content)
            if match_total_input:
                total_input = int(match_total_input.group(1).replace(',', ''))

            match_covered_detailed = re.search(r"-> Covered \(Filtered/Suppressed\)\s+:\s+([\d,]+)\s+/\s+([\d,]+)", content)
            
            if match_covered_detailed:
                input_covered = int(match_covered_detailed.group(1).replace(',', ''))
                output_covered = int(match_covered_detailed.group(2).replace(',', ''))
                metrics['traffic_saved'] = input_covered + output_covered
                
                if total_input > 0:
                    metrics['suppression_rate'] = float(metrics['traffic_saved']) / float(total_input)
                else:
                    metrics['suppression_rate'] = 0.0
            else:
                match_traffic_saved = re.search(r"Traffic Saved \(Filtered/Absorbed\)\s+:\s+([\d,]+)", content)
                if match_traffic_saved:
                    metrics['traffic_saved'] = int(match_traffic_saved.group(1).replace(',', ''))

            # False Positives & Rate
            match_fp = re.search(r"False Positive Events \(Dead Ends\)\s+:\s+([\d,]+)", content)
            if match_fp:
                metrics['false_positives'] = int(match_fp.group(1).replace(',', ''))
            elif "5. ROUTING OVERHEAD & EFFICIENCY" in content:
                metrics['false_positives'] = 0
            
            # Calculate FP Rate
            if metrics['false_positives'] is not None and metrics['pub_events'] and metrics['pub_events'] > 0:
                metrics['false_positive_rate'] = (float(metrics['false_positives']) / float(metrics['pub_events'])) * 100.0
            elif metrics['false_positives'] == 0:
                metrics['false_positive_rate'] = 0.0

    except Exception as e:
        print(f"Error parsing {file_path}: {e}", file=sys.stderr)
        return None

    if metrics['publishers'] is None or metrics['fpr_threshold'] is None:
        return None
        
    return metrics

def format_value(key, value):
    if value is None:
        return ""
    
    if key == 'table_size_avg':
        return f"{value:.2f}"
    elif key == 'core_table_size_avg': 
        return f"{value:.2f}"
    elif key == 'avg_comparisons':
        return f"{value:.2f}"
    elif key == 'traffic_ratio':
        return f"{value:.4f}"
    elif key == 'suppression_rate':
        return f"{value:.4f}"
    elif key == 'false_positive_rate':
        return f"{value:.2f}" 
    else:
        return str(value)

def scan_directory_and_process(root_folder, output_csv):
    all_data = []
    print(f"Scanning directory: {root_folder} ...")

    for root, dirs, files in os.walk(root_folder):
        for file in files:
            if file.endswith(".log") and file.startswith("simulation_log"):
                full_path = os.path.join(root, file)
                data = parse_log_file(full_path)
                if data:
                    all_data.append(data)

    if not all_data:
        print("No valid simulation logs found.")
        return

    # Sort data
    all_data.sort(key=lambda x: (
        x['publishers'] if x['publishers'] is not None else 0, 
        x['subscribers'] if x['subscribers'] is not None else 0, 
        x['fpr_threshold'] if x['fpr_threshold'] is not None else 0
    ))

    # Define CSV Headers
    headers = [
        'experiment_id',
        'publishers',
        'subscribers',
        'fpr_threshold',
        'table_size_avg',
        'core_table_size_avg',
        'pub_events',
        'avg_comparisons',
        'sub_updates_sent',
        'traffic_ratio',
        'traffic_saved',
        'suppression_rate',
        'false_positives',
        'false_positive_rate'
    ]

    print(f"Writing {len(all_data)} rows to {output_csv} ...")
    
    with open(output_csv, 'w', newline='') as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=headers)
        writer.writeheader()
        
        for row in all_data:
            row_to_write = {k: row.get(k) for k in headers}
            formatted_row = {k: format_value(k, v) for k, v in row_to_write.items()}
            writer.writerow(formatted_row)

    print("Done.")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Extract DNS++ metrics from simulation logs.')
    parser.add_argument('input_folder', help='Path to the batch folder containing timestamped subfolders')
    parser.add_argument('output_csv', help='Path for the output CSV file')

    args = parser.parse_args()

    if not os.path.isdir(args.input_folder):
        print(f"Error: Directory '{args.input_folder}' does not exist.")
        sys.exit(1)

    scan_directory_and_process(args.input_folder, args.output_csv)