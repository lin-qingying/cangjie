# cfir/raw-cfir/raw-cfir-common/ — Raw CFIR 共享基类

`psi2cfir` 与 `light-tree2cfir` 共享的基类与抽象层。对齐 Kotlin K2 `compiler/fir/raw-fir` 的 common 部分。

## 关键包

| 包 | 职责 |
|---|---|
| `cfir.builder` | `AbstractRawCfirBuilder<T>`（泛型转换基类）、`RawCfirBuilderContext`、错误节点构造（`buildErrorExpression`）、转换工具 |
| `cfir.builder.macro` | 宏面 token 化（`MacroPayloadTokenizer`） |

## 依赖

- `:cfir:cfir-tree`
- `:cfir:providers`
- `:psi`（`implementation`，不传递暴露）

## 命令

```bash
./gradlew :cfir:raw-cfir:raw-cfir-common:assemble
./gradlew :cfir:raw-cfir:raw-cfir-common:test
```

## 相关文档

- `../README.md` — Raw CFIR 总览
