# RawArray F4-A/B 定向复审（2026-11-18）

**本轮限定 gate：REJECT。F4-A、F4-B 两个原 findings 的修订 ACCEPT，原触发路径已关闭；新边界 F4-C（重复 TP 绑定仍以物理 Cone 相等裁决）阻塞 RawArray 双表示在真实 extend matcher 中的一致性。**

本报告只复审 [上一报告](source-frozen-encoder-gate-final-20261118.md) 的 F4-A/B 修订及关联的新边界，不改旧报告，不修改生产/测试，不运行 Gradle、cjc 或测试，不联系旧会话。文件名中的 closed 不代表剩余边界或完整 S4 已获签发。

唯一工作树：`本记录所指的唯一实现树`。实际 HEAD：`b4dd742d589e4b5ee6592d454634c44f634df08a`；修订包含未提交/未跟踪文件，不能用 HEAD 代替文件内容身份。当前 TypeUtils SHA-256 为 `E5E5DB99CF3D43EFD753152FB00603C4588E49DD90659503807E120899A3473D`，ConeInferenceContext 为 `5689F54C1C78055116E1F322197FE9EA0671D1C6AA2A28B77665CDFE3C018975`，CfirExtendSubstitution 为 `80FFF26C10B05D34FC6D0AD345F3351F10C46F172A73AF6A8BA7B5D201AD7AC5`。完整字节绑定见文末，写报告前复核 18 个摘要及 HEAD 均未变化。

固定 Kotlin 对象：`3c7a5041f238102285764d370665f56f587f9dc4`；固定官方仓颉对象：`14276740ffec1262b81b6b5236b1d840af270b37`。上游正文通过 `external/kotlin`、`external/cangjie_compiler` 中的 `git show <固定对象>:<文件>` 读取，固定对象搜索只用于定位。

用户提供的 `fc048caa` 候选 184/184 是上轮信息，本报告没有复验其日志，也没有将其继承为当前新字节的测试结果。当前批次仍 **待测试**；本次结论为静态考证。

## 原 findings 的修订裁决

| 项目 | 当前证据 | 裁决 |
|---|---|---|
| F4-A：函数替换/erase 在物理嵌套后重计维度 | ConeInferenceContext L709–712 的函数重载先取 typeSystemArguments，L726–739 的 erase 同样读取规范叶子；两者调用 L669–675 的列表重载，以原累计维度重建。公共 deep 操作继续调用函数重载。 | **ACCEPT／原 finding CLOSED**。原二维恒等替换变三维的路径消失。 |
| F4-B：二维 wire receiver 的 T 被绑定为物理叶子 | TypeUtils L31–37 的 declarationTypeArguments 与 L26–28 的通用 marker 叶子域分开；RawArray 返回现有稳定 lazy 降维视图。matcher L406–419 按声明实参递归，predicate L173 也统一查询。 | **ACCEPT／原 finding CLOSED**。一层 RawArray<T> 对二维 flat/nested 都绑定 T 为一维 RawArray，而不是 Int64。 |

F4-A 的修订遵守固定 Kotlin `compiler/fir/semantics.api/src/org/jetbrains/kotlin/fir/types/ConeInferenceContext.kt` L377–386 的同一替换合同；固定公共 `core/compiler.common/src/org/jetbrains/kotlin/types/model/TypeSystemContext.kt` L256–264 的 deep 算法仍通过函数重载组合。仓颉的规范叶子与累计维度对应官方 TypeManager.cpp L279–286 的数组实例化，以及 L113–126 的 GetArrayTy 累加规则。新的函数恒等替换不再把旧内层数组当作新叶子重复计维。

F4-B 的 [公共查询](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/TypeUtils.kt)（L31） 位于已有 semantics.api 类型工具 owner，没有在各调用点独立重建降维数组。以下实际入口均已消费它：

| 消费入口 | 当前行号 |
|---|---|
| CfirClassSubstitutionScope.createClassLikeDeclarationSubstitutor | L791–796 |
| CfirTypeResolveTransformer.assumptionTypeArguments，以及 exposeExtendedTypeAssumptionBounds | L920、L808–821 |
| ConeInferenceContext.createSubstitutorForSuperTypes | L935–942 |
| CfirTypeAwareSupertypeProviderImpl.createDeclarationSubstitutor | L511–516 |
| CfirExtendSubstitution.matchExtendTargetType 的名义分支 | L406–419 |
| CfirExtendSubstitution.extendPredicateTypeArguments | L173 |

[rawArrayTypeArguments](../../cfir/cfir-cones/src/org/cangnova/cangjie/cfir/types/ConeClassLikeType.kt)（L120） 每个 Cone 只创建一次声明视图；二维输入返回规范叶子 Int64、维度 1 的数组，三维返回维度 2。实际 provider L412–426 消费匹配的 substitutor 生成接口父边，L435–443 再计算 predicateVisible。一层目标 RawArray<T> 的 T 在第一层声明视图即完成绑定，当前代码没有用数组维度立即相等来误拒泛型匹配。

固定官方 TypeManager.cpp L1876–1887 的 GetTypeArgs、L1367 的 GetAllExtendInterfaceTy、L1595–1598 的 CheckExtendWithConstraint，以及 L1284–1328 的实际接口实例化 helper 支持上述职责区分。通用 marker 查询仍返回规范叶子，声明 TP 查询返回降维视图，二者没有混回一个域。

## F4-C — [P1] 重复 TP 绑定绕过 RawArray 语义相等，双表示仍可被真实 matcher 拒绝

[matchExtendTargetType 的 Generic 分支](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExtendSubstitution.kt)（L364） 在 L369–370 仍以 `existing == semanticActual` 检查同一 extend TP 的重复绑定。[Cone 相等](../../cfir/cfir-cones/src/org/cangnova/cangjie/cfir/types/ConeClassLikeType.kt)（L137） 继续比较物理 typeArguments 和 rawArrayDimensions；它不能把逐层二维和 flat 二维视为相等。

具体完成输入可用同一名义目标 `Pair<T, T>` 说明，设：

- `N = RawArray(RawArray(Int64, dims=1), dims=1)`：逐层二维。
- `F = RawArray(Int64, dims=2)`：平坦二维。

当前公共 [RawArray 类型检查](../../common/src/org/cangnova/cangjie/type/AbstractTypeChecker.kt)（L559） 对 N 与 F 判断累计维度同为 2、规范叶子同为 Int64；普通名义不变实参比较 L586–595 也使用这条语义等值，因此 Pair<N,N> 与 Pair<N,F> 属于相同的完成类型语义。

但实际 matcher 对 `Pair<T,T>` 和 `Pair<N,F>` 的流程是：

1. Pair 的 declarationTypeArguments 保留两个有序实参。
2. 第一次 T 记录 substitutions[T] = N。
3. 第二次 T 用 N == F 比较；物理实参不同且原层维度 1 与 2 不同，返回 false。
4. createExtendDeclarationSubstitution L251–261 返回 null，实际 provider L419 丢弃该 extend 父边。
5. 相同目标对 Pair<N,N> 或 Pair<F,F> 则不会在此处失败。

这条失败无需 Alias、最终 Generic upperBounds 或严格 capture 的既知余额。它是本轮 RawArray 双表示合同在同一真实 matcher 的另一个出口仍未闭合，不将其称为原 F4-B 的旧“一层 T 绑定成 Int64”问题。

[semanticExtendMatchType](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExtendSubstitution.kt)（L484） 仅调用 [别名展开](../../cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/TypeExpansionUtils.kt)（L87）；无 Alias 的上述数组不会被统一驻留。AbstractConeSubstitutor L205–223 在没有替换时返回 null，withoutAbbreviation 在没有 abbreviation 时返回原对象，因此预处理没有消除这个差异。

固定官方 TypeManager.cpp L113–126 先将嵌套 ArrayTy 统一为规范叶子/累计维度，再用 GetTypeTy<ArrayTy> 驻留；固定 MultiTypeSubstUtils.cpp L302–320 的映射使用这些实际 Ty，TypeManager.cpp L1634–1643 的 extend 实例化核对也使用驻留 Ty。CFIR 既保留两种物理 Cone 表示，就不能在重复绑定点以物理 Cone.equals 替代相同的完成类型身份。本例是源代码调用流推演，没有声称已编译或执行新的 fixture。

**具体改法：**在共享 extend matcher 的重复 TP 绑定处使用已有公共类型系统对完成类型的不变等值判定（保证真实 Generic 身份及既有错误输入合同），或由类型公共 owner 提供同等的规范语义身份；不能只比较 ClassId、只比较叶子、使用单向 subtype，或在 encoder 修正。保持所有维度及叶子约束。增加同一 TP 多次出现时 Pair<N,N>、Pair<F,F>、Pair<N,F>、Pair<F,N> 的实际 provider 一致性回归，以及不同维度/不同叶子的拒绝对照。此修改应落在共同 matcher，而非单个 RawArray fixture 的分支。

## 新测试的实际覆盖与限度

[新增 CfirTypeRelationsTest](../../cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/CfirTypeRelationsTest.kt)（L130） 确实对 nested/flat 两种输入执行函数恒等替换、deep 恒等替换及 deep 改 Boolean 叶子，核对累计维度。它命中 F4-A 的旧路径。当前新增段没有含 TP 叶子的 erase 测试；这仅记录动态验证缺口，**不据此拒绝已修复的 erase 源码路径**。

[新增 type-aware provider 回归](../../cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/providers/CfirTypeAwareSupertypeProviderTest.kt)（L79） 从 CfirBuiltinSymbolProvider 取实际 RawArray BuiltInDecl，并注册正常 CfirTypeAwareSupertypeProviderImpl；目标 RawArray<T>、接口 ElementView<T> 对 nested/flat 二维 receiver 都断言 ElementView<一维 RawArray<Int64>> 及同一 sourceExtend。两个 receiver 在测试中直接构造，flat 形状代表 loader 产品，并非该测试实际执行 CJO 装载。该测试覆盖 F4-B 的旧单次 T 绑定；未覆盖 F4-C 的重复 T，也未直接断言 predicateVisible、类型系统 subtype 或受约束 extend 过滤。

没有执行这些测试，不把源码中新增的断言计为通过。原 dims/不变叶子分派与物理复制合同在本次复读中未被改回旧行为。

## 保持 OPEN

Generic final upperBounds、Alias use slot、strict capture、S5/S6、原 SOURCE14、完整原70/P0–P10、整个 S4 及其余原余额继续 **OPEN**。本报告不重审或扩大上一报告 F1/F2/F3/F5、R2/R6 的签发范围；184/184 不签发完整 S4。

## 字节绑定

路径相对唯一工作树；SHA-256 为实际文件字节。上一报告也纳入摘要，未被修改。

| 文件 | SHA-256 |
|---|---|
| `cfir/cfir-cones/src/org/cangnova/cangjie/cfir/types/ConeClassLikeType.kt` | `9CCC7F5CA06B52B2F21623763F97FADCD19D4CC48A3994770AE894FF89DD92F8` |
| `cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/TypeUtils.kt` | `E5E5DB99CF3D43EFD753152FB00603C4588E49DD90659503807E120899A3473D` |
| `cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/ConeInferenceContext.kt` | `5689F54C1C78055116E1F322197FE9EA0671D1C6AA2A28B77665CDFE3C018975` |
| `cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExtendSubstitution.kt` | `80FFF26C10B05D34FC6D0AD345F3351F10C46F172A73AF6A8BA7B5D201AD7AC5` |
| `cfir/semantics.api/src/org/cangnova/cangjie/cfir/scopes/impl/CfirClassSubstitutionScope.kt` | `F3F6DF00219C7D7EF93BBAAB7C5AADABC34B1CC28D505A174098A59E7FBD9A15` |
| `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt` | `8F10F5A871A5C6ABF04D900CF272C67C4D3C55132ADF0A1B4E629EAA8C8D57EE` |
| `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/services/CfirTypeAwareSupertypeProviderImpl.kt` | `3DCCEB0B3C4F6A1494D938C63726300ECCA3F6D8529EAB7EE8690632F3245F18` |
| `cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/CfirTypeRelationsTest.kt` | `A7B2D8BE993C5A183AFE05390C6510CC0D27DFB47B5523AF577B34C5B10F3728` |
| `cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/providers/CfirTypeAwareSupertypeProviderTest.kt` | `E33BD8EAC3D8C9CA03B5ED8ECCE6DF32FC41D026A00379E270C33BFD6A8914F9` |
| `common/src/org/cangnova/cangjie/type/model/TypeSystemContext.kt` | `3B2AD200EEFDB410F530A0CDA32889A881172592D9061332212FDB9A5E2EB5AA` |
| `common/src/org/cangnova/cangjie/type/AbstractTypeChecker.kt` | `DB299F0A9CA0761ED954441EC4A8C82822D1C2AA72E3A7481B64134857C52620` |
| `cfir/semantics.api/src/org/cangnova/cangjie/cfir/types/ConeTypeContext.kt` | `61B7AF180C748E42DCB129A01F6786EA89C3D26B847948DD3CDFB792A3446D37` |
| `cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirTypeDeserializer.kt` | `849B1F7CC76C259E7EDF0D3344063B426EF8847FD4282D148F8E54C4D1242016` |
| `cfir/cfir-serialization/test/org/cangnova/cangjie/cfir/serialization/cjo/CjoRawArrayLayoutTest.kt` | `F70BF31345FCF65698F9178A251EF2BD3A54EC0F8A83EDF7DB2FD19C275A8B70` |
| `cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt` | `17AF24068B674B17A7709B6460408B3BFF8BEE7559D96942D6AE6853ADF01307` |
| `docs/cjmp-source-helper-20261117/source-frozen-encoder-gate-final-20261118.md` | `D3E0FC666B1F54053FA42987E3E7B197AD769C80F941394C51E39782B5B01BFC` |
| `cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/TypeExpansionUtils.kt` | `E79D8E3A2B6A9BF0063C3A63C41CF4087BDE64882E8A15022E6297A766ACE556` |
| `cfir/semantics.api/src/org/cangnova/cangjie/cfir/resolve/substitution/AbstractConeSubstitutor.kt` | `A6A0821B5E21F8239F3169A5BA40047C7DF8FF884506B72884766655213E2BDB` |

本次唯一写入为本报告，使用 apply_patch 新增。生产/测试只读；全部结论均为固定对象考证及静态调用流分析。

