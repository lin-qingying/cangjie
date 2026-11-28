# S5 类型 loader：R1/R2 限定复审

角色：KotlinParityGatekeeper。结论：**ACCEPT（仅 R1/R2 源码解除）**。读取时间：2026-11-19 13:28:21 +08:00。没有本轮未解除的 R1/R2 Findings。

本报告接续 [上一轮限定审查](s5-type-loader-limited-gatekeeper-20261119.md)，复读最新磁盘 CTX/TY/FullId resolver/provider，并检查实际 CfirModuleData、ModuleDataProvider 与 manager 快照身份合同。未修改生产/测试，未运行 Gradle、cjc 或测试，未联系旧会话。仅新增本报告。

## R1：源码解除

实际路径已改为：请求输入 context → importedSnapshot → provider 的 selectImportedSnapshot(requestingModule, package) → 已选 snapshot → FullId exportId lookup → exact-path canonical declaration。

| 文件/方法 | 当前已核验行为 |
| --- | --- |
| `CfirDeserializationContext.kt:55,222` / dependencySnapshotResolver、importedSnapshot | 正式 context 委托真实输入的 callback；未提供 callback 的 context 只接受零/一个候选，多候选显式失败，没有 first 选择。 |
| `CfirDeserializedSymbolProvider.kt:145–165` / loadPackageDeserializers | callback 捕获该 loaded input 的 moduleData，调用 selectImportedSnapshot；使用的是请求输入所属模块。 |
| 同文件 `:172–189` / selectImportedSnapshot | 零/一个包候选按唯一结果处理；多个候选时，从 requestingModule 出发沿实际 dependencies 与 refinementDependencies 遍历，以 IdentityHashMap 集合保存模块身份；用 moduleDataFor(candidate.sourcePath) 过滤，恰一才返回，零个或多个匹配均失败。没有按模块名或候选顺序选第一个。 |
| `CjoFullIdResolver.kt:152–163` / loadImportedPackageIndex | 直接消费 context.importedSnapshot；检查包名及 `manager.loadedPackageSnapshot(sourcePath) === loaded`，再创建该快照的 index view。随后 resolve 中 L85–95 才解析 exportId。 |
| `AbstractCfirDeserializedSymbolProvider.kt:222–231` / deserializeCjoDeclaration | 后续 canonical TP 物化使用所选 snapshot 的规范 sourcePath，以 singleOrNull 匹配所属共享 context；不会回到包名 first。 |

`CfirModuleData.dependencies/refinementDependencies` 是实际模块依赖字段；候选的路径归属使用与 context 创建相同的 ModuleDataProvider/moduleDataFor 合同。因此上一轮“callback 前先选 Q1、exact-path 只确认错误路径”的具体路径已移除。多个可达 parent 都符合时选择失败，符合本轮 unique/refuse 要求，不宣称具备额外的歧义消解策略。

manager 中供其他用途的 loadPackageSnapshot.firstOrNull 仍存在，但本轮 R1 的 FullId/Generic 路由不再调用它；无需为解除 R1 扩大修改到其他产品。

## R2：源码解除

`CfirTypeDeserializer.kt:229–234 / convertVArrayType` 现在以 `checkNotNull(semaTy.info(ArrayTyInfo()) as? ArrayTyInfo)` 要求真实 info，再直接读取 info.dimsOrSize。原 `?: 0L` 已删除。

缺失 info 将失败，不会制造正常的 size=0 类型进入 CompletedGraph；真实 info 保存的 0 尺寸仍原样读取。此处与固定 native `ReferenceLoader.cpp:360–364` 的必需 ArrayTyInfo、唯一 typeArg、真实尺寸读取合同一致。

## 验收边界

这是限定源代码审查通过，不是行为或整个类型 loader/S5 的验收。双 parent 的正确选择、零/多个匹配的歧义拒绝测试尚待主线程落盘及运行；本报告没有引用新增测试通过的证据。完整 Alias classifier/展开、SOURCE expression、完整 S5/S6 延续既有 OPEN 范围。

固定依据沿用上一报告已实际读取的 Kotlin `3c7a5041f238102285764d370665f56f587f9dc4`、native `14276740ffec1262b81b6b5236b1d840af270b37`，以及指定 Kotlin mapper/native authority 两份映射材料。

## 本轮读取指纹

| 文件 | SHA-256 |
| --- | --- |
| CfirTypeDeserializer.kt | 279AC46D7DF1C58C7CBC04E91A6DAF4A49F3F9BAF72AE8165184727FED5BD39A |
| CfirDeserializationContext.kt | CD8FD3D3D8789DEC52B60B137B46437C5E985736D008163A090221B4D131665F |
| CjoFullIdResolver.kt | FC7E22462A4F76A0CCCFE4700A210B3FA9F4C41817728381002AC927FB1E160E |
| CfirDeserializedSymbolProvider.kt | D8A19E30E08810BC5EBA67D5EB46D3B6BE58EE2C76C08198F72639AF0E05241F |
| AbstractCfirDeserializedSymbolProvider.kt | ABD8B79129AFEE1FAB9E02F311EBF1DE11D663DC0F25C5758DD40A3EF1F5B361 |
