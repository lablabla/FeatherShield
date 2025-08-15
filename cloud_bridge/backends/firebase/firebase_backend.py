
import firebase_admin
from firebase_admin import credentials, firestore, storage
import uuid
import os

from .backend_interface import BackendService

class FirebaseBackend(BackendService):
    def __init__(self):
        service_account_path = os.getenv("FIREBASE_SERVICE_ACCOUNT_PATH", f"{os.path.dirname(os.path.abspath(__file__))}/feathershield-adminsdk.json")
        storage_bucket = os.getenv("FIREBASE_STORAGE_BUCKET", "feathershield-e401e.firebasestorage.app")
        cred = credentials.Certificate(service_account_path)
        firebase_admin.initialize_app(cred, {'storageBucket': storage_bucket})
        self.db = firestore.client()
        self.bucket = storage.bucket()

    def save_alert(self, device_id: str, image_data: bytes, battery_level: float):
        # 1. Upload the image to Firebase Storage
        filename = f"{device_id}/{uuid.uuid4()}.jpg"
        blob = self.bucket.blob(filename)
        blob.upload_from_string(image_data, content_type='image/jpeg')
        image_url = blob.public_url

        # 2. Store the alert data in Firestore
        self.db.collection("alerts").add({
            "deviceId": device_id,
            "imageUrl": image_url,
            "batteryLevel": battery_level,
            "timestamp": firestore.SERVER_TIMESTAMP
        })
        return image_url

    def listen_for_commands(self, device_id: str, callback):
        # We'll implement this part later, but the concept is to
        # use Firestore's on_snapshot() to listen for changes
        # in a specific document that holds the command state.
        pass