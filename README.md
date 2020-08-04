# `BSDiff`实现增量更新

[TOC]

## 一、增量更新

### 1.1 增量更新的好处

* 对用户来说，省流量，下载时间快，提升用户体验；
* 对服务器来说，减少服务器带宽压力。

### 1.2 `BSDiff`算法

`BSDiff`是一个差量更新算法，它在服务器端运行`BSDiff`算法产生`patch`包，在客户端运行`BSPatch`算法，将旧文件和`patch`包合并成新文件。

#### 1.2.1 差量更新算法的核心思想

尽可能多地利用`old`文件中已有的内容，尽可能少地加入新的内容来构建`new`文件。通常的做法是对`old`文件和`new`文件做子字符串匹配或使用`Hash`技术，提取公共部分，将`new`文件中剩余的部分打包成`patch`包；在`patch`阶段中，用`copying`和`insertion`两个基本操作即可将`old`文件和`patch`包合成`new`文件。

#### 1.2.2 `BSDiff`算法的改进

`Insertion`操作会引起大量的指针变动和修改，要记录这些值才能在`patch`阶段给修改过的区域重新定位，由于这些指针控制字段必须在`BSDiff`阶段加入`patch`包，产生的`patch`包会较大。`BSDiff`通过引入`diff string`的概念，大大减少了要记录的指针控制字的数目，从而使`patch`包更小。

![image](https://github.com/tianyalu/NeBsdiff/raw/master/show/diff_string.png)

#### 1.2.3 `BSDiff`算法基本步骤

> 1. 对`old`文件中所有子字符串形成一个字典；
> 2. 对比`old`文件和`new`文件，产生`diff string`和`extra string`；
> 3. 将`diff string`和`extra string`以及相应的控制字用`zip`压缩成一个`patch`包。

**步骤1**是所有差量更新算法的瓶颈，时间复杂度为`O(nlogn)`，空间复杂度为`O(n)`，`n`为`old`文件的长度。`BSDiff`采用`Faster suffix sorting`方法获得一个字典序，使用了类似于快速排序的二分思想，使用了`bucket`，`I`，`V`三个辅助数组。最终获得到一个数组`I`，记录了以前缀分组的各个字符串组的最后一个字符串在`old`中的开始位置。

**步骤2**是`BSDiff`产生`patch`包的核心部分，详细描述如下：

![image](https://github.com/tianyalu/NeBsdiff/raw/master/show/bsdiff1.png)  

![image](https://github.com/tianyalu/NeBsdiff/raw/master/show/bsdiff2.png)  

![image](https://github.com/tianyalu/NeBsdiff/raw/master/show/bsdiff3.png)  

**步骤3**将`diff string`和`extra string`以及相应的控制字用`zip`压缩成一个`patch`包。 

![image](https://github.com/tianyalu/NeBsdiff/raw/master/show/bsdiff4.png)  

可以看出在用`zip`压缩之前的`patch`包时没有节约任何字符的，但`diff strings`可以被高效地压缩，因此`BSDiff`是一个和依赖于压缩和解压的算法。

### 1.3 `BSPatch`算法

客户端合成`patch`的基本步骤如下：

> 1.  接收`patch`包；
> 2. 解压`patch`包；
> 3. 还原`new`文件。

三个步骤同时在`O(m)`时间内完成，但在时间常数上更依赖于解压`patch`包的部分，`m`为新文件的长度。

### 1.4 对比

#### 1.4.1 时间空间复杂度

`BSDiff`与`BSPatch`的时间与空间复杂度如下表所示：

| 复杂度类型\算法名称 | `BSDiff`   | `BSPatch` |
| ------------------- | ---------- | --------- |
| 时间复杂度          | `O(nlogN)` | `O(n+m)`  |
| 空间复杂度          | `O(n)`     | `O(n+m)`  |

#### 1.4.1 `BSDiff`压缩效率实验数据

![image](https://github.com/tianyalu/NeBsdiff/raw/master/show/bsdiff_experimental_data.png)  

参考博客：[[差量更新系列1]BSDiff算法学习笔记](https://blog.csdn.net/add_ada/article/details/51232889)

## 二、实现

### 2.1 服务器端生成差量包

`Windows`端可以直接使用别人生成好的程序。

下载地址：[https://github.com/cnSchwarzer/bsdiff-win/releases/tag/v4.3](https://github.com/cnSchwarzer/bsdiff-win/releases/tag/v4.3)

`Linux`系统服务器可以通过下载源码编译生成。

bsdiff 下载地址： http://www.daemonology.net/bsdiff/

#### 2.1.1 下载

```bash
wget http://www.daemonology.net/bsdiff/bsdiff-4.3.tar.gz
```

本文尝试下载时提示如下：

```bash
Resolving www.daemonology.net (www.daemonology.net)... 34.218.139.66, 2600:1f14:4e4:8c01:55b4:c24e:cc6e:c5c3
Connecting to www.daemonology.net (www.daemonology.net)|34.218.139.66|:80... connected.
HTTP request sent, awaiting response... 403 Forbidden
ERROR 403: Forbidden.
```

无奈只好使用别人下载好的文件`bsdiff-4.3.tar.gz`（在`resources`目录下）。

#### 2.1.2 上传到服务器

在**本地物理机终端上**使用如下命令：

```bash
scp [$File_name] [$Username]@[$IP]:[$File]
```

> - [$File_name]指的是**本地文件**的名字。
> - [$Username]指的是**服务器**的用户名字（eg: root）。
> - [$IP]指的是**服务器**的IP地址。
> - [$File]指的是指定的**服务器**目录。

#### 2.1.3 解压

```bash
tar zxvf bsdiff-4.3.tar.gz
```

#### 2.1.4 编译

进入解压目录尝试编译：

```bash
cd bsdiff-4.3
make
```

报错如下：

```bash
Makefile:13: *** missing separator.  Stop.
```

解决方法：

修改`Makdefile`文件，在13、15行之前添加`TAB`键作为分隔符。之后重新编译，报错如下：

```bash
bsdiff.c:33:10: fatal error: bzlib.h: No such file or directory
 #include <bzlib.h>
          ^~~~~~~~~
```

解决方法：

安装`yum`源中的`bzip2`包( 参考：[centos boost fatal error: bzlib.h: No such file or directory  #include "bzlib.h"](https://blog.csdn.net/jzp12/article/details/89184209) )

```bash
yum install  bzip2-devel.x86_64
```

重新执行`make`命令，则成功编译，生成如下文件：

```bash
bsdiff  bsdiff.1  bsdiff.c  bspatch  bspatch.1  bspatch.c  Makefile
```

#### 2.1.5 使用`BSDiff`生成差量包

首先`Android`端生成新旧`new.apk`和`old.apk`并通过2.1.2所述方法上传到服务器，然后用`bsdiff`命令生成差量包`patch.diff`。

```bash
# 查看bsdiff的用法
# ./bsdiff --help
bsdiff: usage: ./bsdiff oldfile newfile patchfile
# ./bsdiff old.apk new.apk patch.diff
```

同样采用2.1.2所述方法（交换`scp`后面两参数的位置），将生成的差量包`patch.diff`下载到手机`SD`卡上。

### 2.2 `Android`端合成新`apk`文件

#### 2.2.1 导入`c`文件

导入`bspatch.c`文件和`bzip2`目录文件，可以从 [https://github.com/cnSchwarzer/bsdiff-win](https://github.com/cnSchwarzer/bsdiff-win) 获取。

#### 2.2.2 修改`CMakeList.txt`文件

```cmake
cmake_minimum_required(VERSION 3.4.1)
include_directories(${CMAKE_SOURCE_DIR}/bzip2)
aux_source_directory(${CMAKE_SOURCE_DIR}/bzip2/ bzip2_srcs)

add_library( # Sets the name of the library.
        native-lib
        SHARED
        native-lib.cpp
        bspatch.c
        ${bzip2_srcs})

target_link_libraries( # Specifies the target library.
        native-lib
        log)
```

#### 2.2.3 下载（模拟）差量包&合成新`apk`

```java
@Override
protected File doInBackground(Void... voids) {
  //下载更新补丁包（省略）
  String patchPath = new File(APK_PATH, "patch.diff").getAbsolutePath();
  //合成：旧版本apk文件（当前运行的apk）+ 从服务器下载的补丁包文件 = 新版本的apk安装包文件
  String oldApkPath = getApplicationInfo().sourceDir;
  File newApk = new File(APK_PATH, "new.apk");
  Log.e("sty", newApk.getAbsolutePath());
  if(!newApk.exists()) {
    try {
      newApk.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  doPatchNative(oldApkPath, newApk.getAbsolutePath(), patchPath);
  return newApk;
}
```

调用`BSPatch`算法合成新的`apk`:

```c++
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
```

#### 2.2.4 安装新的`apk`

```java
@Override
protected void onPostExecute(File newApk) {
  //安装
  if(!newApk.exists()) {
    return;
  }
  Intent intent = new Intent(Intent.ACTION_VIEW);
  intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  if(Build.VERSION.SDK_INT >= 24) { //Android7.0以上
    //参数2：清单文件中provider节点里面的authorities  参数3：共享的文件，即apk包的file类
    Uri apkUri = FileProvider.getUriForFile(MainActivity.this,
                                            getApplicationInfo().packageName + ".provider", newApk);
    //对目标应用临时授权该URI所代表的文件
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
  }else {
    intent.setDataAndType(Uri.fromFile(newApk), "application/vnd.android.package-archive");
  }
  startActivity(intent);
}
```

`AndroidManifest.xml`文件定义`provider`:

```xml
<provider
          android:authorities="${applicationId}.provider"
          android:name="androidx.core.content.FileProvider"
          android:grantUriPermissions="true"
          android:exported="false">
  <meta-data
             android:name="android.support.FILE_PROVIDER_PATHS"
             android:resource="@xml/file_paths"/>
</provider>
```

#### 2.2.5 采坑

编译运行时报错如下：

```bash
/Users/tian/NeCloud/NDKWorkspace/NeBsdiff/app/src/main/cpp/bzip2/bzlib.c:1431:12: warning: implicit declaration of function '_fdopen' is invalid in C99 [-Wimplicit-function-declaration]
```

将`bzip2/bzlib.c`文件中的1431行改为：

```c
//fp = _fdopen(fd,mode2);  //1431行
fp = fdopen(fd,mode2);
```

