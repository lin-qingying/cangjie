# cfir/raw-cfir/psi2cfir/ — PSI → Raw CFIR 转换

`:cfir:raw-cfir` 的主实现路径：把 `:psi` 模块产出的 PSI 树转换为 Raw CFIR。对齐 Kotlin K2 `compiler/fir/raw-fir/psi2fir`。

## 关键类

- `PsiRawCfirBuilder` — 转换主类，覆盖完整声明（class / interface / struct / enum / extend / function / property / typealias 等）与表达式
- `convertDeclaration(...)` — 声明分派入口
- `convertExpression(...)` — 表达式分派入口

未覆盖节点统一构造错误节点（`CfirInvalidDeclaration` / `CfirErrorExpression`，带 `reason`）。

## 关键包

`org.cangnova.cangjie.cfir.builder` — Builder 主体。

## testFixtures

提供 raw-builder 测试共用的工具，下游 / 同模块测试可复用。

## 测试

```bash
./gradlew :cfir:raw-cfir:psi2cfir:test
```

测试 include-pattern 处理已增强：选择外部测试类时会自动包含内部类。

测试数据位于 `testData/rawBuilder/`，含 [`README.md`](testData/rawBuilder/README.md) 与 [`coverage-matrix.md`](testData/rawBuilder/coverage-matrix.md)。

## 依赖

- `:cfir:cfir-tree`、`:cfir:raw-cfir:raw-cfir-common`
- `:psi`

## 相关文档

- `../README.md` — Raw CFIR 聚合说明
- `../../../docs/psi-cfir-ast-chir-alignment.md` — PSI ↔ CFIR ↔ 官方 AST ↔ CHIR 节点对照
- `testData/rawBuilder/coverage-matrix.md` — 当前覆盖矩阵
