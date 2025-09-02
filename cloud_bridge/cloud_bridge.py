# cloud_bridge/mqtt_to_firebase_bridge.py

import paho.mqtt.client as mqtt
import json
import base64
import ssl
import os
from backends.firebase.firebase_backend import FirebaseBackend

class CloudBridge:
    def __init__(self):
        self.backend_service = FirebaseBackend(self.on_command_callback)

        self.MQTT_BROKER = os.getenv("HOST_IP", "localhost")
        self.MQTT_PORT = int(os.getenv("MQTT_PORT", "1883"))
        self.MQTT_TOPICS = [("nestbox/+/alert", 1), ("nestbox/+/command", 1)]

        self.CA_CERT_PATH = os.getenv("CA_CERT_PATH")
        self.CLIENT_CERT_PATH = os.getenv("CLIENT_CERT_PATH")
        self.CLIENT_KEY_PATH = os.getenv("CLIENT_KEY_PATH")

        self.client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, "mqtt_cloud_bridge_local")
        self.client.tls_set(
            ca_certs=self.CA_CERT_PATH,
            certfile=self.CLIENT_CERT_PATH,
            keyfile=self.CLIENT_KEY_PATH,
            tls_version=ssl.PROTOCOL_TLSv1_2,
            cert_reqs=ssl.CERT_REQUIRED
        )
        self.client.reconnect_delay_set(min_delay=1, max_delay=120)

        self.client.on_connect = self.on_connect
        self.client.on_disconnect = self.on_disconnect
        self.client.on_message = self.on_message

    def on_command_callback(self, command: str, device_id: str):
        print(f"[Cloud Bridge] Command received: {command} for device {device_id}")
        topic = f"nestbox/{device_id}/command"
        print(f"[Cloud Bridge] Publishing to topic: {topic} with payload: {{'command': {command}}}")
        self.client.publish(topic, json.dumps({"command": command}), qos=1)

    def on_connect(self, client, userdata, flags, rc, properties):
        if rc == 0:
            print("Connected to MQTT Broker securely!")
            client.subscribe(self.MQTT_TOPICS)
        else:
            print(f"Failed to connect, return code {rc}")

    def process_alert(self, payload):
        print("Processing alert...")
        device_id = payload.get("device_id")
        image_data_b64 = payload.get("image_data")
        battery_level = payload.get("battery_level")

        if not device_id or not image_data_b64:
            print("Invalid payload received.")
            return

        image_data = base64.b64decode(image_data_b64)
        image_url = self.backend_service.save_alert(device_id, image_data, battery_level)
        print(f"Alert from {device_id} processed. Image URL: {image_url}")
        self.backend_service.send_fcm_notification(device_id, image_url)

    def on_message(self, client, userdata, msg):
        try:
            topic = msg.topic.split('/')
            if not (len(topic) == 3 and topic[0] == "nestbox"):
                print(f"Ignoring message on unexpected topic: {msg.topic}")
                return
            action = topic[2]
            payload = json.loads(msg.payload.decode('utf-8'))
            if action == "alert":
                self.process_alert(payload)
        except Exception as e:
            print(f"Error processing message: {e}")
            print(f"Got message: {msg.payload.decode('utf-8')}")

    def on_disconnect(self, client, userdata, flags, rc, properties):
        if rc != 0:
            print(f"Unexpected disconnection. Attempting to reconnect...")

    def start(self):
        self.client.connect(self.MQTT_BROKER, self.MQTT_PORT, 60)
        self.client.loop_forever()

if __name__ == "__main__":
    bridge = CloudBridge()
    bridge.start()