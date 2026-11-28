# 原 default assignment 来源事实测试

`defaultFacts.cj` 逐字复制自本工作树 `build/source-default-facts-v113/defaultFacts.cj`。
源 SHA-256：`a9dfb1d5611ecc9da2ca302ccaa0361eff545090bbc53b22303e59d3eeaa2116`。

来源索引：

- `build/source-default-facts-v113/authority.json`：固定官方 v1.1.3 提交 `14276740ffec1262b81b6b5236b1d840af270b37`、一次成功 SDK 编译、一次成功解码、源码和产物哈希；`sdkActual.parameterRows` 保存八个参数的默认值与 helper 图。
- `build/source-default-facts-v113/native-producers-v113.md`：原 assignment.isConst、literal 身份与参数 TOOL_ADD 的原生 producer 坐标。原 selector 在 `ASTWriter.cpp:1172–1180`，先检查原始事实，再归一化表达式。
- `build/source-default-facts-v113/source_default_facts.json`：官方 CJO 原始解码结果；CJO 不保存 Expr.isConst，原字段以固定提交的 producer 证据为准。
- `build/source-default-facts-v113/defaultFacts_AST/0_parse_ast.txt`：保留原 `ParenExpr`、直接负 literal 与 `-(7)` 的 `UnaryExpr` 语法区别。

本例由 `CfirSourceExpressionExportTest` 和 `CfirSourceExpressionExportLightTreeTest` 的 `originalDefaultAssignmentFactsAndFinalSelection` 共享执行。沿原诊断 harness 与正式 frontend 编译原源码，不设置 CJO 输出目录；只验收原语法、冻结来源 facts 与八个 FINAL 参数槽，不验收完整 SOURCE 正文 codec。原四个正文图 fixture 的断言保持不变。本次未重编 SDK，未运行 Gradle。
