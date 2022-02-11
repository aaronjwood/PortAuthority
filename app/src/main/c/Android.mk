LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := ipneigh
LOCAL_SRC_FILES := dnet_ntop.c libnetlink.c ipneigh.c ll_map.c ipx_ntop.c mpls_ntop.c
include $(BUILD_SHARED_LIBRARY)
