# flatbuffers-gen/ — FlatBuffers Schema 与生成产物

为仓颉编译器的二进制序列化（`.cjo` 文件、宏协议）提供 FlatBuffers schema 与生成的 Kotlin 代码。

## 内容

- `flatbuffers/` — FlatBuffers schema 源文件（`.fbs`，含上游 `StyleGuide.md` 等参考）
- `gen/` — 生成的 Kotlin 代码（`flatc --kotlin` 产物，构建时生成）

## Schema 用途

参考官方 C++ 编译器的 schema（位于 `external/cangjie_compiler/schema/`）：

| Schema | 用途 |
|---|---|
| `PackageFormat.fbs` | CHIR Package 序列化 |
| `BCHIRFormat.fbs` | CHIR 二进制序列化 |
| `NodeFormat.fbs` | AST 节点序列化 |
| `ModuleFormat.fbs` | 模块 / 包头信息 |
| `CachedASTFormat.fbs` | 增量编译 AST 缓存 |
| `MacroMsgFormat.fbs` | 宏元数据序列化 |

## 调用方

- `:cfir:cfir-serialization` — `.cjo` 文件读写
- `:macro:macro-common` — 宏协议编解码（length-prefixed FlatBuffers 帧）

## 构建

`flatc` 生成的 Kotlin 代码输出到 `gen/` 并作为源集纳入编译（`generatedDir()`）。`compileKotlin` 会触发 `generateKotlinFlatBuffers`：先经 `locateFlatc` 定位（缺失则自动下载 `flatc` v25.2.10），再以 `--kotlin --gen-mutable --gen-object-api` 重新生成；任务为 `@CacheableTask`，输入不变时走 Gradle 缓存。详见模块 `build.gradle.kts`。

## 命令

```bash
./gradlew :flatbuffers-gen:assemble
```

## 注意

`flatbuffers/StyleGuide.md` 是 FlatBuffers 上游同步过来的文档，不在仓库文档维护范围内，不做语义修订。

## 相关文档

- `../docs/cjfir-compiler-stages.md` 第 10 阶段 SAVE_CJO — 序列化格式说明
- `../macro/README.md` — 宏协议设计
