
#include "freertos/FreeRTOS.h"
#include "freertos/idf_additions.h"
#include "freertos/task.h"
#include "freertos/queue.h"
#include "driver/gpio.h"

#include "io.h"

static QueueHandle_t interputQueue;
static constexpr gpio_num_t SENSOR_GPIO = GPIO_NUM_34; // TODO: change to actual sensor GPIO
static bool running = false;

static on_movement_detected_callback_t callback = nullptr;

static void IRAM_ATTR gpio_isr_handler(void* arg)
{
    uint32_t gpioNum = (uint32_t) arg;
    xQueueSendFromISR(interputQueue, &gpioNum, NULL);
}

static void gpio_task_function(void* arg)
{
    gpio_num_t ioNum;
    while (running) {
        if (xQueueReceive(interputQueue, &ioNum, portMAX_DELAY)) {
            printf("GPIO[%"PRIu32"] intr, val: %d\n", static_cast<uint32_t>(ioNum), gpio_get_level(ioNum));
            if (callback) {
                callback();
            }
        }
    }
}

void io_init(on_movement_detected_callback_t callback)
{
    ::callback = callback;
    gpio_config_t ioConf = {
        .pin_bit_mask = (1ULL << SENSOR_GPIO),
        .mode = GPIO_MODE_INPUT,
        .pull_up_en = GPIO_PULLUP_DISABLE,
        .pull_down_en = GPIO_PULLDOWN_DISABLE,
        .intr_type = GPIO_INTR_POSEDGE,
    };
    gpio_config(&ioConf);
    interputQueue = xQueueCreate(10, sizeof(uint32_t));
    running = true;
    xTaskCreate(gpio_task_function, "gpio_task_function", 2048, NULL, 10, NULL);
    gpio_install_isr_service(0);
    gpio_isr_handler_add(SENSOR_GPIO, gpio_isr_handler, (void*) SENSOR_GPIO);
}

void io_deinit()
{
    running = false;
    gpio_isr_handler_remove(SENSOR_GPIO);
    gpio_uninstall_isr_service();
    vQueueDelete(interputQueue);
}