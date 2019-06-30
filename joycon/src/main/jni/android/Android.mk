LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := joycon
LOCAL_SRC_FILES := \
    joycon_jni.c   \
    joycon.c                        \
    joycon_report.c

LOCAL_LDLIBS := -llog -landroid


include $(BUILD_SHARED_LIBRARY)
