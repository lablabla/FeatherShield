from abc import ABC, abstractmethod

class BackendService(ABC):

    def __init__(self, on_command_callback):
        self._on_command_callback = on_command_callback

    @abstractmethod
    def save_alert(self, device_id: str, image_data: bytes, battery_level: float):
        """Saves an alert to the backend, including the image and metadata."""
        pass

    @abstractmethod
    def listen_for_commands(self, device_id: str, callback):
        """Listens for commands and executes a callback function."""
        pass