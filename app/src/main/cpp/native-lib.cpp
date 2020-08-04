#include <jni.h>
#include <string>

//1.声明 2.实现
extern "C" {
    extern int bspatch_main(int argc,char * argv[]);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_sty_ne_bsdiff_MainActivity_doPatchNative(JNIEnv *env, jobject thiz, jstring old_apk_path_,
                                                  jstring new_apk_path_, jstring patch_path_) {
    const char* old_apk_path = env->GetStringUTFChars(old_apk_path_, 0);
    const char* new_apk_path = env->GetStringUTFChars(new_apk_path_, 0);
    const char* patch_path = env->GetStringUTFChars(patch_path_, 0);

    // ./bspatch oldfile newfile patchfile
    char* argv[4] = {
            "bspatch",
            const_cast<char *>(old_apk_path),
            const_cast<char *>(new_apk_path),
            const_cast<char *>(patch_path)
    };

    bspatch_main(4, argv);

    env->ReleaseStringUTFChars(old_apk_path_, old_apk_path);
    env->ReleaseStringUTFChars(new_apk_path_, new_apk_path);
    env->ReleaseStringUTFChars(patch_path_, patch_path);
}