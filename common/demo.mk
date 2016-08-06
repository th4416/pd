########
# demo
#

TOP := ..
-include $(TOP)/build/env.mk
include $(TOP)/build/f.mk

include $(TOP)/build/clear_local_var.mk

LOCAL_DEP_PKG_S := t.typedef.droid
LOCAL_DEP_JAR_F := $(TOP)/common/out/$(LOCAL_DEP_PKG_S).jar
LOCAL_DEP_RES_D := $(TOP)/common/out/res

LOCAL_DEP_JAR_F += $(TOP)/../typedef/out/typedef.jar

LOCAL_SRC_D := ./demo/src
LOCAL_RES_D := ./demo/res
LOCAL_AMF_F := ./demo/AndroidManifest.xml
LOCAL_OUT_D := ./out/demo
include $(TOP)/build/build_apk.mk
