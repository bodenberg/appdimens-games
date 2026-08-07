#include "appdimens_games.h"
#include <jni.h>
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM*,void*){return JNI_VERSION_1_6;}
extern "C" JNIEXPORT jint JNICALL Java_io_github_bodenberg_appdimens_games_android_NativeEngine_nativeAbiVersion(JNIEnv*,jclass){return (jint)adg_abi_version();}
extern "C" JNIEXPORT jint JNICALL Java_io_github_bodenberg_appdimens_games_android_NativeEngine_nativeScaleBatch(JNIEnv* env,jclass,jfloatArray values,jint strategy,jfloat w,jfloat h,jfloat density){
 if(!values)return ADG_NULL_ARGUMENT;
 jsize count=env->GetArrayLength(values);
 jboolean copy=JNI_FALSE;
 jfloat* data=env->GetFloatArrayElements(values,&copy);
 if(!data)return ADG_NULL_ARGUMENT;
 adg_screen screen{w,h,density,{0,0,0,0}};
 adg_config config=adg_default_config();
 adg_status status=adg_scale_batch(data,0,data,0,(size_t)count,(adg_strategy)strategy,&screen,&config);
 env->ReleaseFloatArrayElements(values,data,0);
 return status;
}
