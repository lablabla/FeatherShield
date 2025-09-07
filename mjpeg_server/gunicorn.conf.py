# gunicorn.conf.py
# Configuration file for running the Flask app with Gunicorn.

# The socket to bind to.
# '0.0.0.0' makes the server accessible from any IP address.
bind = "0.0.0.0:5000"

# Number of worker processes. A good starting point is (2 x $num_cores) + 1.
# For a small GCE instance, 3 is a reasonable number.
workers = 3

# Use threaded workers. This is CRITICAL for our use case.
# It allows each worker to handle multiple connections at once,
# so a long-lived stream from an ESP32 doesn't block requests
# from the Android app.
worker_class = "gthread"
threads = 4

# Log level
loglevel = "info"

# Log file paths
accesslog = "-"  # Log to stdout
errorlog = "-"   # Log to stderr
