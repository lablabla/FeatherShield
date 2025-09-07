# Gunicorn configuration file

# --- Server Socket ---
# Bind to all network interfaces on port 5000. Gunicorn will run on this port.
# A reverse proxy like Nginx will forward requests from port 80/443 to this.
bind = "0.0.0.0:5000"

# --- Worker Processes ---
# Use the gevent worker class for asynchronous I/O. This is essential for
# handling long-lived streaming connections without blocking other requests.
worker_class = "gevent"

# The number of worker processes. A good starting point is (2 x $num_cores) + 1.
workers = 3

# The number of threads per worker. When using gevent, this is typically 1.
threads = 1

# --- Timeouts ---
# Set a long timeout to allow for persistent streaming connections from the ESP32.
# Value is in seconds. 10 minutes should be more than enough.
timeout = 600

# --- Logging ---
# Log to stdout and stderr so that systemd can capture the output.
accesslog = "-"
errorlog = "-"

