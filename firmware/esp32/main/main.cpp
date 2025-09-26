#include <stdio.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

#include "esp_system.h"
#include "esp_camera.h"
#include "esp_pm.h"
#include <esp_log.h>
#include <esp_wifi.h>
#include <esp_event.h>
#include <nvs_flash.h>

#include "io.h"
#include "camera.h"
#include "mqtt.h"
#include "provisioning.h"
#include "utils.h"
#include "websocket.h"

const char *TAG = "MAIN";

void on_movement_detected()
{
    ESP_LOGI(TAG, "Movement detected!");
    mqtt_publish_detection();
}

extern "C"
void app_main(void) {
    // esp_log_level_set("*", ESP_LOG_WARN);
    printf("FeatherShield ESP32 project started!\n");

        /* Initialize NVS partition */
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        /* NVS partition was truncated
         * and needs to be erased */
        ESP_ERROR_CHECK(nvs_flash_erase());

        /* Retry nvs_flash_init */
        ESP_ERROR_CHECK(nvs_flash_init());
    }

    /* Initialize TCP/IP */
    ESP_ERROR_CHECK(esp_netif_init());

    /* Initialize the event loop */
    ESP_ERROR_CHECK(esp_event_loop_create_default());

    /* Start provisioning service */
    
    nvs_handle nvs_handle;
    ESP_ERROR_CHECK(nvs_open("info", NVS_READONLY, &nvs_handle) != ESP_OK);
    char *device_id = nvs_load_value_if_exist(nvs_handle, "device_id");
    ESP_LOGI(TAG, "Device ID: %s", device_id);

    provision(device_id);

    printf("Wi-Fi connected!\n");

    io_init(on_movement_detected);

    mqtt_start(device_id);    

    camera_init();

    esp_pm_config_t pm_config = {
        .max_freq_mhz = 160,
        .min_freq_mhz = 80,
        .light_sleep_enable = true
    };
    ESP_ERROR_CHECK(esp_pm_configure(&pm_config));
    ESP_LOGI(TAG, "Power management configured (auto light-sleep enabled)");

    // xTaskCreate(light_sleep_task, "light_sleep_task", 4096, NULL, 5, NULL);

}
