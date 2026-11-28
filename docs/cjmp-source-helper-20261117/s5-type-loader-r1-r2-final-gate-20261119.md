# S5 类型 loader：最终 R1/R2 限定 gate

角色：KotlinParityGatekeeper。**ACCEPT：仅本报告指纹对应的 R1/R2 源码与已落盘限定测试断言审查；无未解除的 R1/R2 Findings。**

本次独立读取时间为 2026-11-19 13:31–13:34 +08:00，最终指纹复核为 13:34:16；工作树为 `C:/Users/lin17/.codex/worktrees/cjmp-framework-implementation/cangjie`，HEAD 为 `b4dd742d589e4b5ee6592d454634c44f634df08a`。审查期间 TY、两个 provider 的字节发生变化，已重新读取变化后的实际生产代码，再复核指纹，不沿用旧报告的 REJECT/ACCEPT。

本轮仅新增此报告。未修改生产或测试，未运行 Gradle、cjc 或测试，未联系旧会话。本报告不提供执行通过证据，不关闭完整 Alias、SOURCE expression、完整类型 loader、S5/S6。

## R1 当前源码判定：解除

实际读取的链为：

1. `CfirDeserializedSymbolProvider.loadPackageDeserializers:145–165` 将 loaded input 的真实 moduleData 捕获进 dependencySnapshotResolver。
2. `selectImportedSnapshot:172–188` 读取候选；多候选时从 requestingModule 沿 dependencies/refinementDependencies 遍历真实闭包，以 IdentityHashMap 集合记录模块身份，再按 moduleDataFor(sourcePath) 过滤。只有一个匹配才返回；零个/多个匹配明确失败。
3. `CfirDeserializationContext.importedSnapshot:222–226` 使用该 callback；未提供 callback 的调用方仅接受唯一候选，多候选失败。
4. `CjoFullIdResolver.loadImportedPackageIndex:152–163` 直接消费 selected snapshot，核对包名与 `manager.loadedPackageSnapshot(sourcePath) === loaded`，用其 header/pkg/sourcePath 创建索引；随后 resolve 的 L85–95 才在该索引查 exportId。
5. CTX 的 materializeTypeParameter 传递该 selected sourcePath；`AbstractCfirDeserializedSymbolProvider.deserializeCjoDeclaration:222–231` 以规范路径 singleOrNull 定位所属共享 context，进入实际 canonical owner/TP 路径。

本次 Generic FullId 路由没有先调用 manager 的包名 first 入口；“先错误选 Q1，再 exact-path 确认 Q1”已不属于上述当前字节。没有选择证据或匹配不唯一时拒绝，不通过候选顺序、模块名称、参数名称猜目标。

## R2 当前源码判定：解除

`CfirTypeDeserializer.convertVArrayType:229–234` 要求真实 ArrayTyInfo，直接读取 info.dimsOrSize。缺 info 的旧 `?: 0L` 分支已不存在；缺失字段失败，实际存在的 0 字段保持 0。

本轮再次用固定 native `14276740ffec1262b81b6b5236b1d840af270b37` 的 `git show` 读取 `ReferenceLoader.cpp:360–364`，核对必需 info、唯一 typeArg 与真实尺寸字段；并复读 `ASTLoader.cpp:550–562` 的真实目标包/exportId 节点路由。Kotlin 固定版本仍为 `3c7a5041f238102285764d370665f56f587f9dc4`，框架约束取已读的本轮 mapper/native authority，不将 CJO 多 snapshot 选择替换成 Kotlin 重复库 first-match。

## 已落盘 R1/R2 测试的实际覆盖

文件：`cfir/cfir-serialization/test/org/cangnova/cangjie/cfir/serialization/cjo/CjoTypeGraphDeserializationTest.kt`。本次读取实际测试与其 metadata/module helper，非仅搜索方法名。

| 当前方法/位置 | 已核验断言与路径 |
| --- | --- |
| `doubleParentFullIdSelectsOnlyTheReachableSnapshotAndRejectsAmbiguity:342–434` | 实际 CjoPackageWriter 写出 Q1/Q2 文件，manager 按 Q1/Q2 顺序加载同包快照；MultipleModuleDataProvider 以精确路径绑定不同模块。Q2 direct、P→bridge→Q2 refinement 两正分支检查 selected===第二份快照、真实路径、imported TP===Q2 canonical symbol/moduleData，另加载 Q1 并检查同名参数 symbol 不同。 |
| 同方法 BOTH/NONE 分支 L414–429 | 对两个/零个可达匹配均要求 provider 选择与实际类型读取失败；完成图 map 身份保持、无新增 imported index/generic reservation/staging，重复失败仍保持旧完成缓存。 |
| `missingVArraySizeInfoRejectsWithoutPollutingCompletedCaches:438–459` | writer 生成缺 info 的 VArray；类型读取要求失败，已有 Int64 及三个 CompletedGraph map 身份保持，未缓存成功 VArray。 |
| `explicitVArraySizeZeroPreservesTheNativeDimensionField:463–475` | writer 明确保存 ArrayTyInfo(0)，检查尺寸与 elementType、重复读取身份；区分缺 info 与真实零字段。 |

覆盖边界：双 parent 测试的 consumer context 在 L385–389 直接构造，绑定正式 provider 的 selection/canonical callbacks；parent 的 canonical 节点通过正式 provider context 物化。它不是纯包名/mock symbol 断言，但也不能据此声称已行为验证 provider 自动创建 consumer context 的接线。该自动接线已在本轮源码中直接核验。以上均是测试源码审查，未运行，不引用旧 XML 或旧编译替代当前测试终态。

## 最终本轮指纹

以下六个生产文件与测试指纹在落盘前复核；结论绑定这些实际字节。后续修改需要复读。

| 文件 | SHA-256 |
| --- | --- |
| CfirTypeDeserializer.kt | 5F84759B818F86438103DD7BB1FB0AC69F83D9533382A9567C9C752E685DB189 |
| CfirDeclDeserializer.kt | 7739CAC72CFC1AC83F1752450D92DEADAE63C844D2B0B5053C35BE0F29B42A19 |
| CfirDeserializationContext.kt | CD8FD3D3D8789DEC52B60B137B46437C5E985736D008163A090221B4D131665F |
| CjoFullIdResolver.kt | FC7E22462A4F76A0CCCFE4700A210B3FA9F4C41817728381002AC927FB1E160E |
| AbstractCfirDeserializedSymbolProvider.kt | 1CC57197C4956182C5526CED1FA23DDED8CD11A4D742CEEDFEFBAE61FC10E2E5 |
| CfirDeserializedSymbolProvider.kt | DBFA67B916608A5BC26D1CCBBDD2C9400BC656CE7A6917928BE63685808AEDBC |
| CjoTypeGraphDeserializationTest.kt | C33E7531D8D02B29603029125EFC468C240C643E0C2D5A2A77CCEB29C54983D4 |
