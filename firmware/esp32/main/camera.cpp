#include "esp_err.h"
#include "esp_log.h"
#include "esp_camera.h"
#include <driver/gpio.h>

#include "camera_pins.h"
#include "camera.h"

void camera_init()
{
    
  camera_config_t config = {	
      .pin_pwdn = PWDN_GPIO_NUM,
      .pin_reset = RESET_GPIO_NUM,
	    .pin_xclk = XCLK_GPIO_NUM,
  
      .pin_sccb_sda = SIOD_GPIO_NUM,
      .pin_sccb_scl = SIOC_GPIO_NUM,
      .pin_d7 = Y9_GPIO_NUM,
      .pin_d6 = Y8_GPIO_NUM,
      .pin_d5 = Y7_GPIO_NUM,
      .pin_d4 = Y6_GPIO_NUM,
      .pin_d3 = Y5_GPIO_NUM,
      .pin_d2 = Y4_GPIO_NUM,
      .pin_d1 = Y3_GPIO_NUM,
      .pin_d0 = Y2_GPIO_NUM,
      .pin_vsync = VSYNC_GPIO_NUM,
      .pin_href = HREF_GPIO_NUM,
      .pin_pclk = PCLK_GPIO_NUM,
      //XCLK 20MHz or 10MHz for OV2640 double FPS (Experimental)
      .xclk_freq_hz = 20000000,
      .ledc_timer = LEDC_TIMER_0,
      .ledc_channel = LEDC_CHANNEL_0,
      
      .pixel_format = PIXFORMAT_JPEG, //YUV422,GRAYSCALE,RGB565,JPEG
      .frame_size = FRAMESIZE_VGA,	//QQVGA-UXGA Do not use sizes above QVGA when not JPEG
      
      .jpeg_quality = 10, //0-63 lower number means higher quality
      .fb_count = 2		//if more than one, i2s runs in continuous mode. Use only with JPEG
    };

    ESP_ERROR_CHECK(esp_camera_init(&config));
}

void camera_deinit()
{
    esp_camera_deinit();
}