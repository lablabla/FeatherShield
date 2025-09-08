import os
import logging
import asyncio
from fastapi import FastAPI, Request, HTTPException, Response
from fastapi.responses import StreamingResponse

# --- In-memory storage for video streams ---
streams = {}
# Use asyncio's Lock for safe concurrent access in an async environment
streams_lock = asyncio.Lock()

# --- Logging Configuration ---
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# --- FastAPI App Initialization ---
app = FastAPI()

# --- Authentication (Placeholders) ---
async def verify_device_certificate(request: Request):
    # TODO: Implement mTLS client certificate verification from request headers
    # For now, we'll allow it to pass for testing.
    logger.debug(f"Device connected from {request.client.host}")
    return True

async def verify_firebase_token(token: str):
    # TODO: Implement Firebase JWT verification
    return "test_user"

# --- Streaming Endpoints ---

@app.post("/upload_stream/{device_id}")
async def upload_stream(device_id: str, request: Request):
    """
    Receives and processes a long-lived MJPEG stream from an ESP32.
    FastAPI and Uvicorn are designed to handle this kind of long-lived,
    asynchronous request efficiently.
    """
    if not await verify_device_certificate(request):
        raise HTTPException(status_code=401, detail="Device certificate invalid")

    logger.info(f"Stream connection opened for device: {device_id}")
    
    boundary = b'--frame'
    buffer = b''
    
    try:
        # Asynchronously iterate over the raw request body chunks
        async for chunk in request.stream():
            buffer += chunk
            # Process all full frames found in the buffer
            while boundary in buffer:
                parts = buffer.split(boundary, 1)
                frame_part = parts[0]
                buffer = parts[1]

                # Find the start of the JPEG data
                jpeg_start = frame_part.find(b'\r\n\r\n')
                if jpeg_start != -1:
                    jpeg_frame = frame_part[jpeg_start + 4:]
                    if jpeg_frame:
                        # Store the latest frame
                        async with streams_lock:
                            streams[device_id] = jpeg_frame
                        logger.debug(f"Received frame from {device_id}, size: {len(jpeg_frame)} bytes")

    except Exception as e:
        logger.error(f"Error while reading stream from {device_id}: {e}")
    finally:
        # Clean up when the ESP32 disconnects
        logger.info(f"Stream ended for device: {device_id}. Cleaning up.")
        async with streams_lock:
            if device_id in streams:
                del streams[device_id]
                
    return Response(content="Stream session finished", status_code=200)

async def frame_generator(device_id: str):
    """
    An asynchronous generator that yields the latest frame for a device.
    """
    logger.info(f"Starting frame generator for device: {device_id}")
    try:
        last_frame_sent = None
        while True:
            frame = None
            async with streams_lock:
                frame = streams.get(device_id)

            if frame:
                if frame != last_frame_sent:
                    yield (b'--frame\r\n'
                           b'Content-Type: image/jpeg\r\n\r\n' + frame + b'\r\n')
                    last_frame_sent = frame
            else:
                logger.warning(f"No active stream for device {device_id}. Viewer disconnecting.")
                break
            
            # Use asyncio.sleep to prevent blocking the server's event loop
            await asyncio.sleep(0.1) # ~10 FPS
    finally:
        logger.info(f"Frame generator stopped for device: {device_id}")

@app.get("/stream/{device_id}")
async def stream(device_id: str):
    """
    Serves the MJPEG stream to the Android app.
    """
    # TODO: Add Firebase authentication check here using a dependency
    
    return StreamingResponse(frame_generator(device_id),
                             media_type='multipart/x-mixed-replace; boundary=frame')

