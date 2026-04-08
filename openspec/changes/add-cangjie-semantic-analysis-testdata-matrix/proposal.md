## 为什么

当前 `cfir/analysis-tests/testData/diagnostics` 已经覆盖了一批基础语义诊断，但覆盖形态仍以“局部补洞”和“重点目录富化”为主，尚未形成一套基于仓颉真实语义域的完整测试矩阵。仓库内 `cfir/analysis-tests/diagnostics-coverage-gap-vs-cpp.md` 已明确指出：现有 suite 在调用绑定、声明状态、effect handler、interop、range / throw / catch / jump、invalid declaration 等方向仍存在薄弱区或结构性空白，而官方 C++ 编译器 `external/cangjie_compiler/src/Sema/*` 已经稳定体现出这些语义域。为避免与现有回归目录耦合，本次提案改为在新的 `cfir/analysis-tests/testData/diagnostics2` 下组织测试数据矩阵。

现在推进这项变更的原因很直接：后续要按真实仓颉语义补全 producer / checker，实现需要一套可追溯、可按语义域分层推进的测试数据基线，而不是继续在零散目录中被动追加个例。

## 变更内容

- 增加硬性前置条件：在编写 `cfir/analysis-tests/testData/diagnostics2` 中的任何测试数据之前，必须先完整阅读并尽可能读透仓颉语言语义，优先使用仓颉文档 MCP 获取官方文档，再结合仓颉语言官方编译器 C++ 实现核对真实语义行为；不得在未读懂语义时凭猜测编写测试数据。
- 为 `cfir/analysis-tests/testData/diagnostics2` 建立一套按仓颉语义域组织的诊断测试数据矩阵，覆盖当前项目已定义诊断与官方 C++ 语义中稳定存在但本项目尚未命名建模的场景。
- 明确测试数据编写约束：以真实仓颉语义为准，所有诊断场景一律使用内联诊断标记；对于项目尚未定义的诊断，除内联诊断名外，还必须在同一段内联诊断范围内紧贴错误代码补充建议诊断消息与来源说明（例如 `<!DIAG!>/* suggested message */code<!>`）。
- 以 OpenSpec 形式约束测试数据覆盖范围、目录策略、诊断命名策略、优先级分层，以及“当前项目已定义诊断”和“尚未定义但仍需使用内联诊断并附带内联范围内建议消息说明的诊断”两类场景的表达方式。
- 不在本次变更中实现 Kotlin 生产代码、diagnostic factory、renderer、checker 注册修复，也不要求将 `diagnostics2` 接入现有测试体系或执行编译/测试验证；本次变更只编写测试数据文件本身。

## 功能 (Capabilities)

### 新增功能
- `cfir-semantic-diagnostics-testdata`: 为 CFIR 语义分析建立一套以官方仓颉语义和项目现有诊断体系为依据的诊断测试数据覆盖规范，面向 `cfir/analysis-tests/testData/diagnostics2` 的测试数据补全与分层推进。

### 修改功能

## 影响

- OpenSpec 产物：`openspec/changes/add-cangjie-semantic-analysis-testdata-matrix/` 下的 proposal、design、specs、tasks。
- 目标测试区域：`cfir/analysis-tests/testData/diagnostics2/**/*`。
- 参考实现与证据来源：
  - 仓颉文档 MCP（作为语义理解的第一入口）
  - 项目诊断定义：`cfir/checkers/checkers-component-generator/src/org/cangnova/cangjie/cfir/checkers/generator/diagnostics/CfirDiagnosticsList.kt`
  - 现有覆盖缺口盘点：`cfir/analysis-tests/diagnostics-coverage-gap-vs-cpp.md`
  - 官方语义基线：`external/cangjie_compiler/src/Sema/Diags.h` 与 `external/cangjie_compiler/src/Sema/TypeCheck*.cpp`、`PatternUsefulness.cpp`、`LegalityOfUsage/*`
- 下游影响：后续实现者可以直接按语义域使用这些 `.cj` 测试样例推进真实诊断实现；本提案本身不要求这些样例立即被现有测试体系发现或执行。

## 前置条件

- 在开始编写任何 `diagnostics2` 测试数据之前，必须先完整阅读并理解相关仓颉语言语义。
- 语义理解的来源顺序必须是：
  1. 仓颉文档 MCP
  2. 仓颉语言官方编译器 C++ 实现
  3. 本项目现有实现与诊断定义
- 如果文档与官方 C++ 实现仍不足以支撑某个语义场景，当前阶段不得凭猜测写测试数据；应先把语义不确定性标记出来，再决定是否保留该场景。
- “完整阅读并理解”必须按语义域逐块完成，不能靠零散查阅或凭印象跨域补样例。
- 每个准备编写测试数据的语义域，在进入写样例阶段前都必须先形成一份最小证据清单：
  - 相关仓颉文档 MCP 条目
  - 对应官方 C++ 编译器 `Sema/*` 或诊断定义位置
  - 该语义域的合法形式、非法形式和边界条件摘要
  - 当前仍未确认的歧义点（如果有）
- 没有完成上述证据清单的语义域，不视为“已经读懂”，不得开始编写对应测试数据。
