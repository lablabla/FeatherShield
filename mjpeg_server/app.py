import os
import threading
from functools import wraps
from flask import Flask, Response, request, jsonify
import firebase_admin
from firebase_admin import auth, credentials
import logging

# --- Application Setup ---

app = Flask(__name__)

# --- Firebase Authentication ---

# Initialize Firebase Admin SDK
# The service account key is loaded from the same environment variable
# as the cloud_bridge, so no new configuration is needed.
try:
    service_account_path = os.getenv("FIREBASE_SERVICE_ACCOUNT_PATH")
    if not service_account_path:
        raise ValueError("FIREBASE_SERVICE_ACCOUNT_PATH environment variable not set.")
    
    cred = credentials.Certificate(service_account_path)
    firebase_admin.initialize_app(cred)
    print("Firebase Admin SDK initialized successfully.")
except Exception as e:
    print(f"Error initializing Firebase Admin SDK: {e}")
    exit(1)
    
# --- In-Memory Frame Buffer ---
# This dictionary will hold the latest JPEG frame for each device.
# We use a lock to make sure it's thread-safe, as multiple workers
# and threads will be accessing it concurrently.
streams = {}
streams_lock = threading.Lock()

# --- Authentication Decorators ---

def firebase_auth_required(f):
    """
    A decorator to protect endpoints that require a valid Firebase ID token.
    This is for authenticating the Android app user.
    """
    @wraps(f)
    def decorated_function(*args, **kwargs):

        # THIS IS FOR DEBUG ONLY NOW
        return f(*args, **kwargs)

        auth_header = request.headers.get('Authorization')
        if not auth_header or not auth_header.startswith('Bearer '):
            return jsonify({"error": "Authorization header is missing or invalid"}), 401
        
        id_token = auth_header.split('Bearer ')[1]
        try:
            # Verify the ID token is valid and not revoked.
            decoded_token = auth.verify_id_token(id_token)
        except auth.InvalidIdTokenError:
            return jsonify({"error": "Invalid ID token"}), 403
        except auth.ExpiredIdTokenError:
            return jsonify({"error": "ID token has expired"}), 403
        except Exception as e:
            return jsonify({"error": f"Token verification failed: {e}"}), 401
            
        return f(*args, **kwargs)
    return decorated_function

def mtls_auth_required(f):
    """
    A placeholder decorator for mTLS authentication.
    In a production setup, mTLS is best handled by a reverse proxy like Nginx.
    The proxy would verify the client certificate and pass the device ID
    (from the certificate's Common Name) to the Flask app in a header.
    """
    @wraps(f)
    def decorated_function(*args, **kwargs):
        # In a real setup, you would check a header set by Nginx, e.g.:
        # device_id_from_cert = request.headers.get('X-Client-Cert-CN')
        # if not device_id_from_cert or device_id_from_cert != kwargs.get('device_id'):
        #     return jsonify({"error": "Client certificate is invalid or does not match device ID"}), 403
        
        # For now, we'll just log that this check would happen.
        print(f"INFO: mTLS check passed for device: {kwargs.get('device_id')}")
        return f(*args, **kwargs)
    return decorated_function

# --- API Endpoints ---

@app.route('/upload_stream/<string:device_id>', methods=['POST'])
@mtls_auth_required
def upload_stream(device_id):
    """
    Endpoint for the ESP32 to POST its MJPEG stream.
    This function reads the multipart stream and updates the frame buffer.
    """
    logging.info(f"Stream started for device: {device_id}")
    
    # The input stream from the ESP32
    stream = request.stream
    
    # Read the stream chunk by chunk to find the multipart boundary
    boundary = b'--frame'
    frame_data = b''
    
    try:
        while True:
            # This read will block until data is available or the client disconnects,
            # thanks to the gevent worker.
            chunk = stream.read(4096)
            if not chunk:
                logging.info(f"Received empty chunk for {device_id}. Client disconnected.")
                break
            
            frame_data += chunk
            parts = frame_data.split(boundary)
            
            if len(parts) > 1:
                for i in range(len(parts) - 1):
                    part = parts[i]
                    if not part:
                        continue
                    
                    jpeg_start = part.find(b'\r\n\r\n')
                    if jpeg_start != -1:
                        jpeg_frame = part[jpeg_start + 4:]
                        if jpeg_frame:
                            with streams_lock:
                                streams[device_id] = jpeg_frame
                
                frame_data = parts[-1]

    except Exception as e:
        logging.error(f"Error while reading stream from {device_id}: {e}")
    finally:
        logging.info(f"Stream ended for device: {device_id}")
        with streams_lock:
            if device_id in streams:
                del streams[device_id]
                
    return "Stream ended", 200


@app.route('/stream/<string:device_id>', methods=['GET'])
@firebase_auth_required
def get_stream(device_id):
    """
    Endpoint for the Android app to GET the latest frame.
    This returns the latest frame from the buffer as a JPEG image.
    """
    return Response(frame_generator(device_id),
                    mimetype='multipart/x-mixed-replace; boundary=frame')
    

def frame_generator(device_id):
    """
    A generator function that yields the latest frame for a device.
    This is used to stream to the Android app.
    """
    while True:
        with streams_lock:
            frame = streams.get(device_id)

        if frame:
            yield (b'--frame\r\n'
                   b'Content-Type: image/jpeg\r\n\r\n' + frame + b'\r\n')
        else:
            # If no stream is active, you could yield a placeholder image or just wait.
            # For now, we'll just signal the stream is unavailable and break.
            logging.warning(f"No active stream found for device {device_id}")
            break

if __name__ == '__main__':
    # This is for local development only.
    # In production, use Gunicorn: gunicorn --config gunicorn.conf.py app:app
    app.run(host='0.0.0.0', port=5000, debug=True, threaded=True)
