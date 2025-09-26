#ifndef __SENSORS_H__
#define __SENSORS_H__

typedef void (* on_movement_detected_callback_t)(void);

void io_init(on_movement_detected_callback_t callback);
void io_deinit();

#endif // __SENSORS_H__