# common/ — 编译器基础设施与领域基础模型

承载编译器最底层的领域模型与通用基础：名称系统、内置类型、描述符、消息收集、语言版本设置、高性能容器。对齐 Kotlin K2 `compiler/util` 与 `compiler/frontend.common.psi` 中的领域基础部分。

## 关键包

| 包 | 职责 |
|---|---|
| `cangjie.name` | `Name` / `FqName` / `ClassId` / `CallableId`，含路径遍历与缓存 |
| `cangjie.descriptors` | `Visibility` / `Modality` / `Modifier` 等描述符与可见性比较 |
| `cangjie.descriptors.annotations` | 注解描述符 |
| `cangjie.builtins` | 内置类型（`PrimitiveType` 18 种：Int8 ~ UInt64 / Float16~Float64 / Bool / Rune / IntNative / UIntNative / Unit / Nothing） |
| `cangjie.constant` | 常量定义 |
| `cangjie.messages` | `CompilerMessageSeverity` / `MessageCollector`（编译器消息系统） |

子模块：

- [`diagnostics/`](diagnostics/README.md) — 诊断框架核心（独立子模块）

## 设计要点

- 无 `:psi`、`:cfir:*` 依赖——所有处理层共用的最底层模型
- 含 `ComponentArrayOwner` / `ArrayMap` 等高性能容器（O(1) 注册 / 查找）
- `LanguageVersionSettings` 从 `:compiler:config` 迁入，作为基础模型

## 依赖

- `:util`

## 命令

```bash
./gradlew :common:assemble
./gradlew :common:test
```

## 相关文档

- `../docs/k2-module-alignment.md` — 与 Kotlin K2 对照
- `../docs/current-module-organization.md` — 在基础设施层的位置
