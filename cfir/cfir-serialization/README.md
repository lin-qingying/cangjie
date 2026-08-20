# cfir/cfir-serialization/ — `.cjo` 元数据与反序列化

提供前端使用的 FlatBuffers `.cjo` 集成：包头加载、声明与类型反序列化、跨模块符号 provider，以及包级元数据写出。

## 边界

`CjoPackageWriter` 只写出其数据模型覆盖的包级字段：包身份与版本、导入、源文件条目和顶层导出声明索引。这些工件服务于宏工件解析、反序列化索引和前端编排；它不宣称序列化完整的 CFIR 或 CHIR 函数体。

读取路径加载 `.cjo` 包头、解析导入包索引，并为反序列化 symbol provider 重建受支持的声明和类型。格式覆盖范围以模块测试为准。

## 关键包

| 包 | 职责 |
| --- | --- |
| `cfir.serialization.cjo` | `.cjo` 包头、包管理器和 FlatBuffers 元数据 writer |
| `cfir.serialization.deserialize` | 声明、类型和包索引反序列化 |
| `cfir.serialization.provider` | 跨模块的反序列化符号与 extend provider |
| `cfir.deserialization` | 前端入口使用的共享反序列化模型 |

## 依赖

- `:cfir:cfir-tree`、`:cfir:cfir-cones`、`:cfir:cfir-common`
- `:flatbuffers-gen`

## 构建与测试

```bash
./gradlew :cfir:cfir-serialization:assemble
./gradlew :cfir:cfir-serialization:test
```

测试资源包含 `cjo-sdk/`；夹具来源见 `testResources/cjo-sdk/README.md`。

## 相关文档

- `../../docs/cjfir-compiler-stages.md` — 前端输出与 `.cjo` 集成边界
- `../README.md` — CFIR 子系统目录
- `../../docs/module-catalog.md` — Gradle 模块目录
