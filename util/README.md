# util/ — 通用工具库

仓颉编译器最底层的通用工具集，对齐 Kotlin K2 `compiler/util`。
无业务依赖、无 IR 依赖，可被任何模块使用。

## 关键包

| 包 | 职责 |
|---|---|
| `cangjie.utils` | 字符串 / 集合 / 打印工具 |
| `cangjie.utils.concurrent` | 并发原语 |
| `cangjie.utils.concurrent.block` | 阻塞队列、锁工具 |
| `cangjie.utils.exceptions` | 异常框架，含 `CangJieExceptionWithAttachments`（可附加附件的异常） |

## 提供的关键工具

- `Printer` / `SmartPrinter` — 带缩进打印器（14 公共方法）
- 集合工具（O(1) 优化、DSL 构建、容量智能分配，50+ 工具方法）
- 异常框架（可附件异常用于诊断 / 调试）
- 字符串工具（大小写转换等）

## 设计原则

- 零业务依赖，可作为整个仓库的最底层基础
- 任何模块都可以 `implementation(project(":util"))`

## 依赖

- 无项目内依赖（仅 Kotlin stdlib + 必要第三方）

## 命令

```bash
./gradlew :util:assemble
./gradlew :util:test
```

