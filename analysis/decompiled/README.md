# analysis/decompiled/ — `.cjo` 反编译聚合

对齐 Kotlin `analysis/decompiled`。
把已编译的 `.cjo` 工件反编译为可读源码 / PSI / stubs / light declarations，供 IDE 浏览外部依赖。

本目录是聚合模块，下挂 4 个子模块：

| 子模块 | 职责 |
|---|---|
| `decompiler-to-file-stubs` | `.cjo` → file-level stubs |
| `decompiler-to-stubs` | `.cjo` → stub 树 |
| `decompiler-to-psi` | `.cjo` → 反编译 PSI（可在 IDE 中浏览） |
| `light-declarations-for-decompiled` | 反编译 light declarations 接入 |

## 关键包

`org.cangnova.cangjie.analysis.decompiled.*` — 反编译入口、`.cjo` 二进制头读取、虚拟文件挂载。

## 测试

- 允许保留的直测：`CjoBinaryFileReaderTest`、`DecompiledFileStubKindsTest`
- 涉及 PSI / session / decompiler service 注册的测试**必须**接入 `AbstractAnalysisApiBasedTest`

详见 `../../TESTING_CONVENTIONS.md` 第 1.1 节。

## 依赖

- `:cfir:cfir-serialization`（读取 `.cjo`）
- `:analysis:analysis-api`、`:analysis:analysis-api-platform-interface`、`:analysis:analysis-api-impl-base`
- `:analysis:stubs`、`:analysis:light-declarations`
- `:psi`

## 命令

```bash
./gradlew :analysis:decompiled:assemble
./gradlew :analysis:decompiled:decompiler-to-psi:test
./gradlew :analysis:decompiled:decompiler-to-stubs:test
./gradlew :analysis:decompiled:decompiler-to-file-stubs:test
./gradlew :analysis:decompiled:light-declarations-for-decompiled:test
```

## 相关文档

- `../README.md` — Analysis 模块总览
- `../../TESTING_CONVENTIONS.md` 第 1.1 节
