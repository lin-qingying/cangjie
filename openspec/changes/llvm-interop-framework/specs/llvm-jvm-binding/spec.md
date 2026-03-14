## 新增需求

### 需求:类型安全的 LLVM 句柄体系
系统必须为每种 LLVM 核心对象（Context、Module、Type、Value、BasicBlock、Builder）提供独立的 value class 句柄类型，禁止不同类型句柄之间隐式互换。每个句柄类型必须包装一个 `Long` 类型的原生指针地址，并提供 `NULL` 常量和 `isNull` 检查。

#### 场景:不同句柄类型编译时隔离
- **当** 开发者将 `LlvmTypeRef` 传入期望 `LlvmValueRef` 参数的函数
- **那么** 编译器报告类型不匹配错误，阻止编译

#### 场景:空句柄检测
- **当** JNI 层返回的原生指针为 0（NULL）
- **那么** 对应的句柄对象的 `isNull` 属性必须返回 `true`

### 需求:LLVM Context 生命周期管理
`LlvmContext` 必须实现 `AutoCloseable` 接口。创建时调用 `LLVMContextCreate()`，关闭时调用 `LLVMContextDispose()`。Context 关闭后，所有由该 Context 创建的 Module、Builder 等子对象必须不可再使用。

#### 场景:正常创建和关闭
- **当** 通过 `LlvmContext()` 创建上下文并调用 `close()`
- **那么** 底层 LLVM 上下文被正确释放，无内存泄漏

#### 场景:use 块自动关闭
- **当** 在 `LlvmContext().use { ctx -> ... }` 块中使用上下文
- **那么** 块结束后上下文自动关闭，即使发生异常也必须释放

### 需求:LLVM Module 创建与基本操作
系统必须支持在指定 Context 中创建命名 Module，并提供以下基本操作：设置 target triple、设置 data layout、添加函数声明、添加全局变量、获取模块 IR 文本表示、验证模块正确性。

#### 场景:创建模块并设置目标
- **当** 调用 `context.createModule("my_module")` 并设置 target triple 为 `"x86_64-unknown-linux-gnu"`
- **那么** 生成的模块 IR 中必须包含 `target triple = "x86_64-unknown-linux-gnu"`

#### 场景:模块验证失败
- **当** 模块中存在不合法的 IR（如函数返回类型不匹配）
- **那么** `module.verify()` 必须返回验证失败结果，包含 LLVM 的错误描述信息

### 需求:LLVM 类型系统绑定
系统必须绑定以下 LLVM 类型构造 API（全部通过 Context 创建，确保线程安全）：
- 整数类型：`i1`、`i8`、`i16`、`i32`、`i64`、任意位宽
- 浮点类型：`float`、`double`
- 空类型：`void`
- 指针类型：不透明指针 `ptr`
- 函数类型：指定返回类型和参数类型列表
- 结构体类型：命名结构体和匿名结构体
- 数组类型：固定大小数组

#### 场景:创建函数类型
- **当** 调用 `context.functionType(returnType = context.int32Type, paramTypes = listOf(context.int32Type, context.int32Type))`
- **那么** 返回代表 `i32 (i32, i32)` 的 `LlvmTypeRef`

#### 场景:创建命名结构体
- **当** 调用 `context.namedStructType("MyStruct")` 并设置 body 为 `[i32, ptr]`
- **那么** 返回代表 `%MyStruct = type { i32, ptr }` 的结构体类型

### 需求:IRBuilder 指令生成
系统必须提供 `LlvmBuilder` 类（包装 `LLVMBuilderRef`），支持生成以下核心指令类别：
- 终止指令：`ret`、`br`、`condBr`、`switch`、`unreachable`
- 算术指令：`add`、`sub`、`mul`、`sdiv`、`udiv`、`srem`、`urem`、`fneg`、`fadd`、`fsub`、`fmul`、`fdiv`
- 位运算指令：`and`、`or`、`xor`、`shl`、`ashr`、`lshr`
- 比较指令：`icmp`、`fcmp`
- 内存指令：`alloca`、`load`、`store`、`getelementptr`
- 转换指令：`trunc`、`zext`、`sext`、`fptrunc`、`fpext`、`fptoui`、`fptosi`、`uitofp`、`sitofp`、`ptrtoint`、`inttoptr`、`bitcast`
- 其他：`call`、`phi`、`select`、`extractvalue`、`insertvalue`

#### 场景:构建简单返回函数
- **当** 使用 Builder 在 entry 基本块中生成 `ret i32 42`
- **那么** 模块 IR 输出中对应函数包含 `ret i32 42` 指令

#### 场景:构建条件分支
- **当** 使用 Builder 生成 `condBr(cond, thenBlock, elseBlock)`
- **那么** 模块 IR 输出中包含 `br i1 %cond, label %then, label %else`

### 需求:JNI 原生桥接层
系统必须提供名为 `LlvmNative` 的 Kotlin object，通过 JNI `external fun` 声明所有 LLVM C API 绑定。原生端（C/C++）必须实现对应的 JNI 函数，正确转换 Java 类型（`jlong`→指针、`jstring`→`const char*`）并处理错误。

#### 场景:字符串参数传递
- **当** Kotlin 调用 `LlvmNative.moduleCreateInContext("test", contextRef)` 传入 Kotlin String
- **那么** JNI 层必须正确将 String 转换为 C 字符串，调用 `LLVMModuleCreateWithNameInContext`，并释放 JNI 字符串资源

#### 场景:LLVM 错误转换为 Java 异常
- **当** LLVM C API 返回错误（如 `LLVMVerifyModule` 返回非零值）
- **那么** JNI 层必须将错误消息提取并抛出 `LlvmException`（继承 `RuntimeException`）

### 需求:原生库加载机制
系统必须提供自动加载 JNI 原生库的机制，按以下优先级尝试：
1. 系统属性 `cangjie.llvm.native.library.path` 指定的路径
2. Classpath 资源 `/native/<os>-<arch>/` 下的库文件
3. 系统库搜索路径（`PATH`/`LD_LIBRARY_PATH`）

加载失败时必须抛出包含诊断信息的异常，说明尝试过的路径和失败原因。

#### 场景:从 classpath 加载
- **当** 运行环境为 Linux x86_64，且 classpath 中存在 `/native/linux-x86_64/libcangjie_llvm_jni.so`
- **那么** 系统自动提取到临时目录并通过 `System.load()` 加载成功

#### 场景:加载失败的诊断信息
- **当** 所有加载路径均不存在原生库
- **那么** 抛出异常，消息中必须列出所有尝试过的路径
