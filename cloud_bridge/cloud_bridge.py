# cloud_bridge/mqtt_to_firebase_bridge.py
import paho.mqtt.client as mqtt
import json
import base64
import os

from backends.firebase.firebase_backend import FirebaseBackend

# Load Firebase credentials and initialize the backend service

backend_service = FirebaseBackend()

# MQTT Settings
MQTT_BROKER = "localhost"
MQTT_PORT = 1883
MQTT_TOPIC = "nestbox/+/alert"

def on_connect(client, userdata, flags, rc, properties):
    print(f"Connected to MQTT Broker with result code {rc}")
    client.subscribe(MQTT_TOPIC)

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

    except Exception as e:
        print(f"Error processing message: {e}")

client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, "mqtt_cloud_bridge")
client.on_connect = on_connect
client.on_message = on_message

client.connect(MQTT_BROKER, MQTT_PORT, 60)
client.loop_forever()