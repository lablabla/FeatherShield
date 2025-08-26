import argparse
from generate_device_certs import generate_certs
from generate_qr import generate_qr_code
from generate_nvs_csv import generate_nvs_csv
import uuid


def main():
    parser = argparse.ArgumentParser(description="Provision a new device.")
    parser.add_argument("--device-id", type=str, required=True, help="Device ID")
    parser.add_argument("--ca-folder", type=str, required=True, help="Path to the CA folder")
    parser.add_argument("--out-folder", type=str, required=True, help="Path to the output folder")
    parser.add_argument("--pop", type=str, default=str(uuid.uuid4()), help="Proof Of Possessiong string")
    args = parser.parse_args()
    device_id = args.device_id
    ca_folder = args.ca_folder
    out_folder = args.out_folder
    pop = args.pop
    generate_certs(ca_folder, out_folder, device_id)
    generate_qr_code(out_folder, device_id, pop)
    generate_nvs_csv(ca_folder, out_folder, device_id, pop)

if __name__ == "__main__":
    main()