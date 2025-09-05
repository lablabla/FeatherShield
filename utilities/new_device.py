import argparse
from generate_device_certs import generate_certs
from generate_qr import generate_qr_code
from utilities.generate_nvs_bin import generate_nvs_bin
import os
import shlex
import subprocess
import uuid
import esptool

def get_mac_address_from_module(esp) -> str | None:
    """
    Reads the MAC address using esptool as an imported Python module.
    This is the most robust and reliable method.
    """
    try:
        mac_bytes = esp.read_mac()
        mac_address = ":".join(f"{b:02x}" for b in mac_bytes)
        print(f"Successfully read MAC address: {mac_address}")
        return mac_address
    except Exception as e:
        print(f"ERROR: Failed to read MAC address using esptool module: {e}")
        return None

def get_device_id(esp) -> str:
    mac = get_mac_address_from_module(esp)
    device_id = f"fs32-{mac.replace(':', '')}"
    return device_id

def main():
    parser = argparse.ArgumentParser(description="Provision a new device.")
    parser.add_argument("--out-folder", type=str, default="firmware/esp32/build", help="Path to the output folder")
    parser.add_argument("--skip_certs", action="store_true", help="Skip certificate generation")
    parser.add_argument("--port", type=str, default="COM4", help="Serial port for flashing (e.g., COM1 or /dev/ttyUSB0)")
    parser.add_argument("--baud", type=int, default=115200, help="Baud rate for flashing")
    parser.add_argument("--chip", type=str, default="esp32s3", help="Chip type (e.g., esp32, esp32s2, esp32s3)")
    args = parser.parse_args()
    out_folder = args.out_folder

    print(f"Connecting to {args.port} via esptool module...")
    with esptool.detect_chip(args.port) as esp:
        esp.connect()
        device_id = get_device_id(esp)
    print(f"Using device ID: {device_id}")

    if not args.skip_certs:
        generate_certs(out_folder, device_id)
    generate_nvs_bin(out_folder, device_id)
    generate_qr_code(out_folder, device_id)

    print("Flashing device...")
    addr_data = [
        (0x0, f"{out_folder}/bootloader/bootloader.bin"),
        (0x10000, f"{out_folder}/feathershield.bin"),
        (0x8000, f"{out_folder}/partition_table/partition-table.bin"),
        (0x9000, f"{out_folder}/nvs.bin"),
    ]
    use_esptool_module = False
    if use_esptool_module == True:
 
        with esptool.detect_chip(args.port) as esp:
            esp.connect()
            esptool.write_flash(esp, addr_data, flash_mode="dio", flash_freq="80m", flash_size="4MB", compress=True)
    else:
        addr_data_str = " ".join(f"0x{addr:02x} {path}" for addr, path in addr_data)
        command = f"esptool -p {args.port} -b {args.baud} --before default-reset --after hard-reset --chip {args.chip} write-flash --flash-mode dio --flash-freq 80m --flash-size 4MB {addr_data_str} --compress"
        subprocess.run(shlex.split(command), check=True)


if __name__ == "__main__":
    main()