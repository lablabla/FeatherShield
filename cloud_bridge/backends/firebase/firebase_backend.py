
import firebase_admin
from firebase_admin import credentials, firestore, storage, messaging
import uuid
import os
from threading import Thread
import time
from google.cloud.firestore_v1.watch import ChangeType
import requests
from .backend_interface import BackendService

class FirebaseBackend(BackendService):
    def __init__(self, on_command_callback):
        super().__init__(on_command_callback)
        service_account_path = os.getenv("FIREBASE_SERVICE_ACCOUNT_PATH", None)
        if not service_account_path:
            raise ValueError("FIREBASE_SERVICE_ACCOUNT_PATH environment variable must be set.")
        storage_bucket = os.getenv("FIREBASE_STORAGE_BUCKET", None)
        if not storage_bucket:
            raise ValueError("FIREBASE_STORAGE_BUCKET environment variable must be set.")
        self.host_ip = os.getenv("HOST_IP", None)
        if not self.host_ip:
            raise ValueError("HOST_IP environment variable must be set.")
        self.mtxmedia_url = os.getenv("MTXMEDIA_URL", None)
        if not self.mtxmedia_url:
            raise ValueError("MTXMEDIA_URL environment variable must be set.")
        cred = credentials.Certificate(service_account_path)
        firebase_admin.initialize_app(cred, {'storageBucket': storage_bucket})
        self.db = firestore.client()
        self.bucket = storage.bucket()
        self.devices = []        
        self.listen_for_commands()

    def save_alert(self, device_id: str, image_data: bytes, battery_level: float):
        # 1. Upload the image to Firebase Storage
        filename = f"{device_id}/{uuid.uuid4()}.jpg"
        blob = self.bucket.blob(filename)
        blob.upload_from_string(image_data, content_type='image/jpeg')
        image_url = filename

        data = {
            "id": device_id,
            "batteryLevel": battery_level,
            "lastImageUrl": image_url,
            "liveStreamUrl": f'rtsp://{self.host_ip}/live/{device_id}', 
            "lastUpdated": firestore.SERVER_TIMESTAMP,
            "name": f"Nest Box {device_id}"
        }
        # 2. Store the alert data in Firestore
        self.db.collection("devices").document(device_id).set(
            data
        )
        return image_url
    
    def send_fcm_notification(self, device_id: str, image_url: str):
        """Sends a push notification to the specified device."""
        # The topic is a unique identifier for the device, which the Android app will subscribe to.
        topic = f'alerts_{device_id}'
        
        # Create the message payload
        message = messaging.Message(
            notification=messaging.Notification(
                title="Myna Bird Detected!",
                body=f"A myna bird with eggs was detected in nest box {device_id}."
            ),
            data={
                "image_url": image_url,
                "device_id": device_id
            },
            topic=topic
        )
        
        # Send the message
        try:
            response = messaging.send(message)
            print(f"Successfully sent message to topic {topic}. Response: {response}")
        except Exception as e:
            print(f"Error sending message: {e}")

    def listen_for_commands(self):
        # Get all device IDs from the Firestore collection
        devices_ref = self.db.collection("devices")
        devices_stream = devices_ref.stream()
        self.devices = [doc.id for doc in devices_stream]
        print(f"Listening for commands for devices: {self.devices}")
        # Create a thread to run the listener to avoid blocking the main thread
        listener_thread = Thread(target=self._run_listener)
        listener_thread.daemon = True
        listener_thread.start()

    def _run_listener(self):

        # A simple, single-document listener
        def on_snapshot(doc_snapshot, changes, read_time):
            if len(doc_snapshot) == 0:
                return
            try:
                id = doc_snapshot[0].reference.path.split('/')[-1]
            except Exception as e:
                print(f"Error getting document ID: {e}")
                return
            for change in changes:
                if change.type == ChangeType.MODIFIED:
                    command_data = change.document.to_dict()
                    if command_data and 'command' in command_data:
                        command = command_data['command']
                        print(f"[Commands listener] Received command for {id}: {command}")
                        if command == "start_stream":
                            self.start_stream(id)
                        elif command == "stop_stream":
                            self.stop_stream(id)
                        elif command == "update_firmware":
                            self.update_firmware(id)

        
        for device_id in self.devices:
            doc_ref = self.db.collection("commands").document(device_id)
            doc_ref.on_snapshot(on_snapshot)
        while True:
            time.sleep(1)

    def call_command_callback(self, command: str, device_id: str):
        if self._on_command_callback:
            self._on_command_callback(command, device_id)

    def start_stream(self, device_id: str):
        print(f"Starting stream for device {device_id}.")
        self.call_command_callback("start_stream", device_id)
        path = f"{self.mtxmedia_url}/v3/config/paths/add/live/{device_id}"
        data = { "name": f"live/{device_id}" }
        try:
            response = requests.post(path, json=data)
            if response.status_code == 200:
                print(f"Stream started successfully for device {device_id}.")
            else:
                print(f"Failed to start stream for device {device_id}. Status code: {response.status_code}, Response: {response.text}")
        except requests.RequestException as e:
            print(f"Error starting stream for device {device_id}: {e}")

    def stop_stream(self, device_id: str):
        print(f"Stopping stream for device {device_id}.")
        self.call_command_callback("stop_stream", device_id)
        path = f"{self.mtxmedia_url}/v3/config/paths/delete/live/{device_id}"
        try:
            response = requests.delete(path)
            if response.status_code == 200:
                print(f"Stream stopped successfully for device {device_id}.")
            else:
                print(f"Failed to stop stream for device {device_id}. Status code: {response.status_code}, Response: {response.text}")
        except requests.RequestException as e:
            print(f"Error stopping stream for device {device_id}: {e}")

    def update_firmware(self, device_id: str):
        print(f"Updating firmware for device {device_id}.")
        self.call_command_callback("update_firmware", device_id)