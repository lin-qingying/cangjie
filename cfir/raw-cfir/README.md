# cfir/raw-cfir/ — Raw CFIR 构建（聚合）

前端管线阶段 6 `CFIR_BUILD` 的实现。从源码树（PSI 或 LightTree）构建 Raw CFIR 骨架——结构完整，但类型引用未解析。对齐 Kotlin K2 `compiler/fir/raw-fir`。

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

## 含 BUILD 内嵌的子步骤

按官方编译器，本阶段内含：

1. **增量判断**（AST_DIFF）：比较 AST 快照，未变更声明从缓存加载，跳过 BUILD + RESOLVE
2. **前置脱糖**（DesugarBeforeTypeCheck）：源码树 → Raw CFIR 转换中一并完成简单脱糖
3. **CFIR 构建**：建立声明节点、作用域树、符号引用占位符

## 错误处理

未覆盖节点统一构造错误节点：

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

- `../../docs/cjfir-compiler-stages.md` 第 6 阶段
- `../../docs/psi-cfir-ast-chir-alignment.md` — PSI ↔ CFIR 节点对照
- `psi2cfir/testData/rawBuilder/README.md` — 测试数据组织
