idf_component_register(SRCS "main.cpp" INCLUDE_DIRS "." REQUIRES wifi_provisioning protocomm mqtt json)

set(EXTRA_COMPONENT_DIRS
  "components/espp/components"
)
