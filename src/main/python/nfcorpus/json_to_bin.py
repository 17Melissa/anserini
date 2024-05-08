import json
from safetensors.torch import save_file
import torch
import os

# Define paths
input_file_path = 'vectors.part00.jsonl'
output_directory = 'doc_vectors'
if not os.path.exists(output_directory):
    os.makedirs(output_directory)

vectors_path = os.path.join(output_directory, 'vectors.safetensors')
docids_path = os.path.join(output_directory, 'docids.safetensors')

# Initialize lists to hold data
vectors = []
docids = []

# Process the JSONL file to extract vectors and docids
with open(input_file_path, 'r') as file:
    for line in file:
        entry = json.loads(line)
        vectors.append(entry['vector'])
        docids.append(entry['docid'])
        print(f"Processed and saved docid: {entry['docid']} and the vectors start with: {entry['vector'][:1]}")

# Convert lists to tensors
vectors_tensor = torch.tensor(vectors)
docid_to_idx = {docid: idx for idx, docid in enumerate(set(docids))}
idxs = [docid_to_idx[docid] for docid in docids]
docids_tensor = torch.tensor(idxs, dtype=torch.int64)

# Save the tensors to SafeTensors files
save_file({'vectors': vectors_tensor}, vectors_path)
save_file({'docids': docids_tensor}, docids_path)

# Save the docid_to_idx mapping to a JSON file
docid_to_idx_path = os.path.join(output_directory, 'docid_to_idx.json')
with open(docid_to_idx_path, 'w') as f:
    json.dump(docid_to_idx, f)

print(f"Saved vectors to {vectors_path}")
print(f"Saved docids to {docids_path}")
