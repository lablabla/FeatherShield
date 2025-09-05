
#include "esp_log.h"
#include "esp_check.h"

#include "utils.h"

static const char *TAG = "UTILS";

char * nvs_load_value_if_exist(nvs_handle handle, const char* key)
{
    // Try to get the size of the item
    size_t value_size;
    esp_err_t err = nvs_get_str(handle, key, NULL, &value_size);
    if(err != ESP_OK){
        ESP_LOGE(TAG, "Failed to get size of key: %s, error: %d, %s\n", key, err, esp_err_to_name(err));
        return NULL;
    }
    ESP_LOGI(TAG, "Key %s has size %zu", key, value_size);

    char* value = (char*)malloc(value_size);
    if (!value) {
        ESP_LOGE(TAG, "Failed to allocate memory for key: %s", key);
        return NULL;
    }
    if(nvs_get_str(handle, key, value, &value_size) != ESP_OK){
        ESP_LOGE(TAG, "Failed to load key: %s", key);
        free(value);
        return NULL;
    }
    ESP_LOGI(TAG, "Loaded key %s", key);

    return value;
}