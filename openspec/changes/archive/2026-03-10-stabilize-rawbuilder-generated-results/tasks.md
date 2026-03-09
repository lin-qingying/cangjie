## 1. 测试基线与更新策略收敛

- [x] 1.1 盘点 `cfir/raw-cfir/psi2cfir` 中所有会写回 golden 的断言入口，并统一为显式更新开关控制
- [x] 1.2 实现默认严格比对模式：不匹配时仅失败并输出差异，禁止改写期望文件
- [x] 1.3 实现更新模式：显式开启后允许写回 `*.txt` / `*.lazyBodies.txt` 并保留清晰提示
- [x] 1.4 更新 README 与相关说明文档，确保运行命令与行为一致

## 2. CjBasicType 转换修复

- [x] 2.1 在 CFIR 类型系统中新增 `CfirBasicTypeRef`，并完成渲染器/访问者/必要序列化路径的最小支持
- [x] 2.2 在 `PsiConversionUtils` 中补充 `CjBasicType` 分支并映射到 `CfirBasicTypeRef`
- [x] 2.3 为基础类型（参数、返回值、typealias、extend 接收者）补充/更新针对性测试样例
- [x] 2.4 重新生成并校验受影响 rawBuilder golden 文件，确认不再出现 `Unsupported type element: CjBasicType`
- [x] 2.5 增加 RAW-only 验收用例，验证无需进入 resolve 也能正确输出 `CfirBasicTypeRef` 语义

## 3. LazyBodies By-Stub 稳定性提升

- [x] 3.1 对齐 by-stub 文件构造流程，确保 `.cj` 输入使用正确 file type 与 physical provider
- [x] 3.2 增强基础设施断言：stub 可用、禁止 tree access 规则有效、`UNKNOWN` 路径被阻断
- [x] 3.3 运行 by-ast 与 by-stub 对照测试，确认类型输出一致且失败信号可解释

## 4. 覆盖护栏与回归验证

- [x] 4.1 为 tests-gen 增加 all-files-present 等效覆盖校验，避免新增 testData 漏测
- [x] 4.2 运行 `:cfir:raw-cfir:psi2cfir:test` 并记录机制修复前后差异
- [x] 4.3 对关键回归样例（如 `topLevelFunction`、`controlFlow`）执行定向验证并沉淀结论
