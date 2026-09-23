# 宏测试失败 × 诊断缺口报告交叉分析（2026-09-22）

- 输入一：`build/cfir-macro-test-failures-2026-09-22.md`（28 个独立宏用例失败分类）
- 输入二：`cfir/analysis-tests/diagnostic-gap-and-dead-analysis-20260922.md`（官方 1072 vs CFIR 527 诊断缺口/死声明盘点）
- 目的：判断宏测试失败中哪些已被缺口报告解释、哪些是新发现、以及两份报告的修复优先级如何合并。

---

## 1. 总体结论

宏测试失败与缺口报告**几乎不重叠但方向互补**：

- 缺口报告回答"**CFIR 缺什么诊断、多什么死声明、哪些没门禁**"——是诊断**清单层面**的完整性问题；
- 宏测试失败暴露的是"**宏展开产物进入分析面后，既有检查器的数据流断链**"——是诊断**机制层面**的正确性问题。
- 两份材料唯一真正交汇的宏域条目：`sema_expand_macro_redefinition`（§1.1C 真缺失）与 `packages_macro_inconsistent`（§1.2）——**缺口报告承认宏域有缺失，宏测试失败则证明宏域已有实现的部分也在漏报/误报**。宏域是两边共同的薄弱区。

---

## 2. 逐类交叉判定

### 2.1 APILEVEL_REF_HIGHER 漏报 ×12（宏测试分类一/漏报）→ **新发现，缺口报告未覆盖，升 P0 修**

- 缺口报告涉及 APILevel 的只有两条，且都不相关：
  - §2.1 P2：`CFIR_APILEVEL_MISSING_ARG` 门禁缺口（missing-arg 分支）——是**多报方向**的门禁问题；
  - §1.3：`sema_apilevel_*` v1.2 诊断——前瞻登记。
- 机制定位（本轮代码核实）：`CfirApiLevelRefHigherChecker.kt:107`
  `val apiLevel = availability.ownApiLevelInfo(declaration) ?: continue` ——
  对**宏展开产物中的声明**，`ownApiLevelInfo` 取不到 `@APILevel` 注解（展开链路没有把依赖声明上的 APILevel/Syscap 属性带过来），`?: continue` **静默跳过**，于是 12 处 level-3 引用全部漏报。
- 性质：这是"展开产物属性保真"问题，和诊断清单无关——**缺口报告的任何一项修复都不会消除它**。6 个 fixture 用例一个机制修复全收。

### 2.2 import 误报族 ×9 用例（宏测试分类二之 1）→ **与 §2.1 P2 `UNUSED_IMPORT` 门禁条目相交，但根因独立**

- 机制定位（本轮代码核实）：`CfirImportsChecker.reportUnusedImports`（:245-280）**已经有宏豁免机制**：
  `macroExpansionRegistry.usedMacroNames(...)`（:268）、`usage.targets.macroPackages`（:276）、`referencesUsedMacroPackage`（:278），
  外加 :206/:211 的两处提前豁免。
- 豁免机制的覆盖语义是"**本文件内的宏调用/展开消耗了该 import**"。失败用例里漏网的是另一种形态：
  **依赖文件自己声明的宏，其宏体（定义体）内部使用了该 import**（如 dep1.cj 的 `import ohos.labels.APILevel` 只被宏体引用；`define_10004` 包的宏定义文件）——
  使用事实在"宏定义体"里，而 `usedMacroNames` 只看"宏名消耗"，不看"宏体内部引用"。豁免机制存在但语义差一角。
- 与缺口报告的交叉：§2.1 P2 讨论的是 `UNUSED_IMPORT` 是否钉 1.0.2 门禁——**门禁决策不解决本问题**（默认版本下照样误报），但两事应合并成一个 `UNUSED_IMPORT` 专项：先修宏体使用事实收集，再决定门禁版本，避免两次动同一 checker。
- `UNRESOLVED_IMPORT`（`define_10004`、`a.*`）走 :234 的 terminal-import-target 解析失败分支：宏定义包作为 import 目标在 import 解析阶段就不可见，属**符号解析面**问题，同样不在缺口报告清单内。

### 2.3 NOT_A_TYPE 误报 ×3（宏测试分类二之 2）→ **新发现**

- 缺口报告无对应条目。宏生成的注解类型（`@A`、`import FFF as TT` 后 `@TT.QA2`）在注解身份解析时未走通，与 §2.3 确认门禁正确的"17 个 OBJC 注解身份诊断"是**不同机制**（那是注解类本身，这是宏生成注解的可见性）。归入 2.2 同一根因族：宏展开产物的符号表完整性。

### 2.4 ObjC/Java 互操作占位多报 ×3 用例（宏测试分类二之 3）→ **与 §2.1 P1 门禁缺口部分相交，需分开裁决**

- `JAVA_MIRROR_INTEROPLIB_MUST_BE_IMPORTED`：缺口报告 §2.1 P1 已点名——该诊断在"普通 @Java（1.0.0）也满足触发条件"上超前报错。测试中期望与实际**都含**此诊断（fixture 写了 `@JavaMirror` 却没 import interoplib，期望反而没报 `MISSING_JAVA_INTEROP_ANNOTATION`），说明 fixture 期望本身也滞后于 §2.1 的结论——**缺口报告的 P1 修复落地后，这组 fixture 要同步重生成**。
- 多报的 OBJC 结构性诊断（`OBJC_IMPL_MUST_HAVE_OBJC_MIRROR_SUPER_CLASS` 等 ×4 类）：§2.3 说 17 个 OBJC 注解身份诊断已由 `ObjCInteropAnnotations` 正确门禁，但这 4 个是**结构检查器**（mirror 继承关系、ForeignName），不在门禁族里。测试里 fixture 显式用了 `@ObjCImpl`，门禁也压不住——真正问题是**占位 fixture 的期望只写了 JAVA 侧没写 OBJC 侧**，倾向"期望过期"，修 fixture 即可；但也暴露这 4 个结构诊断无版本门禁（官方 1.1.0 形态），可顺手挂到 §2.1 P1 的 OBJC 门禁批次。

### 2.5 Fuzz 展开产物多报（TYPECAST_OVERFLOW ×125 / UNREACHABLE_PATTERN ×143 等，4 用例）→ **新发现，量最大**

- 缺口报告完全不涉及（`sema_exceed_num_value_range`→`CFIR_LITERAL_NUMERIC_OVERFLOW` 是字面量溢出，与此处的**类型转换溢出** `TYPECAST_OVERFLOW` 不同名不同义，映射表里也无一对应）。
- 模式高度一致：`UInt64(Int16(-120))` 这类嵌套窄化构造调用在宏展开产物里被逐个标溢出。两个可能方向（需一轮取证）：
  1. cjc 对**宏展开产物**内的整型转换溢出根本不报（展开代码豁免 const-eval 类检查）；
  2. CFIR 的 TYPECAST_OVERFLOW 对非字面量实参的常量折叠过度。
  按"框架正确性优先"，先跑 cjc 对照一条样例即可裁决，不要先改检查器。

> **裁决（2026-09-23，已实测；详见 `REPAIR_LOG.md` 同日条目）**：**两条假设均被否证，方向反转——CFIR 报得对，过期的是 fixture 期望。**
> - 被标构造的**真实形态**不是良性窄化，而是**负值 → 无符号类型**：`UInt64(Int8(-82))`、`UInt64(Int16(-120))`、`UInt32(Int32(Int16(-6)))`、`UInt64(Int64(-65))`、`UInt16(Int8(-26))`、`Int8(UInt8(137))`。
> - cjc 1.0.5 与 1.0.0 **逐项一致**：上述 7 种构造 **7/7 报 `chir_typecast_overflow`**（`integer type conversion overflow` + `range of UInt64 is 0 ~ …`）；而**同形但取值合法**的对照（`Int16(UInt16(47))`、`Int64(UInt8(254))`、`UInt64(190)` …）**零诊断**。⇒ 官方判据是"常量值是否落在目标类型范围内"。
> - `UNREACHABLE_PATTERN` 同理：穷尽枚举匹配后的 `case _` 在双 SDK 下均报 `sema_unreachable_pattern`（`unreachable pattern`，属 `unused` 警告组）。
> - 附带旁证：fixture 实际文件跑 cjc 时解析期报 `parse_redundant_modifier`×4，与 CFIR 的 `REDUNDANT_MODIFIER`×4 数目一致。
> ⇒ 事项 6 的处置改为：**按 cjc 逐构造核验后补齐 `defaultParameter_pkg_02/**` 的 fixture 标记**（约 240 处，机械工作），**不动检查器**。该 5 个用例应从"待修缺陷"移出。
> 证据：`codex-probes/flow_operand_failure/p26_*`（合法取值，零诊断）、`p27_*`（越界取值，7/7 报）、`p28_*`（unreachable）。

### 2.6 INVALID_CALLED_OBJECT vs UNRESOLVED_REFERENCE 口径 1 例 → **独立口径问题，与缺口报告 §3 冗余清理同性质**

- 同一表达式上两个诊断实体的选择分歧。缺口报告 §3.B 已列了 14 组"多声明 vs 实际承载者"的落名错位；本例是第 15 组的候选（`INVALID_CALLED_OBJECT` 覆盖了 `UNRESOLVED_REFERENCE`+`UNDECLARED_TYPE_NAME` 的场景），**应并入 §3 的落名对齐批次**一起裁决，而不是单独修。

### 2.7 仅格式差异 3 例 → 不与缺口报告交叉，最后统一处理。

---

## 3. 宏域在缺口报告中的定位补强

缺口报告对宏域的处置有三处，宏测试失败为其补上"实现侧"证据：

| 缺口报告条目 | 宏测试失败给出的补充判断 |
|---|---|
| §1.1C：`sema_expand_macro_redefinition` 真缺失 | 宏测试套件（CfirAnalysisMacroTestGenerated 1250 个 XML）里也没有覆盖它的用例——**缺口+测试盲区双重确认**，补检查时应同时补 LLT |
| §1.2：`packages_macro_inconsistent`（1.1.0） | 同上，属配置层，登记即可 |
| §1.4：`macro_*` 21 项划给 `macro:macro-process` | 界限成立，但宏测试失败证明**展开产物一进入 CFIR 分析面，属性保真（APILevel）、使用事实（import）、符号可见性（注解类型）三条数据流就断**——这条边界本身需要一份契约（展开产物必须携带哪些 sidecar 属性/使用事实），否则每个检查器都要各自打补丁 |

第三点是本次交叉分析最重要的架构结论：**与其在各检查器里逐个补宏豁免/宏属性（imports checker 已经这样做了，仍漏一角），不如在 `macroExpansionRegistry` / 展开产物 IR 上补齐统一的事实来源**——这正符合"先问框架是否正确"的纪律。

---

## 4. 合并后的修复优先级（覆盖两份报告）

| 级别 | 事项 | 来源 | 收益 |
|---|---|---|---|
| **P0** | 宏展开产物属性保真：`ownApiLevelInfo` 对展开声明取到 APILevel/Syscap | 宏测试（缺口报告未覆盖） | 消除 12 处漏报、6 用例 |
| **P0** | COMMON/CJMP 16 个整族门禁 | 缺口报告 §2.1 | 唯一整族超前报错 |
| **P1** | 宏域统一事实来源（展开产物携带使用事实/符号可见性），修 import 误报族 + NOT_A_TYPE | 两份合并 | 消除 ~12 用例 |
| **P1** | `UNUSED_IMPORT` 专项合并处理（宏体使用收集 → 再裁 1.0.2 门禁） | 两份合并 | 同一 checker 只动一次 |
| **P1** | OBJC/JAVA 门禁批次 + 同步重生成 Interop 占位 fixture | 缺口报告 §2.1 + 宏测试 §2.4 | 门禁与期望一次对齐 |
| **P2** | TYPECAST_OVERFLOW/UNREACHABLE_PATTERN 对展开产物的 cjc 对照取证 | 宏测试 | ~~~240 处多报的方向裁决~~ **已裁决（2026-09-23）：方向反转为 fixture 侧，非缺陷** |
| **P2** | §3 死声明清理（17 冗余 + 5 废弃）+ 本例 `INVALID_CALLED_OBJECT` 口径并入 | 两份合并 | 清单收敛 |
| **P2** | 补 `sema_expand_macro_redefinition` 等宏域缺失检查 + 配套 LLT | 两份合并 | 缺口+盲区双清 |
| P3 | 格式差异 3 例 | 宏测试 | 收尾 |
