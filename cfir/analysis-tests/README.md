# cfir/analysis-tests/ — CFIR 分析端到端测试套件

承载从 PSI 到 CFIR 全管线（构建 + 解析 + 检查 + 序列化）的端到端测试。基于 `:tests:test-infrastructure` 与 `CfirFrontendFacade`。

## 测试组织

```
testData/
├── diagnostics/        # 诊断测试（inline marker，按语义域分目录）
│   ├── coverage/
│   ├── operator/
│   ├── type-mismatch/
│   ├── call/  constructor/  pattern/  mut/  visibility/  ...
├── diagnostics2/       # 第二批诊断测试
└── (其它 fixture)
```

诊断测试通过 `// DIAGNOSTICS:` directive 与 inline marker（如 `<!ERROR_NAME!>...<!>`）声明期望。详见 `../../TESTING_CONVENTIONS.md`。

## testFixtures

`testFixtures/org/cangnova/cangjie/cfir/` 提供本模块测试共用的基础设施。

## 命令

```bash
./gradlew :cfir:analysis-tests:test
```

更新 golden：

```bash
./gradlew :cfir:analysis-tests:test -Dupdate.test.data=true
```

## 相关文档

- `../../TESTING_CONVENTIONS.md` — 全项目测试约定
- `testData/diagnostics2/README.md` — 第二批数据说明
