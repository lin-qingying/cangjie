# SOURCE 声明顺序、返回类型来源与 CJO producer 版本考证（2026-11-19）

角色：独立只读 sidecar，继续 KotlinFrameworkMapper 的源码映射工作。唯一写入本报告；未改生产、测试、fixture 或构建文件，未运行 Gradle/cjc，未联系旧代理或停止的会话。

## 1. 范围、固定对象与直接结论

一方源码只读取 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`（下称 W），读取时 HEAD 为 `b4dd742d589e4b5ee6592d454634c44f634df08a`，存在未提交修改。本库行号和哈希对应工作文件，不冒充该 HEAD 的内容。主仓库及 external 只读。

参考对象均以 `git show 提交:路径` 读取：

- native：`D:/code/intellij/cangjie/external/cangjie_compiler`，`14276740ffec1262b81b6b5236b1d840af270b37`。
- Kotlin：`D:/code/intellij/cangjie/external/kotlin`，`3c7a5041f238102285764d370665f56f587f9dc4`。
- 既有官方产物：[frozen-type-roles-native-evidence-20261118.json](frozen-type-roles-native-evidence-20261118.json)，SDK 1.1.3，既有编译 `exitCode=0`。本轮未重跑编译。
- 文档 MCP 本轮读取 `manual_source_zh_cn_define_struct`、`manual_source_zh_cn_class_This_类型_8`；文档用于语言规则，固定源码用于实际构造、顺序和格式职责。

用户给出的最新失败是 Carrier `ClassInfo.body` 期望 `[asThis,asNominal,choose,pair,init]`、实际 `[init,asThis,asNominal,choose,pair]`；本轮未重新执行这项测试。

**顺序结论：**固定 native 的实际方法名为 **`AddDefaultCtor`**，不是 `AddDefaultConstructor`；class/struct 的默认 `init` 由语义准备阶段追加到已有成员列表末尾。W 两个 raw builder 的 class/struct 四个分支均用 `declarations.add(0, ...)`，是当前普通 Carrier 顺序差异的直接来源。修复应在 raw 构造和宏完成后的 nominal 声明产品 owner，SOURCE/metadata/writer 共同消费最终成员顺序；不能在 CJO writer 为 fixture 排序。

**版本结论：**native `Package.version` 是 `CANGJIE_VERSION`（生产编译器构建身份），不是 schema 档位，也不是语言选项或 `CjoVersion`。W 当前将 `schemaProfile.compilerReleaseVersion` 直接写入这个字段，职责混合。`gradle.properties` 的 `cangjieVersion=1.1.1` 目前证明项目发布版本；应建立独立 producer-version 发布入口，才可将它作为该构建的 CJO producer 版本。使用 OFFICIAL_V1_1_3 profile 不构成自称 producer 为 1.1.3 的依据。

## 2. 官方归档的独立核验

本轮在内存中解压已有 `cjoGzipBase64`，校验 SHA-256，并按固定 `schema/ModuleFormat.fbs` 的 FlatBuffers 槽直接读取 Package、Decl 与 ClassInfo。没有写临时二进制、没有调用本库 encoder/serializer、没有借测试断言生成结果。

| 官方归档事实 | 结果 |
| --- | --- |
| 源文件 SHA-256 | `6B911947B0533ED9D7889C1CCAF820527A0349BF68F90B9B6130E41ACF92C996` |
| CJO SHA-256（解压后验证匹配） | `5BA49A5DE228E98195C52878F029DB0B5261978D04EFD090676415E9354A54A9` |
| `Package.version` | `1.1.3` |
| `Package.cjoVersion`（既有解码记录） | `0.1.0` |
| Carrier declaration index | 14 |
| Carrier `ClassInfo.body` 直接解码 | `[16,26,27,31,33]` |
| 这些索引的 identifier | `[asThis,asNominal,choose,pair,init]` |

官方归档的类型角色记录另给出：

| 对象 | Decl index / Func type index | 返回 type index | 返回绑定及标记 |
| --- | --- | --- | --- |
| Carrier self | 14 / nominal 27 | — | Carrier，实参对应 TP declarations `[24,25]`，`isThisTy=false` |
| asThis | 16 / Func 30 | 31 | Carrier，TP `[24,25]`，`isThisTy=true` |
| asNominal | 26 / Func 32 | 27 | Carrier，TP `[24,25]`，`isThisTy=false` |
| choose | 27 / Func 33 | 29 | Carrier::U |
| pair | 31 / Func 34 | 35 | Pair，实参为 Carrier::T（TP declaration 24） |
| implicit init | 33 / Func 32 | 27 | Carrier nominal self；与 asNominal 可共享相同 Func type |

这些编号属于官方归档，不要求 CFIR 输出复用相同的绝对编号。必须保持 body 顺序、真实声明绑定、重复类型槽和 This 标记。Carrier 普通方法及隐式 init 的已记录 `FuncBody.body=0`，不表示签名返回为空。

## 3. 固定 native：默认构造器在哪里插入

### 3.1 真实调用链与生成条件

固定 [TypeChecker.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/TypeChecker.cpp) L2091–2113 的准备顺序是：

```text
CheckPrimaryCtorBeforeMerge
→ PrepareTypeCheck4CJMP（common/specific 合并）
→ interop PrepareTypeCheck
→ AddDefaultFunction
→ 后续符号表阶段
```

固定 [Utils.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/Utils.cpp) L246–260 的 `AddDefaultFunction(Node&)` 对 FILE 调用 `CheckDefaultParamFuncsEntry`；TOOL_ADD 根直接退出。

TypeChecker.cpp L2219–2227 的 `HasCtorForTypeDecl()` 查 `GetMemberDeclPtrs()`：存在 `CONSTRUCTOR && !STATIC` 就算已有实例构造器。class/struct 的 `CheckDefaultParamFunc` L2231–2255 在未 broken 的 body 中，只有该谓词为 false 才调用 `AddDefaultCtor`。**static init 不能替代实例构造器。** enum/interface 的对应访问器 L2317–2331 只处理属性 accessor，不调用默认构造器添加。

Utils.cpp L369–395 的 `AddDefaultCtor(InheritableDecl&)`：

```cpp
if (decl.astKind == ASTKind::STRUCT_DECL) {
    // 非 COMMON 的 struct
    structDecl->body->decls.push_back(CreateDefaultCtor(decl));
} else if (decl.astKind == ASTKind::CLASS_DECL) {
    // 非 COMMON / JAVA_MIRROR 的 class
    classDecl->body->decls.push_back(CreateDefaultCtor(decl));
}
```

以上仅摘取实际 append 动作；完整函数还有 ObjC mirror/synthetic-wrapper 排除。它不把构造器插到头部，也不依构造器 source offset 排序。specific 的资格不能只读函数注释：具体分支检查 COMMON，而 specific 的真实构造器判定是在前面 CJMP 合并后完成的。

Utils.cpp L309–346 的 `CreateDefaultCtor()` 创建无参 FuncDecl `init`，设置真实 outerDecl、PUBLIC、IMPLICIT_ADD、CONSTRUCTOR、COMPILER_ADD、parentClassLike/parentStruct。生成位置先沿声明名字，随后 `SetCtorFuncPosition` 按成员更新。**源码位置不是其列表顺序的 owner**；拿 source offset 排序会把 append 语义再次破坏。

文档 MCP 对 struct 明确说：没有自定义构造器（含 primary）且实例字段都已初始化时才有合法的自动无参构造器。固定上述早期生成分支没有按“所有字段已初始化”预先筛选；生成树节点和后续初始化合法性检查应分阶段理解，不能据文档简述改写这个早期分支，跳过该声明的真实初始化诊断。

### 3.2 class / struct / explicit primary / enum 完整区分

| 情况 | native 正式产品及顺序 |
| --- | --- |
| 普通 class 无任何实例构造器 | 保留已有 body 成员相对次序；默认 init `push_back` 到末尾。 |
| 普通 struct 无任何实例构造器 | 与 class 同样 append；不是 Kotlin 式 constructor-first。 |
| 有普通显式实例 `init`，不论有无参数 | `HasCtorForTypeDecl=true`，不额外合成默认 init；原显式成员次序保持。不能把所有名为 init 的声明一律移动到末尾。 |
| 只有 static init | 不满足实例构造器谓词；在其他条件允许时仍追加默认实例 init；static init 保持自身原顺序。 |
| class/struct 有显式 primary（名称为类型名） | 走 `CheckPrimaryCtorForClassOrStruct → DesugarPrimaryCtor`，派生成员字段按参数顺序 append，再 append 生成的 FuncDecl init；不额外再合成默认实例 init。 |
| primary 没有成员参数，或参数都是普通参数 | 字段段为空，但生成的 init **仍由同一个 append owner**发布；不能只在有字段时调整顺序。 |
| enum | 独立 named constructor/variant 集合；无 class/struct 默认 init。`Decl::GetMemberDeclPtrs` 先返回 constructors，再返回 members，保持各自次序。 |
| interface | 不合成实例构造器。 |
| COMMON / interop mirror / broken / imported 等 | 消费真实阶段与 typed 标志的排除；不从普通 Carrier 的规则推断这些类型都应添加 init。 |

显式 primary 的精确证据：

- TypeChecker.cpp L1606–1637，`CheckPrimaryCtorForClassOrStruct` 排除 imported，识别 PrimaryCtorDecl、报告多 primary/非法泛型/多列表，并避免重复反糖；L1636 调用 `DesugarPrimaryCtor`。
- 固定 [DesugarInTypeCheck.cpp](https://gitcode.com/Cangjie/cangjie_compiler/blob/14276740ffec1262b81b6b5236b1d840af270b37/src/Sema/Desugar/DesugarInTypeCheck.cpp) L647–687：先 `DesugarPrimaryCtorHandleParam`，最后 `DesugarPrimaryCtorSetPrimaryFunc`。
- `DesugarPrimaryCtorHandleParamSetEachParam` L153–209：仅成员变量参数派生字段，排除参数本身 STATIC，L182/184 append 字段到 class/struct body。
- `DesugarPrimaryCtorSetPrimaryFunc` L242–280：创建对应 init、保留 primary 的来源及关键属性，L273/280 append 生成的 init。

原始 primary 语法节点和其生成的 init 是 native 的两个角色；CFIR 当前用一份带 source/provenance 的 CfirPrimaryConstructor 表示对应构造器。修正最终成员顺序应保留该身份和原语法来源，不制造第二个 CFIR 构造器来模拟 native 节点数量。

### 3.3 native writer 消费最终顺序

固定 `src/AST/Node.cpp::Decl::GetMemberDeclPtrs` L620–650：class/struct 按 body.decls 迭代；enum 是 constructors 后接 members。

固定 `src/Modules/ASTSerialization/ASTWriter.cpp::GetBody` L451–507：在该迭代顺序上应用真实 export/linkage 条件；有 specificImplementation 时先发布该位置的 specific，再发布符合条件的原成员，防重复。**没有 constructor-first、constructor-last 或名字排序。** `SaveClassDecl` L1397–1404 和 `SaveStructDecl` L1317–1323 直接使用 GetBody 结果。

因此应区分三种顺序：源码成员顺序、生成/反糖后的正式成员顺序、全局 allDecls 索引分配顺序。ClassInfo.body 是第二种顺序经过 export 选择后的引用向量，不能按第三种编号或名字重新排序。

## 4. 固定 Kotlin raw constructor order 的职责

固定 Kotlin 实际文件为 `PsiRawFirBuilder.kt`，不是旧名字 RawFirBuilder.kt。已 `git show`：

| Kotlin 文件 / 方法 | 固定位置与事实 | 对 CFIR 的映射 |
| --- | --- | --- |
| `compiler/fir/raw-fir/psi2fir/src/org/jetbrains/kotlin/fir/builder/PsiRawFirBuilder.kt::KtClassOrObject.extractSuperTypeListEntriesTo` | 方法 L1160 起；L1247–1267 根据 primary/secondary/expect/interface 建 primary，并先 `container.declarations += firPrimaryConstructor`。 | 构造器身份、是否生成与初始顺序属于 raw construction owner。 |
| 同文件 `Visitor` 内的 class 构建及 `FirClassBuilder.addDeclarations` | L1942 调上述 helper；L1952–1960 加 primary 参数属性；L1964–1970 才加 class body；L2037–2067 按 body 顺序转换成员。 | PSI raw declaration construction 拥有顺序；serialization 不倒过来定义 raw 成员次序。 |
| `compiler/fir/raw-fir/light-tree2fir/src/org/jetbrains/kotlin/fir/lightTree/converter/TreeRawFirDeclarationBuilderProxy.kt::convertClass` | L714–725 primary，L726 delegates，L729–744 primary properties，L746–749 body members。 | LightTree 必须和 PSI 使用相同的语言 construction 产品。 |
| 同文件 `convertPrimaryConstructor` | L1182–1201：implicit/secondary/interface/expect/enum entry 有自己的规则。 | 规则属于 Kotlin 语义，不能直接套在仓颉 class/struct/enum。 |

Kotlin 把 primary 放到成员列表前面，是 Kotlin raw 构建的实际结果；它只证明**顺序归属在前端构造阶段**，不能证明仓颉 native 也应 constructor-first。仓颉默认构造器 append 与 primary 反糖 append 是有固定 native 证据的语义差异。尤其 Kotlin enum/enum-entry 构造器、primary 参数属性、委托字段、expect 规则不等于仓颉 struct/enum/CJMP 合同。

## 5. 本库 raw、construction、canonical 与 metadata 的统一顺序

### 5.1 现有节点和直接缺口

以下路径均相对 W，位置为读取快照：

| 层 / 完整仓库内路径 | 方法和位置 | 当前行为 |
| --- | --- | --- |
| `cfir/raw-cfir/raw-cfir-common/src/org/cangnova/cangjie/cfir/builder/AbstractRawCfirBuilder.kt` | `shouldSynthesizeImplicitPrimaryConstructor` L268–271 | 共用 presence 策略，排除 specific、检查已有非 static CfirConstructor；没有顺序发布职责。此签名也不独立表达 native COMMON/mirror 资格。 |
| `cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt` | `convertClass` L933 起，class L971、struct L1033 | 都 `add(0, buildImplicitPrimaryConstructor(...))`，把 raw implicit init 放在头部。 |
| `cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt` | `convertClass` L485 起，class L526、struct L586 | 与 PSI 同样 `add(0, ...)`。 |
| 同两 builder | PSI `buildImplicitPrimaryConstructor` L1573–1590；LightTree L640–657 | 隐式默认构造器沿 K2 用 CfirPrimaryConstructor 表示，source.kind=ImplicitConstructor、body=null。isPrimary 本身不证明 native 显式 primary。 |
| 同两 builder | PSI `convertClassMembers` L3923 起；LightTree `extractClassMembers` 末段 L2347–2361 | 明确声明按语法遍历追加；显式 primary 的 CFIR 节点先在其语法位置产生。enum 两分支均不再合成默认 primary。 |
| `cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/ImplicitConstructorDeclarations.kt` | `finalizeImplicitConstructorDeclarations` L21–38 | 宏完成后若有显式实例构造器，移除早期 raw implicit；若应保留 implicit，则 L32 提前 continue，**没有把其放到宏展开后成员尾部**。 |
| 同包 `PrimaryConstructorParameterFields.kt` | `materializeFields` L39–105 | 从真实显式 primary 派生字段，L102 按参数顺序 append 字段；未将表示 native 生成 init 的显式 primary 放到这些字段之后。members为空时 L50 提前 return。 |
| 同文件 | `PrimaryConstructorFields` 产品、`checkMaterializedFields` L128–137 | L104 保存 owner.declarations 完整身份顺序；后续检查按 zip 校验。不能在这个产品封闭后再偷偷移动 primary。 |
| 同包 `MacroConstructionApi.kt` | construction 完成入口 L1084–1088 | 依次调用 implicit finalizer 与 primary field materializer，位于 successOf/provider 注册前，是已有正式发布门。 |

这解释了普通 Carrier 的 raw 次序，同时揭示宏替换和显式 primary 不能只用一次 raw `add` 修改覆盖全部情况。

### 5.2 canonical 与 metadata 实际消费的共同 owner

`cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/source/CfirSourceGraphProducer.kt::produce / indexDeclaration` L70–88、116–145，从正常文件 owned declarations 及 family 的最终根/成员域登记定义，再建立表达式 SOURCE 图。本库 SOURCE graph 是表达式/正文 canonical 产品，**不是一份独立的 ClassInfo.body 排序表**。

`cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt::Capture.captureDeclaration` L564–570，用 `freeze(members(owner).flatMap(::projectPattern))` 保存 memberBodies。L288–290 以身份和顺序核验输入未变化。`CfirExportAnalysis.kt::memberDeclarationsOf` L178、814 直接返回这份捕获产品。

`cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt`：

- `exportMemberInput` L1786–1799：普通输入从 `owner.declarations` 取成员；CJMP 输入从最终 family 导出成员产品取，保留真实投影/来源。
- `declaredMembers` L1802 读取 `exportAnalysis.memberDeclarationsOf`。
- `bodyMembers` L1805–1806 只应用 exportSelection，保持列表次序。
- ClassInfo/StructInfo 构造 L2070–2086 对 bodyMembers `map(::ref)`。

**统一的顺序真相是 construction/merge 完成的 nominal 成员列表（family 时为对应最终成员产品），其后冻结到 providers.memberBodies。** canonical 与 metadata 共同消费该声明产品。不得在 CjoCanonicalSourceEncoder、CjoPackageWriter 或 ClassInfo 构造处另建 fixture 排序逻辑。

### 5.3 主线程应改的完整生产路径

本节为实施建议，不声称已经修改或已有下述新 helper。

1. **raw class/struct 四个 insertion site：**将默认 constructor 的初始发布规则改为 native 的 append；复用已有构造节点、符号和来源，不只修 PSI。普通显式 secondary/static init 的源码顺序不变，enum/interface 不添加默认 init。
2. **宏完成后的保留/删除与最终顺序：**在现有 `ImplicitConstructorDeclarations.kt` construction owner 内统一确定 retained implicit 的位置。显式/宏实例构造器出现时移除早期 implicit；没有出现时，把同一 retained implicit 节点放在最终 expanded 成员之后。不能因没有显式构造器就提前退出顺序发布。typed COMMON/specific/interop 资格按 native 阶段处理，不按名字推断。
3. **显式 primary 的反糖顺序：**`PrimaryConstructorParameterFields.kt` 完成字段 append 后，将对应实际 init 的 primary CFIR 节点按 native 规则发布在字段段之后。零成员参数也必须走同一顺序规则，不能被 L50 的早退跳过。原 source/provenance、parameter/field 双向绑定、annotations 和符号身份继续保存。
4. **唯一 construction ordering helper：**建议在上述 construction package 的 `ImplicitConstructorDeclarations.kt` 发布 internal 的 nominal-constructor-order helper，由 implicit finalizer 和 primary materializer 消费各自已确定的角色。这个 helper 只负责**生成构造器的 append 关系**，保留其余成员次序；不是按名字、offset、isPrimary 总排序。它的声明产品仍是 owner.declarations，没有新 serialization order 通道。名称属于实施建议，不是 Kotlin 同名 API。
5. **快照边界：**对显式 primary，顺序最终化必须在 PrimaryConstructorFields L103–105 保存 declarations **之前**；检查 previous 产品的 replay 只验证既有顺序，不再重排。不要在 MacroConstructionApi 两个现有 helper 之后追加一个会破坏已封闭 field 产品的清理步骤。可将字段生成末尾的共同顺序调用放在现有快照构造前；无字段的 primary 路径仍执行它。
6. **发布与冻结：**construction success/provider 注册、CJMP structure/family 生成、SOURCE graph 生成及 export input capture 必须看到同一最终次序。family 已保存 `declarationOrder` 的场景，在结构/merge owner 发布前落实顺序，不能在已固定 family/export 索引后移动声明。
7. **metadata/writer：**保留现有 bodyMembers/filter/map 和 append-only d/t/e 编号职责。Carrier 修复后应自然得到正确顺序；source graph 的表达式编号不要求因为 constructor 调整而保持旧绝对值。`ClassInfo.body` 与 allDecls 分别检查，不把 sorted declaration IDs 当作 body。

生产完整性检查应覆盖普通 class/struct、显式 secondary、static-only、显式 primary 有/无成员参数、宏新增/替换成员与构造器、enum、CJMP common/specific 资格。这里没有运行这些验证，也不为这些情况新增 fixture。

## 6. This / asNominal / CFIR signature 的返回源节点

### 6.1 签名返回与正文返回不能混为同一来源

`orderedAliasRoles.cj` L11 显式写 `asThis(): This`，L15 显式写 `asNominal(): Carrier<T,U>`；两段正文都是 `return this`。**返回声明语法决定显式签名的源节点角色，不能因正文相同把二者归并。**

固定 native：

- `src/Sema/PreCheck.cpp::ReplaceThisTypeInFunc` L1731–1747：把合法 class 实例函数 retType 的 ThisType 替换为 RefType，绑定当前 class，标识为 This，并用当前 class 的真实 typeArgs 建 ClassThisTy；保留原 This 的位置。
- `src/Modules/ASTSerialization/ASTWriter.cpp::SaveNominalTy` L995–1012：真实声明绑定、typeArgs 和 `Is<ClassThisTy>` 分别写入；This 与 nominal self 不能因 classId/TP 相同而合并标记。
- `src/Sema/TypeManager.cpp::ObtainsAliasTypeOfFuncDecl` L2395–2408：参数按序取真实参数节点；存在 FuncBody.retType 时取该原 Type 节点，不存在时才使用已解析 FuncTy.retTy 作为**签名返回**。这是 native 定义的两个输入情况。
- `ASTWriter.cpp::SaveFuncBody` L1258–1269：FuncBody.retType 只在真实 retType 节点存在时写入；没有节点则 INVALID。与 Decl.type 的 FuncTyInfo.retType 有不同合同，不能在后者存在时补造前者。

MCP 的 This 规则明确只用于实例成员函数返回，且子类 receiver 调用的结果跟随子类类型。这是语义上下文，不是 serializer 看到短名 This 后重绑声明的规则。

### 6.2 当前 CFIR 来源链的准确位置

| W 文件 / 方法 | 已存在的事实 |
| --- | --- |
| `cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiConversionUtils.kt::toCfirOrImplicitTypeRef / CjTypeElement.toCfirTypeRef` L71–101 | 没有声明 Type 时生成 implicit；真实 CjThisType L95 分派到 `toCfirThisTypeRef` L165 起；实际 Type 引用统一标为 SOURCE。 |
| `cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeTypeConverter.kt::convertThisType` L172–183 | This 保留为 CfirUserTypeRef 的 This qualifier；同文件统一赋 SOURCE origin，不在 raw 推断绑定。 |
| `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirTypeResolveTransformer.kt` L369–405、422–427 | 单独解析 function.returnTypeRef；`thisTypeContextForFunctionReturn` 为 CfirClass 建 classThisType，按当前 class TP 的真实 lookupTag 顺序取 `[T,U]`，禁止 static This。 |
| `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/CfirTypeResolver.kt::resolveThisType` L370–389 | 消费上述 typed context，拒绝 This 的 type arguments/非法位置；不用 mapper 或 writer 重建 This。 |
| `cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportAnalysisInputSnapshot.kt::headerTypeRefOf` L335–339、`declarationType` L811–817 | headerTypeRef 对 callable 是原 returnTypeRef；声明函数类型以完整 parameter refs 与完成的 returnTypeRef 构造 ConeFunctionType，C ABI/variadic 位独立保存。 |
| 同文件捕获 Declaration L530–557 | 完成语义 type 与 headerTypeRef 分别保存；来源节点身份没有被函数类型返回槽代替。 |
| `cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportSourcePayload.kt::captureExportSourcePayload` L237–244 | ResolvedType 捕获 typeUseOrigin 和 delegatedTypeRef prototype；语法包装不会凭空变成新的原 Type 节点。 |
| `cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/CfirExportTypeUseGraph.kt` L73–75、97–99、131–158、167–184 | functionReturns 只用 actualTypeNode；This nominal 标记 L136 直接消费完成语义类型，避免因其语法没有显式 TP 而丢 `[T,U]`；asNominal 使用真实 qualifier arguments；Decl.type 的签名返回按原 header 节点或已完成语义槽选择。 |
| `cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt` L1989–2023、3256–3275 | Decl.type 消费 declarationTypeUse；FuncBody.returnType 消费 functionReturnUse，无实际原节点时为 0；nominal、Func 参数向量和独立 returnType 分别编码。 |

`cfir/cfir-tree/src/org/cangnova/cangjie/cfir/types/CfirTypeRefOrigin.kt` 明确 SOURCE、SEMANTIC、COMPILER_ADDED 的不同来源合同。不能仅据 resolved/ref.source/delegated 存在推测原 Type 是否存在。

本轮已核对到 SOURCE 类型选择链的实现与官方 This/asNominal 记录；用户的新失败尚停在 body 顺序处，**不声称后面的 This/asNominal/签名断言已经在 CFIR 运行通过**。主线程修复排序后，应逐槽检查上述两个返回角色；若有后续类型错误，应沿本节真实 type origin/lookupTag/type-use owner 修复，不在 writer 按函数名修正标记。

## 7. Package.version 的真实来源与本库错误耦合

### 7.1 native 三个版本维度是独立产品

固定 `schema/ModuleFormat.fbs` 原文：

```text
Package.version:string      // cjc version
Package.cjoVersion:CjoVersion // cjo format version
```

`src/Basic/Version.cpp` L18：`CANGJIE_VERSION = CJ_SDK_VERSION`。固定 CMakeLists.txt L10–11 未指定构建参数时 CJ_SDK_VERSION 的源码默认值为 `0.0.1`；L201–202 发布编译宏。实际 SDK 构建用什么值，不能单凭 Git commit 或 schema 类型判定。

`ASTWriter.cpp::AST2FB` L724–726 将 CANGJIE_VERSION 建为字符串，L758–761 另外构造 `CjoVersion(CJO_MAJOR_VERSION, CJO_MINOR_VERSION, CJO_PATCH_VERSION)` 并传给 CreatePackage。它没有把 profile 名称或语言版本转换成 producer string。

固定 CMakeLists.txt L310–311 的 CANGJIE_VERSION CMake 参数用作 VERSION_TAIL，Basic/Version.cpp 对**展示用** CANGJIE_COMPILER_VERSION 拼接尾部；这不能与全局 CANGJIE_VERSION/CJ_SDK_VERSION 的 producer 身份混淆。官方归档实际 producer=1.1.3 是 SDK 1.1.3 的产物事实，不是固定源树所有编译结果都必须写 1.1.3。

### 7.2 本库现状

| 文件 / 位置 | 当前事实 |
| --- | --- |
| W `gradle.properties` L15 | `cangjieVersion=1.1.1`。 |
| W `build.gradle.kts` L58–62 | 读取该属性，赋值给 `allprojects.version`；这是项目发布版本的直接证据。 |
| `CfirCjoPackageMetadataProducer.kt` L255–265 | common-part 选择 OFFICIAL_CJMP，language>=1.1.0 选择 OFFICIAL_V1_1_3，否则 OFFICIAL_V1_0_0；随后 `version = schemaProfile.compilerReleaseVersion`。 |
| `CjoPackageWriter.kt::CjoSchemaProfile` L1139–1151 | OFFICIAL_V1_0_0=1.0.0，OFFICIAL_V1_1_3=1.1.3，OFFICIAL_CJMP=1.1.3，REPOSITORY_EXTENDED=null。 |
| 同文件 `CjoPackageMetadata` L1071–1080 | 当前 version 注释写成“旧格式版本字符串”；cjoVersion 为独立结构，默认0.1.0。version 注释与 native 的 cjc producer 字段含义不符。 |
| 同文件 write L111、173–189 | writer 原样序列化 metadata.version，另外序列化 metadata.cjoVersion。错误来源在 metadata producer，而不是 FlatBuffer addVersion。 |
| `CfirSourceFrozenTypesTest.kt` L248 | 当前断言 `pkg.version == "1.1.3"`；这实际随 profile 常量通过，不能用该断言反证本库 producer 真的是1.1.3。 |

本轮在 compiler/config、entrypoint、common 的检索未找到已发布的 compilerProducerVersion/编译器构建版本 accessor。`compiler/arguments/.../CangJieReleaseVersion.kt` 是支持的 release/参数 schema 枚举，列有1.0.0/1.0.5/1.1.0/1.1.3；它不是这个编译器构建的 producer 身份，不能选“最新支持版本”代替1.1.1。

### 7.3 可实施的独立 producer-version 链

应发布三份独立值：

| 值 | 正确 owner / 含义 | 此构建的例子 |
| --- | --- | --- |
| compiler producer release | 编译器构建元数据，经 frontend/session 或 export invocation 明确传入 metadata producer | 如果该构建使用当前 cangjieVersion 发布且该属性被确立为 producer release，则 `1.1.1`。 |
| CJO binary format version | 格式常量/format policy | `0.1.0`，不能改成1.1.1。 |
| schema profile / language settings | wire 能力选择 / 源语言兼容选择 | 可以选 OFFICIAL_V1_1_3 或 OFFICIAL_CJMP，不改变 producer release。 |

固定 Kotlin 的生产构建版本参考已 `git show`：

- `compiler/compiler.version/src/org/jetbrains/kotlin/config/KotlinCompilerVersion.java` L14–16、39–56，从 `/META-INF/compiler.version` 读取 VERSION；不是从语言设置推导。
- `compiler/compiler.version/build.gradle.kts` L19–24，通过 processResources 将真实 build version 替换进该资源。

若主线程采用严格的 Kotlin 模块对应，独立 owner 的目标为 `compiler/compiler.version` 中的本地化 `CangjieCompilerVersion` 和相应 build-version 资源；**当前本库没有这项 owner，本报告不声称已存在或已实现**。其必要合同是不可变的实际 producer release，未配置时拒绝正式产物写出；不照搬未知 snapshot 的兼容行为、不读 runtime Gradle 文件、不以schema/latest language 兜底。

然后通过真实 frontend/session/export invocation 的公开输入传递该值。已有 `cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoDeclarationExportSelection.kt::CjoExportOptions` L30–52 是实际 invocation 产品，可加入明确的 compilerProducerVersion 输入（此名称为建议）；它的 source-files 工厂和 CLI/common/specific 等调用者必须全部接通同一构建身份。metadata producer L264 改为消费该输入，writer 继续原样编码。

schemaProfile 可保留其验证过的字段/布局能力信息，但 `compilerReleaseVersion` 不再作为 Package.version 来源，最好移除这种误导职责或明确改为 schema 取证版本信息。REPOSITORY_EXTENDED 也不应因此省略真实 producer 身份。

对于官方 SDK 基线，可保留其归档 version=1.1.3；本库实际 producer=1.1.1 的产物不能为了逐字节比较伪写1.1.3。后续测试应分别验收版本来源与类型/声明/wire 能力，不让fixture指定的SDK版本覆盖生产构建身份。本轮不修改测试断言或基线。

## 8. 主线程后续验收边界

Carrier 应在正常 resolved CFIR/final construction 列表、exportAnalysis.memberDeclarationsOf 与 CJO ClassInfo.body 上同时出现 `[asThis,asNominal,choose,pair,init]`；只是 wire 最后显示正确而 normal/family 产品仍 constructor-first 不算完成。

This 与 asNominal 各自保留 Carrier::T/U 的真实 TP owner：前者 This 标记 true，后者 false。Decl.type 的 Func signature return 与 FuncBody.retType/source-Type 存在性独立检查；默认 init 没有显式 ret Type 不因其签名返回 Carrier 就获得新 SOURCE ret Type。

版本应能观察到 schema/profile、cjoVersion、producer release 三份独立值。固定源码考证、既有官方归档解码和本库当前读取不构成生产修复通过证明。本轮没有执行任何编译或测试。

读取快照 SHA-256：

| W 文件（完整路径见上文） | SHA-256 |
| --- | --- |
| PsiRawCfirBuilder.kt | `2B534C9CC642F14FE86495C0C4ABE6BB57C1C4C115ED0C21FBAFD96EA4F6D2C9` |
| LightTreeRawCfirDeclarationBuilder.kt | `4053F11D17AC9D8A3B7D81DAD35CA9329F1EE4AB5611D8A95171618EDA0394E8` |
| ImplicitConstructorDeclarations.kt | `60E93640E3C42121B3AD7247F0F725B74D8AEA0F36D726D6A2C2A4595CC64A2B` |
| PrimaryConstructorParameterFields.kt | `E236B3C70EBF96137668487786552397765B8831B9004D02D7672A87D7C04C6B` |
| CfirExportTypeUseGraph.kt | `AD47B04C93B6F7310D0E255EC23DC087808BDF19F76DECA15C4A8C9BA1D1408B` |
| CfirCjoPackageMetadataProducer.kt | `3B2CF078AC4A0CE779A7C7B84E7F8902AA552F12AC83B1CC2B40B0198308A58B` |
| CjoPackageWriter.kt | `D0C56CB03E5558DEC44BE4EBB3A34065395FB03AA3B219EB204CCEE43A8951A4` |
