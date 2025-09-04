import argparse
from generate_device_certs import generate_certs
from generate_qr import generate_qr_code
from utilities.generate_nvs_bin import generate_nvs_bin
import os
import shlex
import subprocess
import uuid
import esptool


def main():
    parser = argparse.ArgumentParser(description="Provision a new device.")
    parser.add_argument("--device-id", type=str, required=True, help="Device ID")
    parser.add_argument("--out-folder", type=str, default="firmware/esp32/build", help="Path to the output folder")
    parser.add_argument("--pop", type=str, default=str(uuid.uuid4()), help="Proof Of Possessiong string")
    parser.add_argument("--skip_certs", action="store_true", help="Skip certificate generation")
    args = parser.parse_args()
    device_id = args.device_id
    out_folder = args.out_folder
    pop = args.pop
    if not args.skip_certs:
        generate_certs(out_folder, device_id)
    generate_nvs_bin(out_folder, device_id, pop)
    # TODO: Run esptool to generate certs.bin
    generate_qr_code(out_folder, device_id, pop)
    
    command = "esptool -p COM3 -b 460800 --before default_reset --after hard_reset --chip esp32s3 write_flash --flash_mode dio --flash_freq 80m --flash_size 4MB 0x0 bootloader/bootloader.bin 0x10000 feathershield.bin 0x8000 partition_table/partition-table.bin 0x9000 certs.bin"
    subprocess.run(shlex.split(command), cwd=out_folder, check=True)


if __name__ == "__main__":
    main()