# analysis/stubs/ — Stub 索引与 Stub 数据模型

对齐 Kotlin `analysis/stubs`。
为 IDE 提供基于 stub 的快速符号索引能力，避免完整解析大文件即可定位类 / 函数 / 顶层声明。

## 关键包

`org.cangnova.cangjie.analysis.stubs.*` — Stub 模型、stub 索引扩展、stub element 类型。

## 典型索引

- 类短名索引
- 函数短名索引
- 包级声明索引
- 继承关系索引
- 注解使用索引

## 测试

测试**必须**接入 Analysis API 测试框架：

- `CaStubSourceGoldenTest` — 源码 stub 黄金测试
- `BuiltinsStubsTest` — Builtins stub 测试
- `CaStubCompiledGoldenTest` / `CaStubCompiledIntegrationTest` — CJO compiled stub 测试

允许保留的纯单元测试（不创建 PSI / project / session）：

- `CaStubSnapshotAssemblerTest`
- `CaStubTreeSummaryExtractorTest`

详见 `../../TESTING_CONVENTIONS.md` 第 1.1 节。

## 依赖

- `:psi`
- `:analysis:analysis-api`、`:analysis:analysis-api-platform-interface`、`:analysis:analysis-api-impl-base`

## 命令

```bash
./gradlew :analysis:stubs:assemble
./gradlew :analysis:stubs:test
```

## 相关文档

- `../README.md` — Analysis 模块总览
- `../../TESTING_CONVENTIONS.md` 第 1.1 节 — Stubs 测试分类
- `../../intellij-ide/docs/design/binary-stub-building-design.md` — IDE 侧 binary stub 构建设计
