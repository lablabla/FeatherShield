import csv
import os

DEVICE_ID = "esp32-001"

def generate_nvs_csv(ca_folder, out_folder, device_id, pop) -> str:
    csv_file_path = os.path.join(out_folder, f"{device_id}_nvs.csv")

    csv_values = [
        {"key": "client_cert", "type": "file", "encoding": "binary", "value": "client.crt"},
        {"key": "client_key", "type": "file", "encoding": "binary", "value": "client.key"},
        {"key": "ca_cert", "type": "file", "encoding": "binary", "value": "ca.crt"},
        {"key": "device_id", "type": "string", "encoding": "", "value": device_id},
        {"key": "pop", "type": "string", "encoding": "", "value": pop},
    ]

    with open(csv_file_path, mode="w", newline="") as csv_file:
        writer = csv.writer(csv_file)
        writer.writerow(["key", "type", "encoding", "value"])
        for value in csv_values:
            writer.writerow([value["key"], value["type"], value["encoding"], value["value"]])
    return csv_file_path

    print(f"Generated NVS CSV at {csv_file_path}")



if __name__ == "__main__":
    generate_nvs_csv("../ca", "../out", DEVICE_ID, "unique_pop_string")
    print(f"Generated NVS CSV for {DEVICE_ID}")