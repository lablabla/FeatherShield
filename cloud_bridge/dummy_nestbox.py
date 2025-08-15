import paho.mqtt.client as mqtt
import json
import base64
import random
import time

# MQTT Settings
MQTT_BROKER = "localhost"
MQTT_PORT = 1883
DEVICE_ID = "nestbox_001"
MQTT_TOPIC = f"nestbox/{DEVICE_ID}/alert"

def create_dummy_image_b64():
    """Generates a small, dummy Base64-encoded JPEG image."""
    # A tiny JPEG image (1x1 pixel, red) as a placeholder.
    # This avoids dealing with actual image files for a simple test.
    img_data = b'\xff\xd8\xff\xe0\x00\x10JFIF\x00\x01\x01\x00\x00\x01\x00\x01\x00\x00\xff\xdb\x00C\x00\x08\x06\x06\x07\x06\x05\x08\x07\x07\x07\t\t\x08\n\x0c\x14\r\x0c\x0b\x0b\x0c\x19\x12\x13\x0f\x14\x1d\x1a\x1f\x1e\x1d\x1a\x16\x16\x1d"!\x11\x18!""\xf0\xf0\xf0\xf0\xf0\xf0\xf0\xf0\xf0\xf0\xff\xc0\x00\x11\x08\x00\x01\x00\x01\x01\x01\x11\x00\xff\xc4\x00\x14\x00\x01\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\xff\xda\x00\x08\x01\x01\x00\x00\x3f\x00\xd2\xcf \xff\xd9'
    return base64.b64encode(img_data).decode('utf-8')

def on_connect(client, userdata, flags, rc, properties):
    if rc == 0:
        print("Test client connected to MQTT Broker!")
    else:
        print(f"Failed to connect, return code {rc}")

def main():
    client_id = f'python-pub-{random.randint(0, 1000)}'
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id)
    client.on_connect = on_connect
    
    try:
        client.connect(MQTT_BROKER, MQTT_PORT, 60)
    except Exception as e:
        print(f"Could not connect to broker: {e}")
        return

    client.loop_start()

    for _ in range(2):  # Publish 2 test messages
        # Create a dummy payload
        payload = {
            "device_id": DEVICE_ID,
            "image_data": create_dummy_image_b64(),
            "battery_level": round(random.uniform(3.5, 4.2), 2)
        }
        json_payload = json.dumps(payload)

        # Publish the message to the topic
        client.publish(MQTT_TOPIC, json_payload)
        print(f"Published message to {MQTT_TOPIC}")
        
        time.sleep(5)

if __name__ == "__main__":
    main()