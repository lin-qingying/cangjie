# cfir/cfir-serialization/ — .cjo 序列化与反序列化

承载前端管线阶段 4 `IMPORT_PACKAGE` 与阶段 10 `SAVE_CJO` 的序列化层。

**当前状态**：反序列化路径完整可用；序列化写入侧仍在补齐。

## 关键包

| 包 | 职责 |
|---|---|
| `cfir.serialization.cjo` | `.cjo` 文件格式编解码（FlatBuffers schema 由 `:flatbuffers-gen` 提供） |
| `cfir.serialization.deserialize` | 反序列化主流程：`.cjo` → CFIR |
| `cfir.serialization.provider` | 跨模块符号 provider（基于反序列化结果） |
| `cfir.deserialization` | 反序列化数据模型（与 `serialization.deserialize` 分层） |

## 阶段映射

- **阶段 4 IMPORT_PACKAGE**：本模块读取 `.cjo` 文件，向 CFIR 注入外部包符号（class / function / property / typealias），供跨包类型引用与重载解析使用。
- **阶段 10 SAVE_CJO**：将当前包的 CFIR 序列化为 `.cjo` 文件 + `.cjo.flag` 标志，供下游包导入。

## 格式

- 序列化使用 FlatBuffers（schema 见 `:flatbuffers-gen`，参考官方 `PackageFormat.fbs` / `NodeFormat.fbs` / `ModuleFormat.fbs`）
- 启用 `-g` 或 `--coverage` 时包含绝对源码路径
- `.cjo` 等价于 Kotlin 的 `.klib`

## 依赖

- `:cfir:cfir-tree`、`:cfir:cfir-cones`、`:cfir:cfir-common`
- `:flatbuffers-gen`

## 命令

```bash
./gradlew :cfir:cfir-serialization:assemble
./gradlew :cfir:cfir-serialization:test
```

测试资源含 `cjo-sdk/`，详见 `testResources/cjo-sdk/README.md`。

## 相关文档

- `../../cjfir-compiler-stages.md` 第 4、10 阶段 — IMPORT_PACKAGE / SAVE_CJO 设计
- `../../docs/current-module-organization.md` — SAVE_CJO 写入侧状态
