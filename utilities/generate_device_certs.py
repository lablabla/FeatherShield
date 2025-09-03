# generate_device_certs.py
import os
import shutil
import subprocess
import tempfile
import uuid

from google.cloud import secretmanager_v1

DEVICE_ID = "esp32-001"

def access_secret_payload(project_id: str, secret_id: str, version_id: str) -> bytes:
    """
    Access the payload for the given secret version and return it.
    """        
    client = secretmanager_v1.SecretManagerServiceClient()
    name = f"projects/{project_id}/secrets/{secret_id}/versions/{version_id}"

    response = client.access_secret_version(request={"name": name})

    payload = response.payload.data
    return payload

def create_file_from_secret(path: str, project_id: str, secret_id: str, version_id: str = "latest"):
    secret_bytes = access_secret_payload(project_id, secret_id, version_id)
    with open(path, "wb") as f:
        f.write(secret_bytes)

def generate_certs(out_folder, device_id):
    if not os.environ.get("FEATHERSHIELD_PROJECT_ID"):
        raise ValueError("FEATHERSHIELD_PROJECT_ID environment variable must be set.")
    project_id = os.environ.get("FEATHERSHIELD_PROJECT_ID")

    if not os.environ.get("FEATHERSHIELD_CA_CERT_SECRET_ID"):
        raise ValueError("FEATHERSHIELD_CA_CERT_SECRET_ID environment variable must be set.")
    ca_cert_secret_id = os.environ.get("FEATHERSHIELD_CA_CERT_SECRET_ID")

    if not os.environ.get("FEATHERSHIELD_CA_KEY_SECRET_ID"):
        raise ValueError("FEATHERSHIELD_CA_KEY_SECRET_ID environment variable must be set.")
    ca_key_secret_id = os.environ.get("FEATHERSHIELD_CA_KEY_SECRET_ID")
    
    # Generate temp folder
    tmp_folder = tempfile.mkdtemp()
    ca_folder = os.path.join(tmp_folder, "ca")
    os.makedirs(out_folder, exist_ok=True)
    os.makedirs(ca_folder, exist_ok=True)
    create_file_from_secret(f"{os.path.join(ca_folder, 'ca.crt')}", project_id, ca_cert_secret_id)
    create_file_from_secret(f"{os.path.join(ca_folder, 'ca.key')}", project_id, ca_key_secret_id)
    # Copy ca.crt to out_folder as we will need to copy it to the ESP32
    shutil.copy(f"{os.path.join(ca_folder, 'ca.crt')}", f"{os.path.join(out_folder, 'ca.crt')}")

    # Generate client private key
    subprocess.run(["openssl", "genrsa", "-out", f"{out_folder}/client.key", "4096"], check=True)
    # Create client CSR
    subprocess.run(["openssl", "req", "-new", "-key", f"{out_folder}/client.key", "-out", f"{tmp_folder}/client.csr", "-subj", f"/CN={device_id}"])
    # Sign client certificate with the CA
    subprocess.run(["openssl", "x509", "-req", "-in", f"{tmp_folder}/client.csr", "-CA", f"{ca_folder}/ca.crt", "-CAkey", f"{ca_folder}/ca.key", "-CAcreateserial", "-out", f"{out_folder}/client.crt", "-days", "3650", "-sha256"], check=True)
    # Remove temp folder
    shutil.rmtree(tmp_folder)

if __name__ == "__main__":
    generate_certs("../out", DEVICE_ID)
    print(f"Generated certificates for {DEVICE_ID}")
