import csv
import os

DEVICE_ID = "esp32-001"

def generate_nvs_bin(out_folder, device_id, pop) -> str:
    csv_file_path = os.path.join(out_folder, f"{device_id}_nvs.csv")

    with open(csv_file_path, mode="w", newline="") as csv_file:
        writer = csv.writer(csv_file)
        writer.writerow(["key", "type", "encoding", "value"])
        writer.writerow(["certs", "namespace", "", ""])
        writer.writerow(["client_cert", "file", "string", "client.crt"])
        writer.writerow(["client_key", "file", "string", "client.key"])
        writer.writerow(["ca_cert", "file", "string", "ca.crt"])
    print(f"Generated NVS CSV at {csv_file_path}")

    #TODO: Convert CSV to BIN using nvs_partition_gen.py
    
    return csv_file_path

if __name__ == "__main__":
    generate_nvs_bin("../out", DEVICE_ID, "unique_pop_string")
    print(f"Generated NVS CSV for {DEVICE_ID}")