#ifndef __UTILS_H__
#define __UTILS_H__

#include <nvs_flash.h>

char * nvs_load_value_if_exist(nvs_handle handle, const char* key);

#endif // __UTILS_H__