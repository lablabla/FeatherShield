import csv
import os
import subprocess
import sys


def generate_nvs_bin(out_folder, device_id, partition_size="16384") -> str:
    csv_file_path = os.path.join(out_folder, f"nvs.csv")
    bin_file_path = os.path.join(out_folder, f"nvs.bin")

    with open(csv_file_path, mode="w", newline="") as csv_file:
        writer = csv.writer(csv_file)
        writer.writerow(["key", "type", "encoding", "value"])
        writer.writerow(["certs", "namespace", "", ""])
        writer.writerow(["client_cert", "file", "string", "client.crt"])
        writer.writerow(["client_key", "file", "string", "client.key"])
        writer.writerow(["ca_cert", "file", "string", "ca.crt"])
        writer.writerow(["info", "namespace", "", ""])
        writer.writerow(["device_id", "data", "string", device_id])
    print(f"Generated NVS CSV at {csv_file_path}")

    idf_path = os.getenv("IDF_PATH")
    if not idf_path:
        raise EnvironmentError("IDF_PATH environment variable is not set. "
                            "Please source the ESP-IDF export script (e.g., 'get_idf' or '. ./export.sh').")
    
    nvs_tool_path = os.path.join(idf_path, "components", "nvs_flash", "nvs_partition_generator", "nvs_partition_gen.py")
    command = [
        sys.executable,
        nvs_tool_path,
        "generate",
        os.path.basename(csv_file_path), # Use basename as we run from out_folder
        os.path.basename(bin_file_path),
        partition_size
    ]
    try:
        print(f"Running command: {' '.join(command)}")
        # We must run the command from the output directory so the tool can find the certificate files
        result = subprocess.run(command, check=True, cwd=out_folder, capture_output=True, text=True)
        if not os.path.exists(bin_file_path):
            print(result.stderr)
            raise FileNotFoundError(f"NVS binary file was not created at {bin_file_path}")
        print(result.stdout)
        print(f"Successfully generated NVS binary at {bin_file_path}")
    except subprocess.CalledProcessError as e:
        print("ERROR: Failed to generate NVS binary.")
        print(f"STDOUT:\n{e.stdout}")
        print(f"STDERR:\n{e.stderr}")
        raise e


    return csv_file_path

if __name__ == "__main__":
    generate_nvs_bin("../out")
    print(f"Generated NVS CSV")