
import firebase_admin
from firebase_admin import credentials, firestore, storage, messaging
import uuid
import os

from .backend_interface import BackendService

class FirebaseBackend(BackendService):
    def __init__(self):
        service_account_path = os.getenv("FIREBASE_SERVICE_ACCOUNT_PATH", None)
        if not service_account_path:
            raise ValueError("FIREBASE_SERVICE_ACCOUNT_PATH environment variable must be set.")
        storage_bucket = os.getenv("FIREBASE_STORAGE_BUCKET", None)
        if not storage_bucket:
            raise ValueError("FIREBASE_STORAGE_BUCKET environment variable must be set.")
        cred = credentials.Certificate(service_account_path)
        firebase_admin.initialize_app(cred, {'storageBucket': storage_bucket})
        self.db = firestore.client()
        self.bucket = storage.bucket()

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

    def listen_for_commands(self, device_id: str, callback):
        # We'll implement this part later, but the concept is to
        # use Firestore's on_snapshot() to listen for changes
        # in a specific document that holds the command state.
        pass