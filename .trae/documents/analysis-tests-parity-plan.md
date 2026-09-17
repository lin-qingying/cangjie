# 对齐 Kotlin analysis 模块测试：仓颉 analysis 测试框架组织与内容补齐计划（v2）

## Context

**目标**：对齐 Kotlin 编译器 analysis 模块的**测试框架组织**与**测试内容**，补齐仓颉 `analysis/` 各模块测试覆盖。

**调研结论**：

* 三层测试结构（testData → testFixtures 抽象基类 → TestGenerator 生成 tests-gen）仓颉已复刻，但**框架组织仍有 6 项差异**（死指令、variant 未启用、golden 批量更新缺失、testData 接线差异、analysis-api 无自测、build 排序缺失）。

* 内容缺口经 API 存在性调查：**6 个需新增测试基类**（resolveCandidates、readWriteAccess、isOperator、containingModuleByFile、annotationApplicableTargets、typeByTypeReference），**2 个已覆盖**（isStatementLike 对偶 isUsedAsExpression、containingDeclaration 已有测试），14 个 N/A 文档化跳过。

* 已确认决策：N/A 维度文档化跳过；分阶段连续执行，阶段间不打断征求决策。

***

## Part A：框架组织对齐工作清单（6 项）

### F1（P0，先行）：修复死指令 IGNORE\_STANDALONE / DISABLE\_DEPENDED\_MODE

* **问题**：`AnalysisApiTestDirectives` 声明了 `IGNORE_STANDALONE`（29 行）与 `DISABLE_DEPENDED_MODE`（37 行），但 [AbstractAnalysisApiBasedTest.kt](file:///d:/code/intellij/cangjie/analysis/analysis-test-framework/testFixtures/org/cangnova/cangjie/analysis/test/framework/base/AbstractAnalysisApiBasedTest.kt) 的 `runTest`（311-355 行）从不读取，指令是死代码。

* **改法**：在 `runTest` 开头按 Kotlin 语义补跳过逻辑：`DISABLE_DEPENDED_MODE` → 若 `configurator.analyseInDependentSession` 直接 return；`IGNORE_STANDALONE` → 若 standalone 模式 return。

* **文件**：`AbstractAnalysisApiBasedTest.kt`。

### F2（P0）：standalone 启用 golden variant（`standalone.cfir`）

* **问题**：Kotlin standalone 用 `testPrefixes = ["standalone.fir"]`；仓颉 `testPrefixes` 全空，standalone 与 IDE 输出不同时互相覆盖，且 IDE golden 可能被本地自动创建逻辑误改。

* **改法**：给 `CaCfirStandaloneAnalysisApiTestConfigurator`（`analysis/analysis-api-standalone/testFixtures/.../configurators/CaCfirStandaloneAnalysisApiTestConfigurator.kt`）设 `testPrefixes = listOf("standalone.cfir")`。

* **注意**：启用后现有 standalone golden 文件需批量添加 `.standalone.cfir` 前缀 → 用机制 A 重新生成后统一 `git mv`。

* **文件**：standalone configurator + `AbstractAnalysisApiBasedTest`（变体查找逻辑已存在，无需改）。

### F3（P2）：build 排序

* standalone 与 low-level 的 `testTask` 加 `mustRunAfter(":analysis:analysis-api-cfir:test")`（对齐 Kotlin golden 先跑约定）。

* **文件**：`analysis/analysis-api-standalone/build.gradle.kts`、`analysis/low-level-api-cfir/build.gradle.kts`。

### F4（P2）：golden UPDATE 机制接线

* **问题**：`update.test.data` 属性被 build 转发给测试 JVM，但 [DefaultAssertionsService.kt](file:///d:/code/intellij/cangjie/tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/services/impl/DefaultAssertionsService.kt) 的 `assertEqualsToFile` 不消费它；analysis 测试无法批量更新 golden（只能删文件重跑）。

* **改法**：`JUnit5Assertions.assertEqualsToFile` 在 mismatch 时读 `System.getProperty("update.test.data")`，为 true 则覆写 golden（对齐 Kotlin TestDataAssertions 行为）。

* **影响面**：仅 analysis 相关 golden 断言；`cfir/analysis-tests` 等已有独立 update 分支不受影响。

### F5（P3）：analysis-api 模块自测（对齐 Kotlin 7 个 surface 校验中的 3 个）

* 新增 `analysis/analysis-api/tests/` source set（`projectDefault()` + `project-tests-convention`）：

  1. `AnalysisApiNameConventionTest` — 所有 public 顶层类须 `Ca` 前缀（含豁免表）
  2. `AnalysisApiExtensibilityTest` — 可继承类须 `@CaSpi` / sealed / `@SubclassOptInRequired` 限制
  3. `AnalysisApiKDocCoverageTest`（降级）— 收集无 KDoc public 声明与 `api/analysis-api.undocumented` 比对，文件缺失时本地自动生成（依赖 F4 的 update 机制）

* 基类：新建 `analysis-test-framework/testFixtures/.../AbstractAnalysisApiCodebaseTest`（PSI 遍历 `src/org/cangnova/cangjie/analysis/api`，对齐 Kotlin `AbstractAnalysisApiCodebaseTest`）。

* 注：surface dump 一致性仓颉已有自研 `ApiSurfaceExtractor`（`analysis-api/build.gradle.kts` + `api/analysis-api.txt`），保留；Mixin/bridges 校验降级不做（组件数少价值低）。

### F6（P3，独立二期）：最小 test-data-manager 等价物

* **问题**：Kotlin 有 `manageTestDataGlobally --mode=update` CLI + test-data-manager 插件；仓颉缺失，批量 check/update golden 靠手工。

* **最小方案**（不做 variant 冲突检测/incremental）：

  * 新模块 `analysis/test-data-manager`：`testFixtures/` 下 `ManagedTest`、`ManagedTestAssertions`（CHECK/UPDATE 行为矩阵）、`TestDataManagerRunner`、`filters/`；build.gradle.kts `main { none() }`。

  * 新约定 `repo/gradle-build-conventions/test-data-manager-convention/`：`manageTestData`/`manageTestDataGlobally` 两个 JavaExec task。

  * `AbstractAnalysisApiBasedTest` 实现 `ManagedTest` 接口，`assertEqualsToTestOutputFile` 走 `ManagedTestAssertions`。

* **注**：这是独立二期项，置于 P3 最后，若时间/风险受限可单独排期。

### N/A（框架层）

FE10 双实现（Fe10TestConfigurator、`.descriptors.txt`、`IGNORE_FE10`/`IGNORE_FIR`、K1/K2 指令分流）、binary ABI dump、JS/Wasm runtime 支撑、`codebaseTest` source set、low-level 子模块聚合 → 仓颉单前端/无多平台，全部 N/A。

***

## Part B：Phase 1（P0）组件层新增测试

### 通用落地模板（每个领域）

1. 新建 `AbstractXxxTest.kt` 于 `analysis-api-impl-base/testFixtures/org/cangnova/cangjie/analysis/api/impl/base/test/cases/components/<族>/`（继承 `AbstractAnalysisApiComponentTest`；参考 [AbstractResolveSymbolTest.kt](file:///d:/code/intellij/cangjie/analysis/analysis-api-impl-base/testFixtures/org/cangnova/cangjie/analysis/api/impl/base/test/cases/components/resolver/AbstractResolveSymbolTest.kt)）。
2. 新指令加入 `AnalysisApiComponentTestDirectives.kt` 或新建 `AnalysisApiXxxTestDirectives.kt`。
3. testData 置于 `analysis/analysis-api/testData/components/<族>/<目录>/`（注解族在 `testData/annotations/`）。
4. 在 [TestGenerator.kt](file:///d:/code/intellij/cangjie/analysis/analysis-api-impl-base/testFixtures/org/cangnova/cangjie/analysis/api/impl/base/test/dsl/TestGenerator.kt) `generateAnalysisApiTests()` 注册。
5. 重新生成（`generateTestGeneratorTests` 自动随 compileTestKotlin 触发）+ 跑测 + 审查 golden（机制 A 首跑自动创建）。

### B1. resolveCandidates（`components/resolver/resolveCandidates/`）

* **基类**：

  * `AbstractResolveSymbolsTest`（继承 `AbstractAnalysisApiComponentTest`）：按 `TARGET_NAME` 定位引用，`resolveToSymbols()` 渲染集合；golden `symbols.txt`（复用 `ResolveTestRenderers.renderSymbolForResolveTest`，空集合输出 `NO_SYMBOLS`）。

  * `AbstractResolveCallInfoTest`（继承 `AbstractResolveCallTest`）：`TARGET_CALL` 定位，`resolveToCall()` 渲染 `callInfoClass`（`CaBaseSuccessCallInfo`/`CaBaseErrorCallInfo`）+ `callsSize` + candidate 符号；golden `call.txt`。

* **用例**：

  | 文件                         | 场景                          | 预期                                              |
  | -------------------------- | --------------------------- | ----------------------------------------------- |
  | `overloadedCall.cj`        | 两个同签名重载                     | resolveToSymbols 2 个；error callInfo，callsSize 2 |
  | `uniqueCall.cj`            | 唯一调用                        | resolveToSymbols 1 个；success callInfo           |
  | `unresolvedCall.cj`        | 未定义调用                       | resolveToSymbols 空（NO\_SYMBOLS）；error callInfo  |
  | `ambiguityByConversion.cj` | 参数类型歧义（Int64 vs Float64 候选） | error callInfo，candidates 渲染                    |

### B2. readWriteAccess（`components/expressionInfoProvider/readWriteAccess/`）

* **基类**：`AbstractVariableAccessKindTest`（继承 ComponentTest）：`TARGET_CALL` 定位变量访问表达式 → `resolveToCall()` → `CaVariableAccessCall.kind`；golden `kind: Read` / `kind: Write(value: 2)`。

* **用例**：

  | 文件                                     | 场景                      | 预期              |
  | -------------------------------------- | ----------------------- | --------------- |
  | `variableRead.cj`                      | `let v = 1; consume(v)` | Read            |
  | `variableWrite.cj`                     | `v = 2`                 | Write(value 渲染) |
  | `propertyRead.cj` / `propertyWrite.cj` | 成员属性读写                  | Read / Write    |

* **依赖 API**：`CaVariableAccessCall.kind`（`CaCallInfo.kt` 152-201 行，`Write` 带 `value: CjExpression?`）。

### B3. isOperator（`components/symbolInformationProvider/isOperator/`）

* **基类**：`AbstractIsOperatorTest`：caret 定位函数声明（`getBottommostElementOfTypeAtCaret<CjFunctionDeclaration>` 或 `TARGET_FUNCTION`），取 `(declaration.symbol as CaFunctionSymbol).isOperator`；golden `isOperator: true/false`（可附带 `isStatic`/`isMutating`）。

* **用例**：

  | 文件                    | 场景                                                      | 预期    |
  | --------------------- | ------------------------------------------------------- | ----- |
  | `operatorFunction.cj` | `public operator func +(self: User, other: User): User` | true  |
  | `regularFunction.cj`  | 普通函数                                                    | false |
  | `operatorOnType.cj`   | `operator func <` 比较操作符                                 | true  |

### B4. containingModuleByFile（`components/containingModuleProvider/containingModule/`）

* **基类**：`AbstractGetModuleTest`（继承 ComponentTest）：主文件元素 → `session.getModule(element)` → 渲染 `moduleDescription` + `stableModuleName` + `isResolvable`。

* **用例**：

  | 文件                         | 场景                                                                | 预期                         |
  | -------------------------- | ----------------------------------------------------------------- | -------------------------- |
  | `sourceModule.cj`          | 主模块源码元素                                                           | `Sources of main` / `main` |
  | `libraryDependency.cj`     | 多模块，依赖库（`MODULE: main(sampleLib)` + SDK\_LIBRARY 或 LibraryBinary） | `CaLibraryModule`          |
  | `crossModuleDependency.cj` | 引用依赖模块声明                                                          | 归属依赖模块                     |

* **依赖 API**：`CaSession.getModule(element)`（`CaSession.kt` 160 行）、`CaModule` 子类型 `moduleDescription`/`stableModuleName`。**注意**：CodeFragment/DanglingFile 场景（返回 `CaDanglingFileModule`）作为扩展用例，需 CodeFragment moduleKind 支持。

### B5. annotationApplicableTargets（`annotations/applicableTargets/`）

* **基类**：`AbstractAnnotationTargetTest`：caret 定位 `CjDeclaration` → `declarationSymbol.annotations` → 复用 `TestAnnotationRenderer` 渲染 `@classId[args]` + 追加 `target: <CangjieAnnotationTarget.name | null>`。

* **用例**：

  | 文件                       | 场景                                    | 预期                                                |
  | ------------------------ | ------------------------------------- | ------------------------------------------------- |
  | `explicitTarget.cj`      | `@A(target: [MEMBER_FUNCTION])` 在类成员上 | `target: MEMBER_FUNCTION`                         |
  | `noTargetDeclaration.cj` | 未声明 target                            | `target: null`                                    |
  | `allTargets.cj`          | `@A(target: [*])`                     | `target: ALL`                                     |
  | `invalidTargetUsage.cj`  | target 不匹配（如全局函数用 MEMBER\_FUNCTION）   | target 渲染 + 联动 `AnnotationNotApplicableJffi` 诊断用例 |

* **依赖 API**：`CaAnnotation.target: CangjieAnnotationTarget?`（`CaAnnotation.kt` 25 行，10 枚举值）。

### B6. typeByTypeReference（`components/typeProvider/typeByTypeReference/`）

* **基类**：`AbstractTypeByTypeReferenceTest`：caret 定位 `CjTypeReference` → `typeReference.type.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES)`（`normalizeTypeRendering`）；golden `CjTypeReference: <getTypeText()>\nCaType: <rendered>`（对齐现有 `typeReference/functionReturn.txt` 格式）。

* **用例**：`basicType.cj`（Int64）、`classType.cj`（User）、`genericType.cj`（`Box<Int64>`）、`functionType.cj`（`(Int64) -> Int64`）、`tupleType.cj`（`(Int64, String)`）、`typealiasType.cj`（typealias 展开）、`unionType.cj`（`Int64 | String`）。

* **注**：与现有 `AbstractTypeReferenceTest`（经 returnType）部分重叠，本测试直测 `CjTypeReference.type` 桥接（`CaTypeProvider.kt` 42-44 行）。

### 已覆盖项（不新建，补用例即可）

* **isUsedAsExpression**：现有 `AbstractExpressionInformationTest` + `expressionInfoProvider/basicInfo/` 已测 `isStatementLike`（对偶）。补 1 用例：`callAsExpressionArgument.cj`（调用作实参 → isStatementLike false）。

* **containingDeclaration**：现有 `AbstractContainingDeclarationProviderByReferenceTest` 已覆盖（含 extend/局部链）。不新建。

### 注册代码示例（TestGenerator.kt 内新增）

```kotlin
component("resolver") {
    // 已有 ... 追加：
    test<AbstractResolveSymbolsTest> { model(it, "resolveCandidates/symbols", excludeDirsRecursively = listOf("call")) }
    test<AbstractResolveCallInfoTest> { model(it, "resolveCandidates/call", excludeDirsRecursively = listOf("symbols")) }
}
component("expressionInfoProvider") {
    test<AbstractVariableAccessKindTest> { model(it, "readWriteAccess") }
}
component("symbolInformationProvider") {
    test<AbstractIsOperatorTest> { model(it, "isOperator") }
}
component("containingModuleProvider") {
    test<AbstractGetModuleTest> { model(it, "containingModule") }
}
component("typeProvider") {
    test<AbstractTypeByTypeReferenceTest> { model(it, "typeByTypeReference") }
}
group("annotations", filter = ...) {
    test<AbstractAnnotationTargetTest> { model(it, "applicableTargets") }
}
```

（具体 filter 沿用各能力族既有约束，如 Normal session + Source module kind。）

***

## Part C：Phase 2（P1）LL 层补齐

### C1. 缓存并发单元测试（纯单测，`low-level-api-cfir/test/.../caches/`）

* `ValueWithPostComputeTest`：多线程并发只算一次、`ValueIsPostComputingNow` 状态机、postCompute 抛错不缓存可重算（类：`src/.../caches/ValueWithPostCompute.kt`）。

* `StateKeeperTest`：`add/entity/entityList/postProcess`、失败 restore（`src/.../transformers/StateKeeper.kt`）。

* `CleanableWeakValueReferenceCacheTest`：`createCopy/createReference`、弱引用回收清理回调（`src/.../caches/cleanable/`）。

### C2. GetOrBuildCfir Binary 变体

* 新增 LL Binary 配置器：`analysisApiCfirBinaryTestConfigurator`（moduleKind=LibraryBinary、Normal session；工厂层已支持）。

* 新基类 `AbstractGetOrBuildCfirBinaryTest` + `testData/getOrBuildCfirBinary/`（从 binary 依赖解析声明，含 `<expr>` 标记渲染）。

### C3. InBlockModification 测试

* 基于 `CaTestModificationTracker` + `LLCfirDeclarationModificationService`：编辑函数体触发块内修改 → 断言 body 失效（回退 `CfirLazyBlock`、`decreasePhase`）→ 重新 lazyResolve 后恢复。

* testData 新增 `inBlockModification/`（或复用 `fileStructure`）。

### C4. DeprecationsResolve 测试

* 懒解析到 STATUS 后断言 deprecation provider 非 `UnresolvedDeprecationProvider`（复用 `checkDeprecationProviderIsResolved` 契约，`util/cfirCheckResolvedUtils.kt:147`）。

* 用例：`@Deprecated` 注解声明 + 使用点。

***

## Part D：Phase 3（P2/P3）模块级补齐

### D1. symbol-light-declarations 自定义场景（P2）

新增 `CaSymbolLightDeclarationCustomTest`：file modification tracker、enum entry 类型解析（typealias 同名回归）、注解参数 PSI 元素、facade light class（对齐 Kotlin `SymbolLightClassesCustomTest`）。

### D2. stubs/decompiled golden（P2）

对标 Kotlin `BuiltinsDecompilerTest`：对反编译库渲染 `.decompiled.text` + PSI 树 golden。先侦察 `analysis/decompiled` 渲染基建（`CaStubCompiledGoldenTest` 等），可用则落地，否则降级为现有 decompiled 产物断言。

### D3. analysis-api surface 自测（P3，= F5）

见 Part A F5。

***

## Part E：Phase 4 全量验证与提交

1. 每个阶段完成即跑测 + 提交（阶段内可多 commit，一个阶段一个功能单元）。
2. 全量验证命令：

   * `gradlew-queue.bat :analysis:analysis-api-cfir:test`

   * `gradlew-queue.bat :analysis:analysis-api-standalone:test`

   * `gradlew-queue.bat :analysis:low-level-api-cfir:test`

   * `gradlew-queue.bat :analysis:stubs:test :analysis:symbol-light-declarations:test`

   * （F5 后）`:analysis:analysis-api:test`
3. 输出对比报告：新增基类数、testData 用例数、tests-gen 增量、通过率。

***

## N/A 清单（文档化跳过，14 项）

| 维度                                                     | 原因                                   |
| ------------------------------------------------------ | ------------------------------------ |
| ScopeContextForPosition                                | 无位置感知作用域 API                         |
| SealedInheritors                                       | 仅反向 `isDirectSubClassOf`             |
| IsDenotable                                            | `CaType` 无 isDenotable               |
| ReturnTargetSymbol / ExitPointSnapshot                 | 无 return target / 退出点 API            |
| HasConflictingSignatureWith                            | 无冲突签名 API                            |
| DelegateMemberScope / StaticMemberScope                | 无 delegate 语义；static 仅 `isStatic` 标志 |
| SymbolMemoryLeak                                       | 无 leak 检测基建                          |
| UseSiteLibraryModuleAnalysisRejection                  | 无此机制，近似 `canBeAnalysed` 已测           |
| OriginalConstructorIfTypeAliased                       | 仅 `expandedType`                     |
| ResolveExtension 扩展点                                   | 仅陈旧 KDoc，无实际类型                       |
| CompilerFacility / CompilerPluginGeneratedDeclarations | 无编译器插件 API                           |
| DoubleColonReceiverType                                | 仓颉 `::` 是组织名分隔符                      |
| GetExpectsForActual / SAM / Java 静态                    | 语言无对应特性                              |

（TypeEquality/Subtyping 已由 `AbstractTypeRelationTest` 覆盖；isUsedAsExpression、containingDeclaration 已由现有测试覆盖。）

***

## 关键文件索引

* 生成 DSL：`analysis/analysis-api-impl-base/testFixtures/.../dsl/TestGenerator.kt`、`AnalysisApiTestGroup.kt`

* 测试基座：`analysis/analysis-test-framework/testFixtures/.../base/AbstractAnalysisApiBasedTest.kt`

* 指令池：`AnalysisApiComponentTestDirectives.kt` + 各 `AnalysisApiXxxTestDirectives.kt`

* 配置器：`CaCfirAnalysisApiTestConfiguratorFactory.kt`、`CaCfirStandaloneModeTestConfiguratorFactory.kt`、`AnalysisApiCfirSourceTestConfigurator.kt`

* golden 断言：`tests/test-infrastructure/testFixtures/.../services/impl/DefaultAssertionsService.kt`

* 生成器入口：各模块 `TestGenerator.kt`（cfir / standalone / low-level）

* LL 缓存设施：`low-level-api-cfir/src/.../caches/`、`transformers/StateKeeper.kt`

* 参照 Kotlin：`external/kotlin/analysis/test-data-manager/`、`external/kotlin/analysis/analysis-api/tests/`、`external/kotlin/analysis/analysis-api-fir/build.gradle.kts`
