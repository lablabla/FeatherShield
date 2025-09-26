#include "websocket.h"

#include "esp_log.h"
#include "esp_camera.h"
#include "esp_websocket_client.h"

#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

// TODO: Move to kconfig
#define WS_URI "ws://192.168.1.84:8000/ws/device/device123"

static const char *TAG = "WEBSOCKET";

static bool ws_disconnected = false;
static bool ws_connecting = false;
static bool ws_running = false;
static esp_websocket_client_handle_t ws_client = NULL;

static void ws_event_handler(void *handler_args, esp_event_base_t base, int32_t event_id, void *event_data)
{
    esp_websocket_event_data_t *data = (esp_websocket_event_data_t *)event_data;
    switch (event_id) {
        case WEBSOCKET_EVENT_CONNECTED:
            ESP_LOGI(TAG, "Websocket connected");
            ws_disconnected = false;
            ws_connecting = false;
            break;
        case WEBSOCKET_EVENT_DISCONNECTED:
            ESP_LOGI(TAG, "Websocket disconnected");
            ws_disconnected = true;
            ws_connecting = false;
            break;
        case WEBSOCKET_EVENT_DATA:
            if (data->op_code == 0x1) { // text
                ESP_LOGI(TAG, "Received text: %.*s", data->data_len, (char*)data->data_ptr);
                // parse JSON and act upon it
            } else if (data->op_code == 0x2) { // binary
                ESP_LOGI(TAG, "Received binary (%d bytes)", data->data_len);
            }
            break;
        case WEBSOCKET_EVENT_CLOSED:
            ESP_LOGI(TAG, "Websocket closed");
            ws_disconnected = true;
            ws_connecting = false;
            break;
        default:
            break;
    }
}

static void ws_task(void *pvParameters)
{
    esp_websocket_client_config_t cfg = {
        .uri = WS_URI,
    };
    ws_connecting = true;
    ws_client = esp_websocket_client_init(&cfg);
    esp_websocket_register_events(ws_client, WEBSOCKET_EVENT_ANY, ws_event_handler, NULL);
    esp_websocket_client_start(ws_client);
    const int chunk_size = 4096;
    ws_running = true;
    while (ws_running) {
        if (esp_websocket_client_is_connected(ws_client)) {
            // Capture frame
            camera_fb_t * fb = esp_camera_fb_get();
            if (!fb) {
                ESP_LOGW(TAG, "Camera capture failed");
                vTaskDelay(pdMS_TO_TICKS(100));
                continue;
            }

            // Send JPEG bytes as binary WS message
            if (fb->format == PIXFORMAT_JPEG) {
                int total_size = fb->len;
                int chunk_count = (total_size + chunk_size - 1) / chunk_size;

                // Send header: "FRAM" + size(2B) + chunk_count(2B)
                uint8_t header[8];
                header[0] = 'F'; header[1] = 'R'; header[2] = 'A'; header[3] = 'M';
                header[4] = (total_size >> 8) & 0xFF;
                header[5] = total_size & 0xFF;
                header[6] = (chunk_count >> 8) & 0xFF;
                header[7] = chunk_count & 0xFF;

                esp_websocket_client_send_bin(ws_client, (const char*)header, sizeof(header), portMAX_DELAY);

                // Send chunks
                for (int offset = 0; offset < total_size; offset += chunk_size) {
                    int to_send = (total_size - offset > chunk_size) ? chunk_size : (total_size - offset);
                    esp_websocket_client_send_bin(ws_client, (const char*)fb->buf + offset, to_send, portMAX_DELAY);
                }
                ESP_LOGI(TAG, "Frame %d bytes sent in %d chunks", total_size, chunk_count);
            } else {
                // If not JPEG, you must convert to JPEG or choose another approach
                ESP_LOGW(TAG, "Frame not JPEG");
            }

            esp_camera_fb_return(fb);
            vTaskDelay(pdMS_TO_TICKS(200)); // ~5 FPS, adjust as needed
        } else {
            ESP_LOGW(TAG, "Not connected, retry in 1s");
            vTaskDelay(pdMS_TO_TICKS(1000));
            if (ws_disconnected && !ws_connecting) {
                esp_websocket_client_start(ws_client);
                ws_connecting = true;
            }
        }
    }

    esp_websocket_client_stop(ws_client);
    esp_websocket_client_destroy(ws_client);
    vTaskDelete(NULL);
}

void websocket_init()
{
    if (!ws_running) {
        xTaskCreate(&ws_task, "ws_task", 8192, NULL, 5, NULL);
    }
}

void websocket_deint()
{
    ws_running = false;
}