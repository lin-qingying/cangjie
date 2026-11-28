# S5 类型 loader 生产候选限定审查

角色：KotlinParityGatekeeper。结论：**REJECT，限定 R1/R2 两项**。读取时间：2026-11-19 13:23:59 +08:00。审查当前磁盘六个生产文件，未运行 Gradle、cjc 或测试，未修改生产/测试，未联系旧会话。主线程报告第二次编译 exit=0；这不是最终输入 freeze，也不是本审查执行的验证。

本轮范围为实际 TP 两阶段 reserve、own/outer/跨包 symbol、semantic bounds 与原约束组分离、同代缓存发布、Invalid 字段。完整 Alias classifier/展开、SOURCE expression 和完整 S5/S6 留在原任务账，不以这些后续工作扩大本轮拒绝。

## R1 [P1] 跨包 Generic 的 snapshot 在 exact-path callback 之前已被 first 选定

位置：[CjoFullIdResolver.kt:152](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CjoFullIdResolver.kt)，`loadImportedPackageIndex`，特别是 L155–157；[CfirDeserializationContext.kt](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirDeserializationContext.kt)，`materializeTypeParameter`；[AbstractCfirDeserializedSymbolProvider.kt:222](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/provider/AbstractCfirDeserializedSymbolProvider.kt)，`deserializeCjoDeclaration`。

当前 imported `FullId` 只用 `header.imports[importIndex]` 调 `cjoManager.loadPackageSnapshot(packageName)`；该方法内部是 `loadPackageSnapshots(...).firstOrNull()`。得到的 index/exportId/sourcePath 再送入 provider 的 exact-path `singleOrNull`。后者只能确认先前选出的路径，不能纠正其选择。

具体触发：同代 provider 已支持同包 Q 的两个显式 parent 快照 Q1/Q2；它们具有相同的 GenericParam exportId，但属于不同的真实 owner/moduleData。输入依赖关系选择 Q2 时，该路径仍无条件先取得 Q1，最终 `materializeTypeParameter` 返回 Q1 的 canonical TP。即使 bounds 相同也会成功通过比较；实际 symbol 身份仍错误。只在 Q2 存在的 exportId 则可能被错误判不存在。此项是代码路径证明，未声称已运行真实双 parent fixture。

可执行修复：由请求方所属输入的已确定依赖选择结果提供 target snapshot/context，再在该 snapshot 解析 exportId 并取得它拥有的 TP；多候选且没有选择证据时应明确拒绝歧义。不能用 manager first 或全局包名选择冒充 canonical 路由。本项针对跨包 Generic，独立于完整 Alias 展开。

权威：固定 native `ASTLoader.cpp:532–569`，引用取输入包的 importedFullPackageNames 所指目标包 exportId map 中的实际节点；固定 Kotlin mapping 的 binder domain 亦包含真实 owner/input。Kotlin mapper 已明确排除移植 linker 的 first-match 重复库歧义为 CJO 多快照选择策略。

## R2 [P2] 损坏 VArray info 被制造成合法 size=0

位置：[CfirTypeDeserializer.kt:229](../../cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirTypeDeserializer.kt)，`convertVArrayType`，落盘后复读 L232–234。

`semaTy.info(ArrayTyInfo()) as? ArrayTyInfo` 失败后，`info?.dimsOrSize ?: 0L` 返回 `ConeVArrayType(elementType, 0)`。缺少必需 union info 与真实保存 size=0 的输入由此不可区分，并作为正常 type 进入 CompletedGraph。原 zero→Unit 已删除，但此处仍存在同类的必需字段默认值制造。

可执行修复：要求真实 ArrayTyInfo，缺失/错误 kind 形成明确 invalid 产品或失败；有 info 时原样读取尺寸，包括真实 0。沿现有类型错误合同处理，不补尺寸，不改正确 fixture。

权威：固定 native `ReferenceLoader.cpp:361–365` 的 VArray 分支要求 ArrayTyInfo 与唯一 typeArg，再读取真实 `dimsOrSize`。本项属于 type loader 输入 shape，不要求扩大 SOURCE expression/S5 范围。

## 当前已解除的静态拒绝

- `deserializeTypeParameters` 先遍历完整 own 列表建立实际 symbol/owner，再遍历各参数读取 bounds；复用项检查 containingDeclarationSymbol 身份。CURRENT 回边查 context 内 reserved TP；未生成 synthetic TP。
- `GenericParamDecl.type=0` 不被解释成 Unit，也不再补 Any。新增同表 `GenericTyInfo.declPtr(CURRENT)`→TP 索引，在参数发布前消费实际 semantic 类型槽。`Constraint.type` 本身也是实际 t 引用，能够引入真实 GenericTyInfo；没有从原组 RHS 推导 final bounds。
- 原 constraints 进入独立 CompletedGraph 产品，保留 subject、upper 顺序/重复、implicit 与 begin/end；不再拼入 TP.bounds。own 列表为空时也执行原组恢复。
- `convertGenericType` 仅对当前 context 的 reserved TP `replaceBounds`；外包完成参数只比较既有 canonical bounds。此前外包无条件写回的拒绝撤回。当前 context 保留本表 wire 的校验记录，不等于取得目标包的所有权。
- declaration/type 入口进入同一 context 根事务；types/declarations/constraints staging 完成后由单次 volatile CompletedGraph 发布，公开 map 为只读；失败清除本次 staging 和新增 generic 状态。旧逐槽锁已删除。这是静态发布顺序确认，尚无并发/取消行为验收。
- TypeKind.Type 已读取 Alias kind/FullId 与原 arguments；零 type field 和缺 pointer 槽不再补 Unit。完整 Alias 的精确 classifier 绑定、self/RHS/展开生命周期按最新主线程范围保持后续 OPEN，本轮不将它们列为新增阻断。

## type=0 资格考证

固定 native `ASTWriter.cpp:1601–1602` 由 UNREACHABLE 或真实 `SaveType(ObtainsAliasType(decl))` 决定 Decl.type；`SaveGenericParamDecl:1479–1487` 沿用这一字段，没有独立的按 version 缺省分支。`ReferenceLoader.LoadDeclRefs:521–528` 仍分别读取 Decl.type 与 owner 原 constraints；`LoadGenericConstraintsRef:432–450` 的 subject 调 LoadType，真实 GenericTyInfo 经 SetGenericTy 恢复 semantic bounds。原 uppers 是另一个产品。

因此本轮允许**实际同表 GenericTyInfo 的确证入口**，不因 Decl.type=0 一律拒绝历史输入；也不从“历史 profile”猜测默认 final bounds。该固定对象没有证明任意 version/profile 下的零字段都拥有已知空上界。没有任何实际 GenericTyInfo 时不得将 unknown 与已存空上界混为已证实的 wire 产品；具体历史样本资格留给其真实来源证据。

## 固定依据与验证边界

先读 [Kotlin mapper](s5-kotlin-framework-mapper-20261119.md)、[native authority](s5-type-identity-native-authority-20261119.md) 和类型 decoder 映射；关键源码再次通过 `git show SHA:path` 读取：

- Kotlin `3c7a5041f238102285764d370665f56f587f9dc4`：FirTypeDeserializer.init / forChildContext、FirDeserializationContext.childContext、FirCachesFactory.createCacheWithPostCompute。
- native `14276740ffec1262b81b6b5236b1d840af270b37`：ReferenceLoader.SetGenericTy / SetTypeTy / LoadGenericConstraintsRef / LoadDeclRefs；ASTLoader.LoadGeneric / GetDeclFromIndex / nominal 与 Alias 创建阶段；CjoManager 两遍 decls→refs；ASTWriter Generic/type 槽。

跨包 refs fill 仍在持请求方包锁时进入目标包 context。固定 Kotlin 明确提醒跨 key 递归锁风险。本轮没有合法实际输入与交错执行证明死锁，故**不把条件性锁环写作已复现 Finding**；行为窗口应验证正式跨包请求能结束。CompletedGraph 的静态通过也不替代并发、取消、重复查询与旧图保持的真实验证。

## 当前六文件指纹

工作树 HEAD：`b4dd742d589e4b5ee6592d454634c44f634df08a`。以下 SHA-256 属于当前磁盘读数；改动仍在持续，后续审查必须复读，不把本报告当最终 freeze。

| 文件 | SHA-256 |
| --- | --- |
| CfirTypeDeserializer.kt | E1DDAB9F4F0B78F7C5E43764197FC5B22F5D24812E8071FE4E3D1CCF94DCA3CE |
| CfirDeclDeserializer.kt | 4728C743A1CA80090C3AC319B5D12CE3F9825D9F948E537560A77C8C5E135E25 |
| CfirDeserializationContext.kt | 624A9800F4D8935400CA01719A95F1DEB9DCC1A24AC39B74621FD973AAE92649 |
| CjoFullIdResolver.kt | ADDF6275C3E42A7131A482F0869A5806E17B1526E9641A730B7203E5E583297D |
| AbstractCfirDeserializedSymbolProvider.kt | ABD8B79129AFEE1FAB9E02F311EBF1DE11D663DC0F25C5758DD40A3EF1F5B361 |
| CfirDeserializedSymbolProvider.kt | A45F5A2A9A186BF6B37F819A632F59D66CB334BCFEF6B2EE36F28836FB6BCA97 |

落盘后继续复读：TY 更新为 `F993E99FD473E93A2B8A657E78B0F8629DD817F0FAF61620B4B2CA57A02830D0`；CTX 更新为 `4358FF164B96D54EFC469AFE27DF488DE5D4FCF02A9AFD004BA416C7E43D60A7`，新增根事务失败时恢复 initializationDependenciesRestored。R1 first 路由与 R2 size=0 默认分支仍在，结论不变。其他四文件指纹不变。
