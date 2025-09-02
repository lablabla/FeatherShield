#include <stdio.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_system.h"

extern "C"
void app_main(void) {
    printf("FeatherShield ESP32 project started!\n");
    while (1) {
        vTaskDelay(1000 / portTICK_PERIOD_MS);
    }
}
