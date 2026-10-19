# CFIR 缺口登记簿（cfir2chir 视角）

> **用途**：`chir:cfir2chir` 的输入是 CFIR。在实现 cfir2chir 的过程中发现的、
> 根因落在 CFIR（cfir-tree / resolve / checkers / serialization）一侧的问题登记在此，
> **cfir2chir 一侧不得为绕过这些缺口修改 CFIR**；由 CFIR 侧排期修复，修复后从本文档勾销。
>
> **登记规则**：每条缺口一节，包含：现象、cfir2chir 侧现状（如何失败/占位）、
> 根因归属（CFIR 哪个模块/阶段）、预期修复方向。解决后在标题加 `[已解决]` 并注明提交。
>
> **基线**：登记日 2026-10-17，main 分支。cfir2chir 输入契约详见各转换器源码的
> 防御性检查（`Cfir2ChirConversionException` 家族）。

---

## 缺口 1：包外可调用符号缺少后端 ABI 元数据

- **现象**：函数体中解析到**当前转换包之外**的可调用符号（如 `std.core` 的函数、
  跨包用户函数）时，cfir2chir 只能生成裸 `ChirImportedFunctionValue`：
  名称是 `callableId.toString()`，**不带任何后端可消费的 ABI 属性**
  （`jvm.owner` / `jvm.name` / `jvm.invokeKind` / `jvm.descriptor`）。
  JVM/LLVM 后端消费该值时立即抛 `missing required JVM ABI attribute`。
- **cfir2chir 侧现状**：`Cfir2ChirFunctionBodyConverter.importedFunctionValue` /
  `importedVariableValue` 按当前能力生成 imported value；无法从 CFIR symbol
  上取到目标平台 ABI 信息（CFIR symbol 没有承载这些信息的字段）。
- **根因归属**：CFIR serialization / providers。
  `.cjo` 反序列化出的 symbol（`CfirNamedFunctionSymbol` 等）不携带
  目标后端 ABI 描述；内建（builtin）符号同样没有登记表。
- **预期修复方向**（CFIR 侧）：
  1. `.cjo` 元数据写入/读出目标平台 ABI（owner/name/invokeKind/descriptor），
     挂到 symbol 或 declaration attributes；
  2. 或提供内建符号 ABI 登记表（std.core 常用函数的 JVM/LLVM 签名映射），
     供 cfir2chir 通过 symbol 查询。
- **临时规避**：无（测试均用显式 ABI 属性手工构造 imported value 绕过）。

---

## 缺口 2：反序列化桩树含 `CfirLazyExpression`，不可直接进后端

- **现象**：`StubBasedCfirMemberDeserializer`（analysis 侧）在反序列化
  字段 initializer / 参数默认值时填 `buildLazyExpression()` 占位。
  cfir2chir 遇到 `CfirLazyExpression` 一律抛
  `"lazy CFIR expression must be materialized before CHIR lowering"`。
- **cfir2chir 侧现状**：按契约拒绝，不静默兜底。这是**正确行为**：
  官方编译器同样不在后端消费未落实表达式。
- **根因归属**：不是缺陷，是**使用方式约束**——反序列化桩树必须先跑
  BODY_RESOLVE 把 lazy 表达式落实后才能送 cfir2chir。
  若未来希望后端直接消费桩树，需要 CFIR 侧提供
  “按需 materialize lazy initializer”的落水道（对齐 K2 的
  `FirBodyResolveTransformer` 对 deserialized FIR 的处理）。
- **预期修复方向**（CFIR 侧，可选）：为 deserialized CFIR 提供
  body re-resolve 入口，或 `.cjo` 直接序列化完整函数体（当前写入侧仍在补齐，
  见 `docs/cjfir-compiler-stages.md` SAVE_CJO 一节）。
- **登记原因**：一旦把 cfir2chir 接入前端管线并对**依赖包**（`.cjo` 输入）
  做转换，该缺口会立即成为阻塞项。

---

## 缺口 3：`CfirMainFunction` 的参数/返回约束无 CHECKERS 强制

- **现象**：cfir2chir/JVM 后端对 `main` 的 main-bridge 判定要求
  “无参 + Unit 返回”。CFIR CHECKERS 目前没有对 `CfirMainFunction`
  形状的诊断（仓颉语言要求 `main(): Unit` 且无参数，但错误形态在前端
  报不报、报什么，取决于 checker 是否已实现）。
- **cfir2chir 侧现状**：非标准 `main` 只是**不生成 bridge**（静默降级），
  不会误生成错误入口；但用户得不到“为什么我的 main 没入口”的解释。
- **根因归属**：`cfir:checkers`——缺 `CfirMainFunction` 形状 checker。
- **预期修复方向**（CFIR 侧）：在 CHECKERS 阶段增加 main 函数形状诊断
  （对齐官方 `cjc` 的 main 签名检查）。
- **cfir2chir 侧不阻塞**：可先实现，等 checker 落地后用户体验闭环。

---

## 缺口 4：接口 default 成员的 body 存在性无显式标记

- **现象**：cfir2chir 需要区分“接口成员是 abstract（无 body）”与
  “default 实现（有 body）”。当前 CFIR 上，接口成员函数 body 为 null
  与 “有 body” 只能靠 `function.body == null` 判断，
  而 body 为 null 的函数在 cfir2chir 统一降级为 `cfir.absent_body` 占位操作，
  **丢失了“这是 abstract 声明”还是“body 恰好缺失”的语义区分**。
- **cfir2chir 侧现状**：接口成员一律生成抽象方法头；带 body 的接口成员
  （default 实现）的函数体语义被丢弃（登记于 cfir2chir 待办，非 CFIR 缺口）。
- **根因归属**：CFIR 声明模型——`CfirFunction` 无
  “成员来源是 interface default”的显式标记或属性；
  cfir2chir 无法从形状上恢复该语义。
- **预期修复方向**（CFIR 侧）：为接口 default 成员加显式 origin/attribute
  （如 `CfirDeclarationOrigin.InterfaceDefault`），或 checker 保证
  接口成员 body 存在性语义一致。
- **注**：该项同时是 cfir2chir 待办（JVM 侧 default 方法发射），双边推进。

---

## 缺口 5：`ConeFunctionType` 的 C 函数 ABI 未建模

- **现象**：`Cfir2ChirTypeMapper` 遇到 `isCFunc` 或 `hasVariableLenArg`
  的函数类型直接抛异常。CHIR 当前无 C 调用约定表达
  （这半边是 CHIR 的缺口），但 CFIR 侧该类型**本身携带的调用约定细节**
  （calling convention、变长参数布局）也没有完整结构化字段可查。
- **根因归属**：双方都缺——CFIR 缺 C ABI 结构化描述，
  CHIR 缺 C calling convention 承载。CFIR 侧先行：
  给 `ConeFunctionType` 补结构化 C ABI 字段，CHIR 侧跟进建模。
- **优先级**：低（C 互操作在真实程序里占比小）。

---

## 缺口 6：`CfirForInExpression` 不携带循环协议方法的解析 symbol

- **现象**：cfir2chir 对 for-in 循环生成三个专有操作
  （`cfir.for_in_iterator` / `cfir.for_in_has_next` / `cfir.for_in_next`），
  后端全部 unsupported。要把它们降级为真实调用
  （iterable.iterator() / iterator.hasNext() / iterator.next()），
  需要这三个协议方法的 JVM ABI（owner/name/descriptor）。
- **cfir2chir 侧现状**：`CfirForInExpression` 只携带 iterable 表达式和循环变量，
  **不携带解析后的 iterator/hasNext/next 函数 symbol**——resolve 层只解析
  iterable 本身，循环协议方法是隐式约定的，没有落到 CFIR 树上。
- **根因归属**：CFIR resolve——for-in 的循环协议调用没有显式化。
  官方编译器在 Sema 阶段就把 for-in 降为显式的协议方法调用序列
  （见 `external/cangjie_compiler/src/CHIR/CHIRBuilder.cpp` 的 ForIn 处理），
  CFIR 对齐后 cfir2chir 即可按普通成员调用降级。
- **预期修复方向**（CFIR 侧）：BODY_RESOLVE 阶段把 for-in 展开为
  显式的 `val it = iterable.iterator(); while (it.hasNext()) { val x = it.next(); ... }`
  调用序列，或至少在 `CfirForInExpression` 上附加三个协议方法的
  resolved symbol。**不修改 CFIR 前，cfir2chir 无法安全降级**：
  硬编码 std.core 方法名违反"后端禁止从展示名猜 ABI"的架构约定。
- **临时状态**：for-in 保持 `cfir.for_in_*` 三占位操作；JVM 后端继续拒绝。

---

## 缺口 7：具名实参在 BODY_RESOLVE 后未按形参顺序重排

- **现象**：cfir2chir 已把 `CfirNamedArgumentExpression` 消化为内层值透传
  （后端按实参位置发射）。当源码具名实参乱序（`f(b = 1, a = 2)`）时，
  `argumentList.arguments` 仍保持源码顺序，与形参顺序不匹配——
  按位置消费的后端会错位。
- **cfir2chir 侧现状**：透传语义正确（这是后端唯一可执行的消费方式）；
  具名实参的正确性完全取决于 CFIR 是否在 resolve 后按形参顺序重排实参列表。
- **根因归属**：CFIR resolve——对齐官方行为，具名实参在调用绑定完成后
  应解包（unwrap）并按形参槽位重排（官方 FIR 在 `FirCallCompleter` 中完成；
  仓颉官方编译器在 Sema 调用绑定后同样消除 named wrapper）。
- **预期修复方向**（CFIR 侧）：BODY_RESOLVE 的调用完成阶段把
  `CfirNamedArgumentExpression` 解包并按 `argumentName` 匹配形参重排到
  正确槽位。修复后 cfir2chir 透传自动正确。
- **风险控制**：修复前，乱序具名实参的调用在 JVM 后端产生错误绑定；
  正序具名实参（常见场景）不受影响。

---

## 缺口 8：调用实参的理想字面量类型在 BODY_RESOLVE 后未回写定型

- **现象**：`--emit-jvm` 管线探针（含 `add(1, 2)` 形态的调用）在
  cfir2chir 处失败：`ideal literal type IDEAL_INT must be resolved
  before CHIR lowering`。字面量 `1`/`2` 作为 `add(Int64, Int64)` 的
  实参，BODY_RESOLVE 完成后仍是 IDEAL_INT——期望类型导向的定型
  没有回写到字面量的 `coneTypeOrNull`。
- **cfir2chir 侧现状**：契约上拒绝 IDEAL_* 是正确行为（官方后端同样
  不消费理想类型）；无法在 cfir2chir 侧补定型——期望类型信息
  （形参类型）只在 resolve 阶段可用。
- **根因归属**：CFIR resolve 的调用完成阶段。`IdealTypeResolver`
  （cfir-cones）已提供 `resolveIfIdeal(type, targetType)` 基建，
  但 BODY_RESOLVE 的实参目标类型传播没有把定型结果回写到
  `CfirLiteralExpression.coneTypeOrNull`。对齐官方：Sema 的
  `ReplaceIdealTy` 在调用绑定完成后对实参执行（见
  `external/cangjie_compiler/src/sema` 的 TypeChecker 对位）。
- **影响面**：这是 `--emit-jvm` 真实程序编译的**首个硬阻塞**——
  任何含整数字面量实参的调用（几乎所有真实程序）都会触发。
- **预期修复方向**（CFIR 侧）：调用完成（CallCompleter 对位）阶段
  对每个实参执行 `resolveIfIdeal(argument.coneType, parameterType)`
  并 `replaceConeTypeOrNull` 回写；`let` 初始化器、match selector
  等独立场景已由 `replaceIdealTypes` 覆盖（基建在，回写缺）。
- **临时探针调整**：管线执行验证测试用显式类型标注的源码
  （如 `let x: Int64 = 1`）回避，待 CFIR 修复后移除标注。

---

## 剩余 `cfir.*` 占位操作的消化分类（2026-10-18 盘点）

### 已消化（cfir2chir 已降级，JVM 后端可执行）

短路 AND/OR（控制流+phi）、成员函数调用（receiver-first）、
this/super receiver（this 局部值）、成员字段读写（getfield/putfield）、
VArray 字面量（newarray+arrayStore）、单下标读写（arrayLoad/arrayStore）、
引用窄化 smart cast / as（checkcast）、局部/匿名函数（辅助声明收集 +
MethodHandle 物化）、枚举 case 引用（getstatic 常量）、
具名实参（透传内层值，见缺口 7 的排序前置）、
无 payload enum constructor（getstatic 常量函数体）、
try-catch 异常绑定与多 catch 类型分派（unwind 绑定 + 类型化异常表）、
字符串插值（chir.string_build/append/result 平台中立 intrinsic 族，
JVM 载体 StringBuilder）、
tuple 字面量与 pattern 提取（chir.tuple_pack/extract，
JVM 载体 `Object[]`，元素装箱 + 下标取值）、
finally 出口内联（try/catch 各 fallthrough 出口复制 finally 体，
对齐 javac 的出口复制方案）。

### 需调用约定设计（inout 与带 payload 构造的引用/构造 ABI）

- **`cfir.inout_argument`**：inout 是地址传参语义；JVM 没有引用传参，
  需要调用约定层的模拟设计（临时变量 + 调用后写回，或 ref 载体对象）。
  非纯透传，保留占位直到约定落地。
- **带 payload enum constructor**：后端 enum `<clinit>` 尚只实例化
  无参 case；带 payload 的构造需要枚举构造器 ABI（`NEW + <init>(payload)`）。

### 需 CHIR 模型扩展（chir-tree 演进项，先设计后实现）

- **finally 的 return/throw 穿透**：出口内联已覆盖正常与 catch 路径；
  try/catch 体内直接 return/throw 时 finally 不执行——完整语义需要
  return 重写机制（把 return 改写为"暂存结果 → 执行 finally → 真正 return"），
  属于 CHIR 控制流层的更深的演进。

### CFIR 缺口（本文档登记，等 CFIR 侧修复）

- **`cfir.for_in_iterator/has_next/next`**：缺口 6。
- **`cfir.range`**：Range 类型的构造/迭代 ABI 来自 std.core，
  CFIR 未携带协议 symbol（与 for-in 同根因）。

### 语言特性类（暂缓，真实程序占比低）

`cfir.spawn`（线程 ABI）、`cfir.synchronized`（monitor ABI）、
`cfir.perform/resume/handle`（effect 系统）、`cfir.quote`（宏展开期应已消除）、
`cfir.annotation*`（注解求值期语义）。

---

## 已确认**不是** CFIR 缺口的事项（避免误登记）

- **`cfir.*` 占位操作的消化**（member_access、call_with_receiver、
  array_literal 等 44 种）：这是 cfir2chir 自身的实现待办，
  输入信息已足够（resolved symbol + receiver + 类型），
  只是 cfir2chir 尚未写出降级逻辑。
- **错误类型/lazy/stub 的拒绝**：cfir2chir 契约如此，官方行为一致；
  前端接管线时应设“有诊断不进后端”门卫（前端管线层职责）。
- **跨包文件必须一起送入转换**：`DefaultCfir2ChirConverter.convert`
  的既有设计（单包转换），调用方职责。
