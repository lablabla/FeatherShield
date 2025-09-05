
#include "esp_log.h"
#include "esp_event.h"
#include "esp_netif.h"
#include <nvs_flash.h>
#include "mqtt_client.h"

#include <stdlib.h>
#include "mqtt.h"
#include "nvs.h"
#include "utils.h"

static const char *TAG = "MQTT";

static char* private_key = NULL;
static char* certificate = NULL;
static char* ca_cert = NULL;

static void log_error_if_nonzero(const char *message, int error_code)
{
    if (error_code != 0) {
        ESP_LOGE(TAG, "Last error %s: 0x%x", message, error_code);
    }
}

static void mqtt_event_handler(void *handler_args, esp_event_base_t base, int32_t event_id, void *event_data)
{
    ESP_LOGD(TAG, "Event dispatched from event loop base=%s, event_id=%" PRIi32, base, event_id);
    esp_mqtt_event_handle_t event = (esp_mqtt_event_handle_t)event_data;
    esp_mqtt_client_handle_t client = event->client;
    int msg_id;
    switch ((esp_mqtt_event_id_t)event_id) {
    case MQTT_EVENT_CONNECTED:
        ESP_LOGI(TAG, "MQTT_EVENT_CONNECTED");
        msg_id = esp_mqtt_client_subscribe(client, "/topic/qos0", 0);
        ESP_LOGI(TAG, "sent subscribe successful, msg_id=%d", msg_id);

        msg_id = esp_mqtt_client_subscribe(client, "/topic/qos1", 1);
        ESP_LOGI(TAG, "sent subscribe successful, msg_id=%d", msg_id);

        msg_id = esp_mqtt_client_unsubscribe(client, "/topic/qos1");
        ESP_LOGI(TAG, "sent unsubscribe successful, msg_id=%d", msg_id);
        break;
    case MQTT_EVENT_DISCONNECTED:
        ESP_LOGI(TAG, "MQTT_EVENT_DISCONNECTED");
        break;

    case MQTT_EVENT_SUBSCRIBED:
        ESP_LOGI(TAG, "MQTT_EVENT_SUBSCRIBED, msg_id=%d", event->msg_id);
        msg_id = esp_mqtt_client_publish(client, "/topic/qos0", "data", 0, 0, 0);
        ESP_LOGI(TAG, "sent publish successful, msg_id=%d", msg_id);
        break;
    case MQTT_EVENT_UNSUBSCRIBED:
        ESP_LOGI(TAG, "MQTT_EVENT_UNSUBSCRIBED, msg_id=%d", event->msg_id);
        break;
    case MQTT_EVENT_PUBLISHED:
        ESP_LOGI(TAG, "MQTT_EVENT_PUBLISHED, msg_id=%d", event->msg_id);
        break;
    case MQTT_EVENT_DATA:
        ESP_LOGI(TAG, "MQTT_EVENT_DATA");
        printf("TOPIC=%.*s\r\n", event->topic_len, event->topic);
        printf("DATA=%.*s\r\n", event->data_len, event->data);
        break;
    case MQTT_EVENT_ERROR:
        ESP_LOGI(TAG, "MQTT_EVENT_ERROR");
        if (event->error_handle->error_type == MQTT_ERROR_TYPE_TCP_TRANSPORT) {
            log_error_if_nonzero("reported from esp-tls", event->error_handle->esp_tls_last_esp_err);
            log_error_if_nonzero("reported from tls stack", event->error_handle->esp_tls_stack_err);
            log_error_if_nonzero("captured as transport's socket errno",  event->error_handle->esp_transport_sock_errno);
            ESP_LOGI(TAG, "Last errno string (%s)", strerror(event->error_handle->esp_transport_sock_errno));

        }
        break;
    default:
        ESP_LOGI(TAG, "Other event id:%d", event->event_id);
        break;
    }
}


void mqtt_start(void)
{
    ESP_LOGI(TAG, "Starting MQTT client...");
    if (strlen(CONFIG_FEATHERSHIELD_BROKER_URL) == 0) {
        ESP_LOGE(TAG, "MQTT broker URL not set. Please set it in menuconfig.");
        return;
    }
    nvs_handle handle;
    ESP_ERROR_CHECK(nvs_open("certs", NVS_READONLY, &handle) != ESP_OK);
    
    private_key = nvs_load_value_if_exist(handle, "client_key");
    certificate = nvs_load_value_if_exist(handle, "client_cert");
    ca_cert = nvs_load_value_if_exist(handle, "ca_cert");

    nvs_close(handle);

    esp_mqtt_client_config_t mqtt_cfg = {};
    mqtt_cfg.broker.address.uri = CONFIG_FEATHERSHIELD_BROKER_URL;
    mqtt_cfg.broker.verification.certificate = (const char *)ca_cert;
    mqtt_cfg.credentials.authentication.certificate = (const char *)certificate;
    mqtt_cfg.credentials.authentication.key = (const char *)private_key;

    esp_mqtt_client_handle_t client = esp_mqtt_client_init(&mqtt_cfg);
    
    esp_mqtt_client_register_event(client, MQTT_EVENT_ANY, mqtt_event_handler, NULL);
    esp_mqtt_client_start(client);
}

void mqtt_stop(void)
{
    ESP_LOGI(TAG, "Stopping MQTT client...");

    free(private_key);
    free(certificate);
    free(ca_cert);
}