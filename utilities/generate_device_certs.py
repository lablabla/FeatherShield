# generate_device_certs.py
import os
import shutil
import subprocess
import tempfile
import uuid

DEVICE_ID = "esp32-001"

def generate_certs(ca_folder, out_folder, device_id):
    # Generate temp folder
    tmp_folder = tempfile.mkdtemp()
    # Generate client private key
    subprocess.run(["openssl", "genrsa", "-out", f"{out_folder}/client.key", "4096"], check=True)
    # Create client CSR
    subprocess.run(["openssl", "req", "-new", "-key", f"{out_folder}/client.key", "-out", f"{tmp_folder}/client.csr", "-subj", f"/CN={device_id}"])
    # Sign client certificate with the CA
    # (This assumes ca.crt and ca.key are in a known location)
    subprocess.run(["sudo", "openssl", "x509", "-req", "-in", f"{tmp_folder}/client.csr", "-CA", f"{ca_folder}/ca.crt", "-CAkey", f"{ca_folder}/ca.key", "-CAcreateserial", "-out", f"{out_folder}/client.crt", "-days", "3650", "-sha256"], check=True)
    # Remove temp folder
    shutil.rmtree(tmp_folder)

if __name__ == "__main__":
    generate_certs("../ca", "../out", DEVICE_ID)
    print(f"Generated certificates for {DEVICE_ID}")
