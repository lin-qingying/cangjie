## 为什么

当前 Kotlin 版本仓颉编译器在 CHIR 与代码生成阶段只实现了部分逻辑，且生成结果不正确，导致后端不可用于稳定产物构建与回归验证。现在需要补齐完整的 CHIR 与 `CHIR -> LLVM IR` 流水线，并以参考编译器 `external/cangjie_compiler` 的 LLVM IR 作为一致性基线，确保 Kotlin 实现可替代并可持续演进。

## 变更内容

- 完整定义并实现可独立消费的 CHIR 语义模型与校验流程（不包含上游谁生成 CHIR 的约束）。
- 完整实现 `CHIR -> LLVM IR` lowering 与代码生成流程，覆盖函数、控制流、类型、常量、调用约定、内存与运行时交互等核心语义。
- 建立 LLVM API 互操作层（JNI/JNA）与 Kotlin 后端调用封装，参考 `external/kotlin` native 后端模式，提供可测试、可替换实现。
- 建立与参考编译器 IR 产物逐项对齐的验证机制（文本/结构对比、稳定化输出、回归基线），保证最终产物一致性。
- **BREAKING**: 调整现有不正确的 CHIR/codegen 内部接口与数据流，旧的实验性实现路径将被替换为完整实现路径。

## 功能 (Capabilities)

### 新增功能
- `chir-core-semantics`: 定义并实现完整 CHIR 核心语义、模块组织与一致性校验接口，支持后续后端稳定消费。
- `chir-llvm-lowering`: 实现从 CHIR 到 LLVM IR 的全量 lowering 规则与代码生成流程，输出可执行的完整 LLVM IR。
- `llvm-backend-interop`: 提供 JNI/JNA 方式的 LLVM API 访问层与 Kotlin 封装，并建立与参考编译器产物完全一致的验证能力。

### 修改功能

无。

## 影响

- 代码模块：
  - 现有 CHIR 相关模块（需补齐语义与校验链路）。
  - 代码生成与 CLI 编译流程模块（接入完整后端与产物输出）。
  - 测试基础设施（新增 LLVM IR 一致性回归测试与基线管理）。
- 外部依赖与集成：
  - 引入或强化 JNI/JNA 与 LLVM 本地库加载策略。
  - 参考 `external/cangjie_compiler` 的 IR 产物行为与 `external/kotlin` 的 LLVM API 调用模式。
- 风险与约束：
  - 跨平台本地库加载与 ABI 差异。
  - IR 文本稳定性（命名、顺序、元数据）对“一致性”判定的影响。
  - 大范围后端替换带来的回归面扩大，需要分层测试与灰度验证。
