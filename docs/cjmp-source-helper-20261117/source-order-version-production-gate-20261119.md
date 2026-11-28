# SOURCE ordering / producer version / C ellipsis 生产复审 gate（2026-11-19）

范围：仅复审本轮新接线及其直接消费者；不扩展为全仓重构或完整 Kotlin parity 验收。唯一写入本报告，未修改生产、测试、fixture、Gradle 文件，未运行 Gradle/cjc，未联系旧代理或停止的会话。

## 1. Gate 结论与证据身份

**整体 gate 尚未关闭。** 已确认 raw default append、宏 retained implicit append、显式 primary 在字段快照前 append、CJO/non-Source primary 排除、独立 build-version 资源链和 C extras 主链已实际接入。仍有以下直接缺口：

| ID | 级别 | 剩余问题 |
| --- | --- | --- |
| V-01 | P1 | 手工打包的 frontend / IDE CFIR fat jar 没有包含新 compiler.version 模块及资源。 |
| O-01 | P1 | 宏删除最后一个显式实例 constructor 后，没有已有 raw implicit 可以保留，finalizer 不生成 native 应追加的默认 constructor。 |
| C-01 | P1 | C-call Unit 禁令只检查初次 argument checking 的实际类型，延迟推断后成为 Unit 的实参没有完成后检查。 |
| O-02 | P2 | 相关的既存 native payload 缺口：Source implicit default 仍以 CFIR isPrimary 无条件写 native PRIMARY_CONSTRUCTOR 位。此次 ordering 改动没有消除该差异；不是新产生的排序回归。 |

这些结论来自源码数据流；本轮没有编译或运行复现。P1 表示已存在可定位的生产路径/缺失消费者，不表示当前 main 的 compile 已失败。

一方读取根 W 为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`，HEAD 为 `b4dd742d589e4b5ee6592d454634c44f634df08a`，工作文件有未提交修改。下文完整仓库内路径相对 W，行号属于本轮读取文件。

依据为前两份报告：

- [SOURCE 声明顺序与版本 authority](source-declaration-order-version-authority-20261119.md)。
- [C ellipsis Kotlin 框架映射](c-ellipsis-kotlin-framework-map-20261119.md)。

本轮另外以 `git -C <对应仓库> show 提交:路径` 复核了固定 native `14276740ffec1262b81b6b5236b1d840af270b37` 的 Utils.cpp、TypeChecker.cpp、TypeCheckCall.cpp、DesugarInTypeCheck.cpp/AttributePack 的相关身份位，以及固定 Kotlin `3c7a5041f238102285764d370665f56f587f9dc4` 的 compiler.version build 脚本和 FirResolvedArgumentList。未以 external 工作文件代替固定对象。

## 2. Ordering 新生产已通过的源码检查

### 2.1 四个 raw default append

`cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt` L971、1033，以及 `cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt` L526、586，均已使用 `declarations.add(buildImplicitPrimaryConstructor(...))`。

这修正了普通 class/struct 的 constructor-first 差异；仍复用原构造节点、symbol、ImplicitConstructor 来源。没有看到为 Carrier 名称或 fixture 做排序。此项通过。它只改变默认 constructor 的初始列表位置，不单独证明宏完成后的 presence 已正确，见 O-01。

### 2.2 宏实际生命周期和 retained implicit

`cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/ImplicitConstructorDeclarations.kt`：

- L30–38 先识别最终显式非 static constructor，决定移除早期 implicit，或把 retained implicit 交给共同 helper。
- `appendPreparedConstructor` L47–52 按 `===` 查找唯一拥有位置，移除该位置并追加**同一对象**；其余成员相对次序保持。
- finalizer 运行于 annotation metadata 的 OPEN transaction 内，没有在已 seal 图中排序。

实际 macro service 也已接线，不能只以 identity service 推断：

| 调用点 | 正确生命周期 |
| --- | --- |
| `compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/MacroExpandPhase.kt` L379–398 | expand/splice/degraded placeholder 后，L392 finalizer、L393 primary field materializer；随后 L398 计算 cache key。 |
| 同文件 L404–418 | 再决定 success/degraded/failed；异常路径 abortAnnotationMetadata，不把未封闭产品注册为成功结果。 |
| `cfir/providers/.../macro/MacroConstructionApi.kt` L1084–1088 | identity service 在其实际成功/可交付路径上调用同一对 helpers。 |
| 同文件 `successOf / degradedOf` L975–1000、`sealAnnotationMetadata` L95–101 | helper 已完成后校验 primary field 产品，再准备 default helper，最后 seal。 |
| `CfirAnnotationMetadataBuilder.kt::withOpenTransaction` L43–46 | requireOpen 并在同一锁内执行；seal L222 起也检查 OPEN。 |

retained implicit 的位置调整和新 helper 的调用顺序通过；finalizer 对“没有 retained implicit”的情况仍不完整，见 O-01。

### 2.3 primary 字段、零成员参数与快照

`cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/PrimaryConstructorParameterFields.kt`：

- L50–54 的零成员参数路径先 append 同一 primary，再保存空字段产品和最终 declarations 快照，不再直接 return 跳过顺序。
- L107–111 的有字段路径按参数次序追加 fields，再 append 同一 primary，之后才构造 PrimaryConstructorFields 快照。
- 前次产品存在时只调用 checkMaterializedFields 校验参数/TP/成员身份及顺序，不重新生成或排序。
- 参数和字段双向绑定、派生 annotation metadata 继续在快照前完成。

此项通过。未发现“先封闭 primary field 快照，后在 construction 尾部排序”的新路径。当前 default-parameter helper 准备在参数 metadata 中挂接 helper，读取代码未见它重排 nominal owner.declarations；不将该阶段另报为快照破坏。

### 2.4 非 Source / CJO primary 不重排

`isDeclaredPrimary` L151–152 现在要求：

```text
isPrimary && origin.fromSource && cjoDeclarationOrigin == null
          && source.kind != ImplicitConstructor
```

因此 Source 显式 primary 才进入字段/顺序派生；Library primary 和携带 cjoOrigin 的 Source-origin 投影节点都被排除。

`cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirDeclDeserializer.kt::convertConstructorIfNeeded` L1734–1789 从 native CONSTRUCTOR/PRIMARY_CONSTRUCTOR 位恢复 constructor，创建 `origin=Library`、`source=null`。这条真实反序列化路径不会被 primary materializer 搬到末尾，也不被 implicit finalizer 的 `source.kind == ImplicitConstructor` 分类为 retained implicit。源码门检查通过。

`appendPreparedConstructor` 本身接受任意 class-like/constructor，implicit finalizer 也没有显式 owner-origin/CJO guard；当前正常 CJO loader 产品的以上属性使它不发生重排。本轮没有建立另一条“带 ImplicitConstructor fake-source 的非 Source/CJO 节点到达该 helper”的真实生产路径，故不把它编造成已发生的 bug。若以后扩大 construction 输入合同，应在 construction owner 明确限定 Source 域，不能由 serializer 补救。

## 3. Ordering 的 remaining

### O-01：宏删除最后一个显式实例 constructor 后，默认 constructor 缺失

这不是假设宏能删除声明：

- `compiler/frontend/.../MacroExpandPhase.kt::replaceDeclarationSlots` L1680–1686 接受空 declaration replacement，实际 `iterator.remove()`。
- 同文件 replacementShapeFailure 的 DECLARATION 分支 L1926–1929 接受零根声明结果；parameter 分支才有“至少一个根”的限制。
- `AbstractRawCfirBuilder.kt::shouldSynthesizeImplicitPrimaryConstructor` L268–271 在原图有非 static constructor 时不生成 raw implicit。

因而有具体的数据链：

```text
原 Source class/struct 有一个显式实例 constructor
→ raw predicate 为 false，没有 early implicit
→ 声明宏返回空声明序列，splice 删除最后一个实例 constructor
→ finalizer: hasDeclared=false，implicitConstructors=[]
→ 当前 forEach 没有任何动作
→ 最终图没有 native AddDefaultCtor 应追加的默认实例 constructor
```

固定 native `TypeChecker.cpp` L2091–2104 在 primary/CJMP/interop 处理后运行 AddDefaultFunction；L2238–2253 只要最终没有实例 constructor 就调用 AddDefaultCtor。`Utils.cpp` L369–395 再应用 COMMON/mirror 等真实资格条件，并 append。它不依赖“parser 曾经生成了一个 implicit”。

正确生产修复位置仍为最终 construction/merge 的默认 constructor 决策 owner：在保留/删除之外，为符合 native 资格、最终无实例 constructor 的 Source nominal 创建正式默认 constructor，并发布真实 symbol、借用 class TP、来源及 annotation/metadata 身份。复用正式 constructor 构建合同，不能复制一个被删除的旧 constructor 充当默认，也不能由 writer 临时添加 init。COMMON/specific/mirror/broken 资格不能省略。

原问题“retained implicit 被放在头部”已修；这个 remaining 是同一最终 presence 合同未覆盖的另一输入形态。

### O-02：CFIR implicit-primary 仍泄漏为 native PRIMARY_CONSTRUCTOR 位

这是与本次 ordering 直接关联、但此前已经存在的 payload 差异，不作为新排序回归。

固定 native `Utils.cpp::CreateDefaultCtor` L329–338 对默认 init 设置 IMPLICIT_ADD、CONSTRUCTOR、COMPILER_ADD，未设置 PRIMARY_CONSTRUCTOR。本轮固定对象检索的实际 `EnableAttr(... PRIMARY_CONSTRUCTOR)` 在 `DesugarInTypeCheck.cpp` L260，属于**显式 PrimaryCtorDecl 反糖出的 init**。

当前 `cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt::attributes` L2894–2896：

```text
如果是 CfirConstructor：写 CONSTRUCTOR
如果 declaration.isPrimary：写 PRIMARY_CONSTRUCTOR
```

Source implicit default 由 raw builder 的 buildPrimaryConstructor 表示，其 CFIR isPrimary=true，因此仍会多写 native PRIMARY_CONSTRUCTOR。primary materializer 的新 origin/CJO guard **没有改变这个独立 attributes 消费者**。

Library/CJO 原位保留已做对：attributes L2846–2847 优先返回原 CJO attributes，不能为了本地 native 标志修复覆盖这条输入。

应在 Source declaration 的 typed native/export 身份 owner 区分默认 init 与真实 primary 反糖身份，再由 attributes 消费；不能对所有 CfirConstructor.isPrimary 一律投射 native primary，也不能按 fixture/名字修位。此次 gate 不改生产。

## 4. 独立 producer version 新链通过，发布闭包未完成

已核对的实际链：

```text
gradle.properties cangjieVersion → allprojects.version
→ compiler/compiler.version ProcessResources 的 @snapshot@ token filter
→ META-INF/cangjie/compiler.version
→ config/CangJieCompilerVersion.VERSION
→ CfirCjoPackageMetadataProducer.version
→ CjoPackageWriter 原样写 Package.version
```

- `compiler/compiler.version/build.gradle.kts` L15–20 使用真实 project.version、登记 task input，并对命名空间资源做 ReplaceTokens；与固定 Kotlin compiler.version 的职责对应。
- `compiler/compiler.version/src/org/cangnova/cangjie/config/CangJieCompilerVersion.java` L14–37 使用该唯一命名空间路径、UTF-8、关闭 stream；资源缺失、空值、未替换 token 都拒绝，不回填 schema/语言版本。
- `settings.gradle.kts` L25 已包含模块；`cfir/cfir-serialization/build.gradle.kts` L12 实际声明 api 依赖。
- metadata producer L265 写真实 VERSION；CjoSchemaProfile 已移除 compilerReleaseVersion 字段。
- 当前新套件代码 L181 锁官方归档1.1.3，L213、248–250 锁 JVM 的真实 build VERSION；这只证明断言来源已经分离，不表示测试已通过。

独立模块及普通 Gradle project runtime 依赖链的源码门通过，未运行资源生成/类加载验证。

### V-01：手工 fat jar 未包含新模块及资源

这是新模块直接引入的发布闭包缺口：

| 实际发布 owner | 证据与后果 |
| --- | --- |
| `prepare/frontend/build.gradle.kts` L19–47、63–67 | frontendProjectPaths 没有 `:compiler:compiler.version`；jar 只 zipTree 这个手工列表，依赖区只列三方库。不会自动合并 serializer 的 api 传递模块。 |
| `prepare/ide-plugin-dependencies/cangjie-frontend-cfir-for-ide/build.gradle.kts` 的 projects 列表 | 包含 serializer，却没有 compiler.version。 |
| `repo/gradle-build-conventions/buildsrc-compat/src/main/kotlin/cangjieIdePublishing.kt::publishCangjieJarsForIde` L28–38、61–72 | 明确直接合并传入项目 jar，不读取这些内部模块的传递依赖/POM；当前 apiDependencies 也没有新内部模块。 |

这些工件若作为独立消费入口，serializer 首次读取 VERSION 时缺少 `CangJieCompilerVersion.class`；即使外部另行提供 class，若缺资源也会被新类明确拒绝。仅 `:cfir:cfir-serialization:compileKotlin` 通过无法证明发布工件正确。

应把新模块及同 jar 内生成资源纳入这些手工发布闭包，并更新相关 sources 工件列表。`prepare/frontend-embeddable` 的 binary ShadowJar 使用 implementation runtime 传递依赖，不能据手工 sources 列表漏项就断言其 binary 同样缺版本；其 sources 列表也未包含新模块，需要对应补齐。没有要求把非传递薄模块工件全部改成 fat jar。

## 5. C extras 的关键接线检查

本节附在同一报告，不另创建第二份 gate。只审本轮 C ellipsis 主链及新增产品，不回审先前文件拆分/包位置的 unrelated parity 差异，也不声称 C variadic 后端 ABI 已实现。

| owner / 当前定位 | 源码门结果 |
| --- | --- |
| `Candidate.kt::hasCVariadicSignature` L747–756；`CfirMapArguments.kt` L160–169、389–417 | 在 Array/exact-arity 路径前识别 C 声明/函数值类型位；fixed 按真实参数绑定，extras 独立 drop(n)，缺 fixed/named 由 outcome 和 sink 错误化。没有 fake CType formal。 |
| `Candidate.kt` L306–317、369–376、491 | 独立 CVariadicArguments，空 E 与无 ellipsis 区分；初始化检查 atom 身份、formal 不吞 E；mapping 更新和前缀替换同步角色。 |
| `CfirCheckArguments.kt` L179–183、136–145 | typed E 在 parameter-null 早退之前取得 CType，使用 candidate/current/显式约束替换；实际调用普通 Processor，并传 isCVariadicExtra。 |
| `ArgumentCheckingProcessor.kt` L167、208–220、932–940 | extra 角色在 ArgumentContext 中随包装/子 atom 递归保留；只对 C extra 排除固定 formal 的 C struct/autobox 比较，类型约束仍执行。 |
| 同文件 L1022–1028；resolution diagnostic 与 renderer | 直接已知 Unit 的 C declaration 参数有实际 INAPPLICABLE 诊断；factory 定义、default message、converter 均已加。延迟类型 remaining 见 C-01。 |
| `CfirCallCompleter.kt` / `CandidateTraversal.kt` L31–33 | normal completion 继续遍历 candidate.arguments；formal-only specialized loop没有伪造 extra formal。 |
| results writer L363、450–478、759–780、873–886、965–968 | 成功 C call 用完整 atoms，E 获取 CType expected map和 replacement 传播，identity cache 转换一次，成功 builder 带独立 E；不进入 Array remap。 |
| `cfir/cfir-tree/src/.../expressions/CfirAbstractArgumentList.kt` L92–100、134–175、225–251 | 公共 arguments 与 formal mapping 分离；构造/转换后校验 fixed n、完整顺序和角色不相交；ordinary 和 C ellipsis成功产品都保留完整 value args。 |
| `CfirOwnedGraphCopyVisitor.kt` L31–56 | 先分配空列表壳，第二遍复制 fixed/context，再使用同一 copyOwned identity 图复制所有 arguments/E并发布完整产品。未发现 E 从 mapping.keys 裁掉。 |
| `CfirAbstractArgumentList.kt::captureMutableState` L177–198 | 一起保存/恢复完整 value arguments、E、Array kind 和两张 mapping 的共享身份。 |
| generated `CfirArgumentList.kt::acceptSourceChildren` L24–25 | 遍历 `arguments`；resolved list L48 的 super调用实际包含 E，不能因后续还遍历 formal keys 就误报“SOURCE visitor 没访问 E”。 |
| 两个 buildResolvedArgumentList入口：`CfirArgumentUtil.kt` 与 expressions/CfirAbstractArgumentList.kt | 均已接受 E和完整 arguments；不是仅内部 impl 增加不可见字段。 |
| `CfirSourceGraphProducer.kt` L388、412–421、appliedSignature | 从完成 arguments 取全实参；只有实际 usesCangjieVariadicCall 才给 VARIADIC，C declaration仍 DECLARED；fixed callee signature 不追加 E。 |

上述是源码接线通过，不是运行/语义全部通过。CType 类型关系复用现有 `CfirCTypeSemantics.constraintType` / type-context；未发现新增 primitive 白名单捷径或把 ABI promotion 写入 Source 类型。

### C-01：推断完成后的 Unit 没有消费 C-call 检查义务

固定 native `TypeCheckCall.cpp::CheckCallCompatible` L1182–1197：先完成 CheckWithCache，再读每项实际 arg.ty；C-call Unit 拒绝先于 foreign-extra 的 C struct 例外。extras 的 expected CType 并不取消 Unit 禁令。

当前 CFIR 对**已知 actual Unit**的新增路径正确；但已经存在以下 normal inference 路径：

1. `ArgumentCheckingProcessor.resolveNestedCallForExpectedType` L448–462 保留成功嵌套候选的尚未 fixed 返回变量，使它合入外层同一系统求解。
2. `argumentExpressionType` L640–660 可以返回这个 fresh T，而不是提前将其替换为声明上界。
3. `checkApplicabilityForArgumentType` L807–818 也保留该当前 inference variable；L1016–1028 添加 `T <: CType` 后，立即读 `argumentType.isUnit`。
4. T 此时不是 Unit，该读数为 false；随后正常 completion 可能把该变量固定为 Unit（Unit 在全局 CType 合同中是允许的原子类型）。
5. 本轮检索的实际 UnitCannotBeCFunctionArgument 报告入口仅在该初次 Processor 分支；CallCompleter/final writer/完成产品检查没有对应的最终 actual-type 消费者。正常 subtype 不会因为 CType允许Unit而自行产生这个调用错误。

这不是要求把 Unit 从 CType 移除，也不是要求破坏 fresh-variable 的正常推断。正确位置是 argument checking/completion 的 C-call 语义义务：为尚未完成 actual type 保留明确的候选/实参/source义务，完成替换后再拒绝实际 Unit，并使候选错误与 results writer 正常错误传播同步。不能在 SOURCE serializer 才抛错，不能重新运行整套解析，也不能仅在 writer按表达式文本检查 `()`。

没有在本轮运行一个 generic Unit fixture；本项结论来自已存在的保留 fresh-variable 路径与新增 Unit检查唯一消费者的组合。函数值入口是否有同样的 native专门 Unit规则继续按 authority区分；不扩大当前 direct C declaration 的规则。

## 6. 后续工作边界

修复优先次序建议：V-01 的发布闭包；O-01 的最终 constructor presence；C-01 的完成后 C-call义务；O-02 的 Source-native constructor身份投射。ordering 和 version 已确认接线的部分无需回退。

main 即将进行的生成/compile可检查新诊断生成、接口签名和资源 task 配置；它不替代宏删除constructor、延迟Unit、grouped copy/restore、SOURCE 角色以及最终发布 jar的检查。本轮按用户要求不执行这些任务，也不修改新套件/生产。

本 gate只给新生产的限定结论，未把整个前端的历史 package/file parity差异纳入本次审查，未给出全项目100% parity批准。

读取快照 SHA-256（用于 main 后续改动后重新定位）：

| 文件 | SHA-256 |
| --- | --- |
| CangJieCompilerVersion.java | `34A7FD26A757C331AB409BCA093130108BCC3FA0E843833E5D0222BD64F7B1B6` |
| compiler/compiler.version/build.gradle.kts | `A5D7E390771DC1D098F9CD61F2215A280B1A4032583DD042339269359CC9BE79` |
| ImplicitConstructorDeclarations.kt | `B26666FD52D1E4A230835D0FB8DCDF1D014771BF14A53C6AE5F7155D862B5BF1` |
| PrimaryConstructorParameterFields.kt | `A0DF2995BF21D5C39ED59FE11B4E3C65F4090239548AED7D31359BB034FC4139` |
| Candidate.kt | `BE0C0B4A5CDBB244FFA5DD9CBECC68007C0D67D80A8D9342F769C78CAF5D2D5D` |
| CfirMapArguments.kt | `246AB449368E7D655D9445D46A4322BCE2B1D8818E5B8FC121167FDD8B898DFE` |
| ArgumentCheckingProcessor.kt | `D841EEC599729FE42C5C313B0DE5E028FDECEBF071CAF6E5EE3C856386721441` |
| CfirCallCompletionResultsWriterTransformer.kt | `E215EC86FF110616ADBBE7006CDEFB91A330BA33F3FB76AAD4E5694A50E403A2` |
| CfirAbstractArgumentList.kt | `CFEB023EEDE690B948252C1D125578A47F822EA65989EA4FFAC48ADC8903F89F` |
