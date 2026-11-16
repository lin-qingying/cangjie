# analysis/analysis-tools/ — Analysis 工具集

Analysis 外围模块（`stubs` / `light-declarations` / `decompiled`）的统一 inspector 入口：反编译文本 dump、stub dump、declaration view dump 与三视图一致性检查。只服务内部检查、golden 输出与一致性回归，对齐 Kotlin `analysis/analysis-tools` 的部分能力。

## 关键包

`org.cangnova.cangjie.analysis.tools` — `CaAnalysisInspectorTools`（`dumpDecompiledText` / `dumpStubFile` / `dumpStubPackage` / `dumpClassMemberStubNames` / `dumpLightDeclarations` / `checkViewConsistency`）与三视图一致性问题类型（`CaAnalysisViewConsistencyIssue`）。

## 使用场景

- 内部检查：按包或文件 dump 反编译文本 / stub / light declaration 视图
- golden 输出：stub 与 declaration view 测试的 golden 来源
- 一致性回归：`checkViewConsistency` 交叉核对 stub、declaration view 与反编译文件三视图

## 依赖

- `:analysis:analysis-api`、`:analysis:analysis-api-platform-interface`、`:analysis:analysis-internal-utils`
- `:analysis:stubs`、`:analysis:decompiled`、`:analysis:light-declarations`、`:analysis:symbol-light-declarations`
- `:psi`、`:common`

## 命令

```bash
./gradlew :analysis:analysis-tools:assemble
./gradlew :analysis:analysis-tools:test
```

## 相关文档

- `../README.md` — Analysis 模块总览
