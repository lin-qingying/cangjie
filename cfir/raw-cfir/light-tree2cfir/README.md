# cfir/raw-cfir/light-tree2cfir/ — LightTree → Raw CFIR 转换

另一条 Raw CFIR 构建路径，把 IntelliJ LightTree（轻量语法树，不构造完整 PSI）转换为 Raw CFIR。对齐 Kotlin K2 `compiler/fir/raw-fir/light-tree2fir`，**用于非 IDE 编译场景**（命令行编译器更高效）。

## 关键类

- `LightTreeRawCfirBuilder` — 转换主类
- `LightTreeTypeConverter` — 类型节点转换；错误类型构造已统一改为 `diagnostic = ConeSimpleDiagnostic(...)`

## 关键包

`org.cangnova.cangjie.cfir.lightTree` — LightTree 专属逻辑。

## 设计要点

- 与 `psi2cfir` 共享 `:cfir:raw-cfir:raw-cfir-common` 的泛型基类 `AbstractRawCfirBuilder<T>`
- 不依赖 `:psi`——直接处理 LightTree
- 产物与 `psi2cfir` 等价（同一份 Raw CFIR 节点定义）

## testFixtures

提供 LightTree 测试共用基础设施。

## 测试

```bash
./gradlew :cfir:raw-cfir:light-tree2cfir:test
```

## 依赖

- `:cfir:cfir-tree`、`:cfir:raw-cfir:raw-cfir-common`
- IntelliJ Platform LightTree（通过 `:dependencies:intellij-core`）

## 已知工程注意点

某些 Kotlin daemon 增量缓存冲突会导致编译失败——清理本模块 `build/kotlin` 后 fresh 编译通常通过。

## 相关文档

- `../README.md` — Raw CFIR 聚合说明
- `../../../docs/psi-cfir-ast-chir-alignment.md` — 节点对照
