import os
import threading
from functools import wraps
from flask import Flask, Response, request, jsonify
import firebase_admin
from firebase_admin import auth, credentials

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
frame_buffer = {}
buffer_lock = threading.Lock()

# --- Authentication Decorators ---

def firebase_auth_required(f):
    """
    A decorator to protect endpoints that require a valid Firebase ID token.
    This is for authenticating the Android app user.
    """
    @wraps(f)
    def decorated_function(*args, **kwargs):
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
    print(f"Stream started for device: {device_id}")
    boundary = b'--frame'
    
    stream = request.stream
    buffer = b''
    
    try:
        while True:
            # Read a chunk from the input stream
            chunk = stream.read(1024)
            if not chunk:
                break # Stream ended
            
            buffer += chunk
            
            # Check if we have a full frame
            start = buffer.find(boundary)
            if start != -1:
                end = buffer.find(boundary, start + len(boundary))
                if end != -1:
                    frame_data = buffer[start + len(boundary):end]
                    buffer = buffer[end:]
                    
                    # Clean up the frame data (remove headers)
                    header_end = frame_data.find(b'\r\n\r\n')
                    if header_end != -1:
                        jpeg_data = frame_data[header_end + 4:]
                        
                        # Update the global frame buffer
                        with buffer_lock:
                            frame_buffer[device_id] = jpeg_data
    except Exception as e:
        print(f"Error while processing stream for {device_id}: {e}")
    finally:
        # When the stream ends, remove the device from the buffer
        with buffer_lock:
            if device_id in frame_buffer:
                del frame_buffer[device_id]
        print(f"Stream ended for device: {device_id}")
        
    return Response("Stream ended", status=200)


@app.route('/stream/<string:device_id>', methods=['GET'])
@firebase_auth_required
def get_stream(device_id):
    """
    Endpoint for the Android app to GET the latest frame.
    This returns the latest frame from the buffer as a JPEG image.
    """
    with buffer_lock:
        frame = frame_buffer.get(device_id)
        
    if frame:
        return Response(frame, mimetype='image/jpeg')
    else:
        # You could return a placeholder image or a 404
        return Response("Stream not available for this device.", status=404)

if __name__ == '__main__':
    # This is for local development only.
    # In production, use Gunicorn: gunicorn --config gunicorn.conf.py app:app
    app.run(host='0.0.0.0', port=5000, debug=True, threaded=True)
