# generate_qr.py
import qrcode
import json
import os

DEVICE_ID = "esp32-001"
POP = "unique_pop_string_from_previous_step"

def generate_qr_code(out_dir, device_id, pop):
    data = {
        "version": 1,
        "id": device_id,
        "pop": pop
    }
    json_data = json.dumps(data)
    qr = qrcode.QRCode(version=1, box_size=10, border=4)
    qr.add_data(json_data)
    qr.make(fit=True)
    img = qr.make_image(fill_color="black", back_color="white")
    img.save(f"{out_dir}/{device_id}_qr.png")

if __name__ == "__main__":
    generate_qr_code("../out", DEVICE_ID, POP)