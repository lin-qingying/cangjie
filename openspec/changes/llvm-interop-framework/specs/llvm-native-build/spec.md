## 新增需求

### 需求:Gradle-native 编译插件
系统必须提供自定义 Gradle 插件 `native-compile-plugin`（参考 Kotlin/Native 的 `NativePlugin`），提供 `nativeCompile {}` DSL 块，支持：
- 声明 C/C++ 源文件目录
- 配置编译选项（头文件路径、宏定义、C++ 标准）
- 配置链接选项（库路径、链接库）
- 自动检测 LLVM 安装路径
- 调用 `clang++` 编译 `.cpp` → `.o`，再链接生成共享库

CMakeLists.txt 必须仅用于 IDE 代码导航（CLion），禁止作为构建系统使用。

#### 场景:Gradle 直接编译 C++ 源文件
- **当** 执行 `./gradlew :llvm-interop-jni:compileNative`
- **那么** Gradle 调用 `clang++` 编译 `src/main/native/*.cpp`，生成对应的 `.o` 对象文件

#### 场景:CMakeLists.txt 不参与构建
- **当** 删除 `src/main/native/CMakeLists.txt` 后执行 `./gradlew :llvm-interop-jni:build`
- **那么** 构建正常完成，CMakeLists.txt 的缺失不影响编译

### 需求:LLVM 开发库自动检测
Gradle 插件必须自动检测 LLVM 开发库的安装位置，按以下优先级查找：
1. Gradle 属性 `llvm.dir`（通过 `-Pllvm.dir=...` 或 `gradle.properties`）
2. 环境变量 `LLVM_DIR`
3. 执行 `llvm-config --prefix` 获取路径（需 `llvm-config` 在 PATH 中）
4. 平台默认路径（Linux: `/usr/lib/llvm-18`，macOS: `/opt/homebrew/opt/llvm@18`，Windows: `C:/Program Files/LLVM`）

找到后必须验证头文件（`llvm-c/Core.h`）和库文件存在。

#### 场景:通过 llvm-config 检测
- **当** 系统 PATH 中存在 `llvm-config`，未设置 `LLVM_DIR` 环境变量
- **那么** 插件通过 `llvm-config --prefix` 获取 LLVM 路径，成功配置编译

#### 场景:LLVM 不可用时跳过原生构建
- **当** 以上所有检测路径均未找到 LLVM 开发库
- **那么** 原生编译任务被跳过并输出警告 "LLVM development libraries not found, skipping native build"，Kotlin 代码正常编译

### 需求:共享库输出与命名
Gradle 插件必须在链接阶段生成正确命名的共享库：

| 操作系统 | 架构 | 输出文件名 |
|---------|------|-----------|
| Linux | x86_64 | libcangjie_llvm_jni.so |
| Linux | aarch64 | libcangjie_llvm_jni.so |
| macOS | x86_64 | libcangjie_llvm_jni.dylib |
| macOS | aarch64 | libcangjie_llvm_jni.dylib |
| Windows | x86_64 | cangjie_llvm_jni.dll |

共享库必须链接 JDK 的 JNI 头文件和 LLVM 库。

#### 场景:Linux 上生成共享库
- **当** 在 Linux x86_64 上执行完整原生构建
- **那么** 输出 `libcangjie_llvm_jni.so`，可被 `System.load()` 正确加载

#### 场景:链接 JNI 和 LLVM
- **当** 编译 JNI 原生代码
- **那么** 编译器参数必须包含 JDK `jni.h` 头文件路径和 LLVM 头文件路径，链接器参数必须包含 LLVM 库

### 需求:JNI 头文件自动生成
Gradle 构建必须在编译 C/C++ 之前，从 Kotlin `external fun` 声明生成 JNI C 头文件。生成的头文件必须放置在 `build/generated/jni-headers/` 目录下，并自动加入 C/C++ 编译的头文件搜索路径。

#### 场景:从 Kotlin external fun 生成头文件
- **当** `LlvmNative.kt` 中声明了 `external fun contextCreate(): Long`
- **那么** 生成的 JNI 头文件中必须包含对应的 `JNIEXPORT jlong JNICALL Java_..._contextCreate(JNIEnv *, jobject)` 声明

### 需求:平台检测与工具链选择
Gradle 插件必须自动检测当前构建平台，选择正确的编译器和链接器，并设置平台特定的编译选项：
- **编译器选择**：优先使用 LLVM 附带的 `clang++`，否则使用系统 `clang++`，最后回退到 `g++`
- **平台宏定义**：根据平台设置 `CANGJIE_LINUX`、`CANGJIE_MACOS`、`CANGJIE_WINDOWS` 宏
- **链接选项**：Linux 使用 `-shared -fPIC`，macOS 使用 `-dynamiclib`，Windows 使用 `/DLL`

#### 场景:Linux 平台编译选项
- **当** 在 Linux 上编译原生代码
- **那么** 编译命令必须包含 `-fPIC -DCANGJIE_LINUX=1`，链接命令必须包含 `-shared`

#### 场景:优先使用 LLVM 的 clang
- **当** LLVM 安装路径为 `/usr/lib/llvm-18`，且 `/usr/lib/llvm-18/bin/clang++` 存在
- **那么** 使用该 clang++ 而非系统 PATH 中的 clang++

### 需求:多平台 CI 构建与产物聚合
CI 构建流水线必须为每个支持平台编译原生库，并将所有平台的产物聚合到统一的目录结构 `native/<os>-<arch>/` 下，可打包进发布 JAR。

#### 场景:CI 多平台构建
- **当** 触发 CI 构建流水线
- **那么** 为每个支持的平台编译原生库，所有平台均编译成功

#### 场景:制品聚合打包
- **当** 所有平台构建完成后执行打包任务
- **那么** 生成的 JAR 文件中包含 `native/linux-x86_64/`、`native/linux-aarch64/`、`native/macos-x86_64/`、`native/macos-aarch64/`、`native/windows-x86_64/` 五个目录下的对应原生库

### 需求:平台标识符映射
系统必须自动检测当前运行平台的操作系统和 CPU 架构，映射到标准化的 `<os>-<arch>` 标识符用于原生库查找：

| `os.name` 属性 | `os.arch` 属性 | 标识符 |
|----------------|----------------|--------|
| Linux | amd64 | linux-x86_64 |
| Linux | aarch64 | linux-aarch64 |
| Mac OS X | x86_64 | macos-x86_64 |
| Mac OS X | aarch64 | macos-aarch64 |
| Windows 10/11 | amd64 | windows-x86_64 |

不支持的平台组合必须返回明确的错误信息。

#### 场景:正确检测 Linux x86_64
- **当** 在 `os.name=Linux, os.arch=amd64` 环境中查询平台标识符
- **那么** 返回 `"linux-x86_64"`

#### 场景:不支持的平台
- **当** 在 `os.name=FreeBSD, os.arch=amd64` 环境中查询平台标识符
- **那么** 抛出异常说明 "Unsupported platform: FreeBSD-amd64"
