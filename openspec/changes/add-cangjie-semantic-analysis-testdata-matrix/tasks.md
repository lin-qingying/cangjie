## 1. 建立覆盖清单与命名约束

- [x] 1.1 在开始编写任何 `diagnostics2` 测试数据前，先使用仓颉文档 MCP 完整阅读相关语言语义，并以官方 C++ 编译器 `Sema/*` 实现继续核对，确保真正读懂对应语义后再进入写样例阶段。
- [x] 1.2 按语义域形成最小证据清单：为每个目标语义域整理文档 MCP 条目、官方 C++ `Sema/*` 对应位置、合法/非法/边界条件摘要，以及仍未确认的歧义点。
- [x] 1.3 只有在语义域证据清单完成后，才依据仓颉文档 MCP、`external/cangjie_compiler/src/Sema/*`、`CfirDiagnosticsList.kt` 和 `diagnostics-coverage-gap-vs-cpp.md` 生成本次测试数据补全的双清单：名称级缺口清单与语义域缺口清单。
- [x] 1.4 为 `cfir/analysis-tests/testData/diagnostics2` 定义本次补全要使用的语义域目录、文件命名规则和批次边界，不要求处理现有生成器或测试扫描契约。
- [x] 1.5 为“项目未定义诊断”确定统一的“内联诊断 + 内联范围内说明”模板，至少包含内联诊断名、建议消息和语义来源。

## 2. 第一批：补当前已定义且可直接触发的名称级缺口

- [x] 2.1 在声明状态与构造器方向补齐已定义且适合直接用内联标记表达的诊断样例，例如 `NO_CONSTRUCTOR` 与 `DEPRECATED_MODIFIER_*`。
- [x] 2.2 在 effects 与推断方向补齐已定义且适合直接用内联标记表达的诊断样例，例如 `MISMATCHING_HANDLE_BLOCK`、`NEW_INFERENCE_ERROR`、`BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION`、`INFERRED_TYPE_VARIABLE_INTO_*`。
- [x] 2.3 对第一批新增 `.cj` 用例执行目录归类复核，确保每个样例既形成名称级保护，又落在正确语义域下。

## 3. 第二批：补薄弱但已有实现依据的语义域

- [x] 3.1 扩充 `call/*`，覆盖普通调用歧义、函数引用歧义、参数名不匹配，以及已有参数绑定错误之外的候选选择语义。
- [x] 3.2 扩充 `pattern/*` 与 `coverage/match/*`，覆盖 `NOT_OVERLOAD_IN_MATCH`、`MATCH_CASE_HAS_NO_TYPE` 与更完整的错误语义分支。
- [x] 3.3 扩充 `const-eval/*`、`jump/*`、`initialization/*`，覆盖 negative shift、shift overflow、更复杂初始化生命周期，以及官方稳定存在的 loop-control 语义。
- [x] 3.4 扩充 `effects/*`、`interop/*`、`generic-access/*`、`mut/*` 等当前目录偏薄的语义域，使其从 smoke coverage 演进为矩阵覆盖。

## 4. 第三批：建立新语义域目录与官方语义占位

- [x] 4.1 为当前 `diagnostics2` 目录要承接的官方稳定语义域制定目录与首批样例规划，例如 `range/`、`throw/`、`try/` 以及更细粒度的 advanced effects / interop 子域。
- [x] 4.2 对项目尚未定义但官方语义明确的场景编写 `.cj` 样例，统一使用内联诊断，并在同一段内联范围内记录建议诊断消息和来源依据。
- [x] 4.3 将后置项如 `common-specific/`、`mock/`、`inout/` 记录为 backlog 或后续批次，而不是与当前可执行批次混排。

## 5. 仅记录语义边界，不处理接线与实现前置条件

- [x] 5.1 对当前项目尚未定义或尚未可触发的诊断场景，使用内联诊断记录诊断名，并在同一段内联范围内补充建议诊断消息和来源依据。
- [x] 5.2 对当前语义设计下仍有争议或实现未收敛的场景，保留语义样例与说明，但不扩展到生产代码或测试接线。
- [x] 5.3 对 `coverage/invalid/`、`CommonTypeCheckers`、`CommonLanguageVersionSettingsCheckers` 等结构性空白，仅在测试数据层面保留后续语义占位说明。

## 6. 收尾与测试数据完整性复核

- [x] 6.1 复核所有规划中的目录和文件命名是否清晰表达语义域，并统一放置在 `diagnostics2` 下。
- [x] 6.2 复核所有未定义诊断占位场景是否都附带完整的内联范围内说明信息，避免仅给出模糊语义描述。
- [x] 6.3 产出最终测试数据编写顺序说明，不要求编译、测试或接入现有测试体系。

## 7. 继续批次：补当前仍明显空白或仅有占位的官方语义域

- [x] 7.1 新建并补齐 `inout/*` 目录，至少覆盖 `inout` 参数要求可变变量、非左值传参、以及与调用绑定相关的核心非法形状。
- [x] 7.2 扩充 `effects/*`，将当前仅有 core smoke 的 effect handler 语义推进到更细粒度的 resumption / handle-block / return-flow 矩阵。
- [x] 7.3 扩充 `interop/*`，从当前的返回类型/参数类型合法性继续推进到更丰富的 FFI / NativeFFI 语义占位与可直接断言样例。
- [x] 7.4 将 `range/*`、`throw/*`、`try/*` 从“占位表达”继续推进为更可信的 runnable semantic matrix，补足元素类型、catch 类型、throw 形状与更多边界条件。
- [x] 7.5 对 `inference/*` 继续拆分占位样例，减少“一个文件挂多个名字”的粗粒度写法，逐步向每个推断分支单独成例推进。

## 8. 后续 backlog：仍未进入本轮的官方语义域

- [ ] 8.1 将 `common-specific/*`、`mock/*` 继续保持为 backlog，并在后续批次进入 `diagnostics2` 时再单独展开。
- [x] 8.2 评估 `visibility/*`、`inheritance/*`、`invalid-declaration/*` 是否需要在 `diagnostics2` 下建立独立目录，而不是长期只依赖旧 `diagnostics/*` 目录。
