LOCAL_PATH:= $(call my-dir)

ifneq ($(TARGET_SIMULATOR),true)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, java)
LOCAL_SRC_FILES += \
		   aidl/ru0xdc/sfts/service/IOemRawResultListener.aidl \
		   aidl/ru0xdc/sfts/service/ISamsungServiceMode.aidl

LOCAL_PACKAGE_NAME := SamsungFieldTestService
LOCAL_JAVA_LIBRARIES := telephony-common
LOCAL_AIDL_INCLUDES += $(LOCAL_PATH)/aidl $(LOCAL_PATH)/java
LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

# Build the test package
include $(call all-makefiles-under,$(LOCAL_PATH))

endif # TARGET_SIMULATOR
