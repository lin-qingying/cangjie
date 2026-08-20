# cfir/raw-cfir/ — Raw CFIR 构建（聚合）

前端的 Raw CFIR 构建层。从源码树（PSI 或 LightTree）构建结构完整、尚未解析类型引用的 Raw CFIR。对齐 Kotlin K2 `compiler/fir/raw-fir`。

本目录是**聚合命名空间**，自身无源码，下挂 3 个子模块。

## 子模块

| 子模块 | 职责 |
|---|---|
| `raw-cfir-common` | 共享基础设施：`AbstractRawCfirBuilder<T>`、`RawCfirBuilderContext`、错误节点构造（如 `buildErrorExpression`） |
| `psi2cfir` | PSI → Raw CFIR 转换主实现（`PsiRawCfirBuilder`），覆盖完整声明 + 表达式 |
| `light-tree2cfir` | LightTree → Raw CFIR 转换（`LightTreeTypeConverter` 等） |

## 关键约束

- `raw-cfir-common` **不依赖** `:psi`——`AbstractRawCfirBuilder<T>` 是泛型抽象
- PSI 专属逻辑只放在 `:cfir:raw-cfir:psi2cfir`
- LightTree 实现共享 `raw-cfir-common` 的干净底座，不被 PSI 反向污染

## 构建边界

Raw builder 负责把声明、表达式、类型引用和源码位置信息转换为 CFIR 骨架；名称、类型、调用和函数体语义由后续 ordinary resolve 完成。宏调用面和注解槽位会在构建期间记录，供后续宏构造使用。

## 语法恢复

对不完整或未覆盖的语法，builder 保留错误节点，使后续诊断能够继续报告：

- 声明：`CfirInvalidDeclaration { reason = "Unsupported declaration: ..." }`
- 表达式：`buildErrorExpression(...)`，`Unsupported expression: ...`
- 字段缺失（如 `if` 缺条件 / `match` 缺 subject / `for-in` 缺 iterable）有兜底分支

## 命令

```bash
./gradlew :cfir:raw-cfir:psi2cfir:test
./gradlew :cfir:raw-cfir:light-tree2cfir:test
```

`:cfir:raw-cfir:psi2cfir` 测试 include-pattern 处理已增强：选择外部测试类时会自动包含内部类。

## 测试数据

- `psi2cfir/testData/rawBuilder/` — Raw CFIR 构建样例，含 `coverage-matrix.md` 覆盖矩阵

## 相关文档

- `../../docs/cjfir-compiler-stages.md` — Raw CFIR、宏构造和 ordinary resolve 的边界
- `../README.md` — CFIR 子系统目录
- `psi2cfir/testData/rawBuilder/README.md` — 测试数据组织
