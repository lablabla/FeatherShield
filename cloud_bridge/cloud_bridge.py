# cloud_bridge/mqtt_to_firebase_bridge.py
import paho.mqtt.client as mqtt
import json
import base64
import ssl
import os

from backends.firebase.firebase_backend import FirebaseBackend

# Load Firebase credentials and initialize the backend service

backend_service = FirebaseBackend()

# Get MQTT settings from environment variables
MQTT_BROKER = os.getenv("HOST_IP", "localhost")
MQTT_PORT = int(os.getenv("MQTT_PORT", "1883"))
MQTT_TOPIC = os.getenv("MQTT_TOPIC", "nestbox/+/alert")

# Get certificate paths from environment variables
CA_CERT_PATH = os.getenv("CA_CERT_PATH")
CLIENT_CERT_PATH = os.getenv("CLIENT_CERT_PATH")
CLIENT_KEY_PATH = os.getenv("CLIENT_KEY_PATH")

def on_connect(client, userdata, flags, rc, properties):
    if rc == 0:
        print("Connected to MQTT Broker securely!")
        client.subscribe(MQTT_TOPIC)
    else:
        print(f"Failed to connect, return code {rc}")

def on_message(client, userdata, msg):
    try:
        payload = json.loads(msg.payload.decode('utf-8'))
        
        device_id = payload.get("device_id")
        image_data_b64 = payload.get("image_data")
        battery_level = payload.get("battery_level")

        if not device_id or not image_data_b64:
            print("Invalid payload received.")
            return

        image_data = base64.b64decode(image_data_b64)
        
        # Use the abstract backend service to save the alert
        image_url = backend_service.save_alert(device_id, image_data, battery_level)
        print(f"Alert from {device_id} processed. Image URL: {image_url}")

        backend_service.send_fcm_notification(device_id, image_url)

    except Exception as e:
        print(f"Error processing message: {e}")
        print(f"Got message: {msg.payload.decode('utf-8')}")
    
def on_disconnect(client, userdata, rc):
    if rc != 0:
        print(f"Unexpected disconnection. Attempting to reconnect...")

client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, "mqtt_cloud_bridge")

client.tls_set(
    ca_certs=CA_CERT_PATH,
    certfile=CLIENT_CERT_PATH,
    keyfile=CLIENT_KEY_PATH,
    tls_version=ssl.PROTOCOL_TLSv1_2,
    cert_reqs=ssl.CERT_REQUIRED
)
client.reconnect_delay_set(min_delay=1, max_delay=120)

client.on_connect = on_connect
client.on_disconnect = on_disconnect
client.on_message = on_message

client.connect(MQTT_BROKER, MQTT_PORT, 60)
client.loop_forever()