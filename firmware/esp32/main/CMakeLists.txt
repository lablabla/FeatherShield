idf_component_register(SRCS 
  "camera.cpp"
  "io.cpp"
  "main.cpp"
  "mqtt.cpp"
  "provisioning.cpp"
  "utils.cpp"
  "websocket.cpp"

   INCLUDE_DIRS "." REQUIRES wifi_provisioning protocomm mqtt json nvs_flash)

set(EXTRA_COMPONENT_DIRS
  "components/espp/components"
)
