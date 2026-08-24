# 修复 deserialized extend 的 import 过滤缺口（CfirExtendAccessibilityChecker fallback）

## 问题

- 症状：deserialized（库）extend（如 std.core 的 `extend Int64 <: Hashable`）在 CFIR 中跨包未导入仍可见；官方要求 `IsExtendAccessible` 四条件（导出/上界导入/接口导入/目标可访问）
- 根因：`CfirExtendIndexStore.rebuild` 只索引 source 文件 → `modelForDeclaration(库 extend) = null` → checker `?: return true` 无条件放行
- 官方证据：`external/cangjie_compiler/src/Modules/ImportManager.cpp:1387-1434`（IsExtendAccessible）、`Node.cpp:1176-1210`（ExtendDecl::IsExportedDecl）、`ImportManager.cpp:1297-1344`（IsTypeAccessible/IsDeclAccessible）

## 改动（1 文件 + 1 测试文件）

### 1. `cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/services/CfirExtendAccessibilityChecker.kt`

- 新增 `ExtendAccessView(packageFqName, targetClassId, inheritedInterfaceClassIds, targetKey)` 内部类：
  - `fromModel(model)`：source 路径字段直取（行为零变化）
  - `fromDeclaration(session, extend)`：库路径声明派生——`session.extendProviderOrNull?.getPackageFqName(extend) ?: return null`（宽松降级）；`extendedTypeRef/superTypeRefs` 的 coneType 派生（`expandedClassIdOrPrimitiveClassId`/`expandedExtendTargetKey`/`isInterfaceShape`）
- `isAccessible` L38 改为：`modelForDeclaration ?: fromDeclaration ?: return true`
- `isExported` 签名改 `view`（3 处字段引用机械替换）
- `isTargetInSamePackageAsExtend` 从 model 扩展函数平移到 view 方法
- 新增私有 `ConeCangJieType.isInterfaceShape()`（与 `CfirExtendIndexStore.isInterfaceTypeShape` 等价的无 resolver 版本）
- KDoc 记录 3 项 fallback 已知差异 + 类 KDoc 说明两条路径语义等价

### 2. `cfir/resolve/test/org/cangnova/cangjie/cfir/resolve/services/CfirExtendAccessibilityCheckerTest.kt`（新增）

注册矩阵（每测试独立构造）：
- `ExtendTestFixtures.newSessionAndModule()` → TestSession + moduleData
- `CfirDefaultImportsProviderHolder.of(DefaultImportsProvider())`（默认导入 std.core.*）
- `CfirExtendIndexStore()`（空 store——不注册则 checker 恒宽松）
- own = `CfirSessionExtendProvider(session, store)`
- stub = 匿名 `CfirExtendProvider`（`getPackageFqName` 按声明返回包名）
- `CfirCompositeExtendProvider([own, stub])` 注册为 `CfirExtendProvider`（own-only 时 fallback 的 getPackageFqName=null）
- 空 `CfirSymbolProvider` 匿名子类（6 个 abstract 成员 + `CfirSymbolNamesProvider` 匿名子类）
- `CfirImportBindingStore()`（显式导入用例 `record(file, [Package(base)])`）

调用链：`CfirAccessibilityFileScope.with(file) { session.extendProvider.isExtendAccessible(extend) }`

用例（8）：
| # | 场景 | 期望 |
|---|---|---|
| 1 | 反例：extend 包 lib，目标 base.Target + 接口 base.I，use-site 包 a 无导入 | false |
| 2 | 正例：同包文件（use-site 包 lib） | true |
| 3 | 正例：同包目标（extend 包 lib 扩展 lib.Target） | true |
| 4 | 正例：默认导入（目标/接口在 std.core） | true |
| 5 | 正例：显式导入（import base.* binding） | true |
| 6 | 正例：primitive std.core 特例（std.core.Int64 <: Hashable） | true |
| 7 | 回归对照：source（store.rebuild 建 model）同场景 3 | true |
| 8 | 反例：泛型上界不可达（T: lib.Bound，newTypeParameter） | false |

## 验证矩阵

1. `./gradlew :cfir:resolve:test` 全绿
2. `./gradlew :cfir:cfir-serialization:test` 6/6（现有，无新增——集成测试模块边界不可行，见下）
3. LLT 全量：`./gradlew :cfir:analysis-tests:test --rerun -x generateTestGeneratorForCfirAnalysisTestsTests` → 8425/1316 对照（零新增）
4. `./gradlew :analysis:analysis-api-cfir:test` → 1346/280 对照（IDE 从不设置 CfirAccessibilityFileScope → 零影响已论证）
5. `./gradlew :compiler:frontend:test` → 94/1（MacroConstructionArchitectureGuardTest 既有失败）
6. REPAIR_LOG.md 追加条目 + git status 检查

## 已知偏差清单（记录，不修）

1. `isTargetInSamePackageAsExtend` 的 `targetKey != null` 附加条件（vs 官方无目标声明时直接 std.core）
2. 成员级第四条件（`FindImplmentInterface`，ImportManager.cpp:1456-1483）无 CFIR 对应——既有整体缺口（extend 级拒绝覆盖其 deserialized 场景）
3. 官方诊断 note（"must import at least one of its inherited interfaces" L1422-1426）由消费 checker 生成
4. fallback 派生的"声明 kind 非接口"resolver 校验省略（deserialized coneType 标记已正确）
5. compileTestsOnly 测试包特权（L1395-1398/L1494-1497）无 CFIR 对应
6. `IsDeclAccessible` 的 typealias 导入链特例（L1337-1342）无 CFIR 对应
7. `IsExportedDecl` 的类型实参导出检查（Node.cpp:1180-1184）无 CFIR 对应

## 集成测试说明

`cfir-serialization` 不依赖 `cfir/resolve` → checker/own provider/indexStore 不可达 → 集成测试无法触达修复代码；现有 `CjoSdkDeserializationIntegrationTest` L391 已覆盖"deserialized 默认不预判"语义，无需新增。