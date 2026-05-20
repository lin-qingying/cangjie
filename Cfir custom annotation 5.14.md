# CFIR Custom Annotation 闭环实施计划 v5.14 Frozen

## Summary
v5.14 冻结为实施基线。目标是修复声明/参数 annotation 只进入 macro surface、不进入 `CfirAnnotationContainer.annotations` 的断链，并保持 `PLAN.md` 的 CFIR 宏展开完整基线。新增门禁：multi-session classification 聚合、preConstruction failure path freeze/register、analysis/LL/checker 一次切到 CFIR annotation 入口。

## Hard Gates
- Raw CFIR First：PSI/LightTree raw builder 必须先把所有声明/参数 annotation 构造成 `CfirAnnotationCall` 并 append 到 `owner.annotations`，再创建 macro surface、annotation carrier、metadata。
- Two-Phase Classification：classification 是两阶段状态：`PreArtifactDemandSnapshot` 驱动 artifact/source-package preparation；artifact definitions 回填后生成并冻结 `FinalMacroSurfaceDecisions` 驱动 expand。
- Multi-Session Aggregation：artifact preparation 聚合所有 session 的 `PreArtifactDemandSnapshot`；artifact definitions 回填后，再逐 session freeze `FinalMacroSurfaceDecisions`。
- Failure Path Registration：strict artifact/preConstruction error 直接失败时，也必须 freeze metadata/classification，并把 session component/registry 状态注册完整，不能只返回 diagnostics。
- Parser API：`MacroFragmentParser.parse` 接收 decision/snapshot；custom annotation 输入完整 annotation slot snapshot，不再只接收裸 tokens。
- Analysis/LL/Checker Cutover：ordinary resolve、LL/lazy、public analysis annotations、checker semantics 必须一次切到 CFIR annotation + metadata 入口，禁止只改 public list 后保留 PSI fallback。
- Cache Stability：`surfaceId` 不进 stable hash；annotation slot snapshot 必须进入 `MacroExpansionCacheKey.stableHash()`。

## Classification Model
- `PreArtifactDemandSnapshot` 在 raw build 后、artifact preparation 前创建，包含 surface、callSite、kind、FQN、same-package result、builtin/non-macro result、import candidates、annotationCarrier、raw metadata。
- `FinalMacroSurfaceDecision` 字段至少包括：`surface`、`callSite`、`slotType`、`annotationCarrier`、`resolution`、`parserMode`、`localConstruction`、`executorRequired`、`externalPackageDemand`、`failurePolicy`、`blockedDiagnostic`。
- Artifact preparation 只消费所有 session decisions 的 `externalPackageDemand`。
- `MacroConstructionService.expand` 只消费本 session frozen decisions 中 `localConstruction=true` 的 surface，禁止重新 resolve/routing 或直接扫 `preFile.surfaces`。
- `noDemandCustomAnnotation` 必须表示为：`externalPackageDemand=null`、`executorRequired=false`、`localConstruction=true`、`slotType=ANNOTATION`、`parserMode=CUSTOM_ANNOTATION`。

## Semantic Contracts
- Same-package wins：annotation site 的 `@Foo/@!Foo` 命中同包 `macro Foo` 时 blocked；同包 macro 名为 `IfAvailable`、`sourceFile` 等 builtin 名称时也 blocked。
- Annotation-site `@!X` 只在 same-package check 之后 fixed custom；不 external demand、不 executor、不影响普通 macro call forced-kind 校验。
- Plain annotation-site `@X` 优先级：same-package blocked -> builtin non-macro -> builtin macro -> macro artifact/source evidence -> custom fallback。
- Unknown ordinary annotation 不产生 construction `MACRO_UNRESOLVED`；semantic unresolved 留给 ordinary resolve/checker。
- `CUSTOM_ANNOTATION` parser 消费完整 slot snapshot，并返回 `CfirAnnotationCall` payload；`@!Anno[...]` 必须被 parser 显式接受，或规范化为 `@Anno[...]` 且 metadata 保留 forced-custom flag。
- Custom success 固定 replace-at-index；禁止 append 第二个 annotation，禁止复用原 annotation 作为成功 payload。

## Implementation Order
- Step 1：新增 `CfirAnnotationMetadataRegistry` session component，raw build 前创建，construction 期可写，stable splice/cache snapshot 或 failure path 后冻结。
- Step 2：扩展 model：`MacroCallSite`、`MacroReplacementSlotType`、`CfirReplaceHandle.annotationCarrier`、`CfirAnnotationReplaceCarrier`、`MacroFragmentResult.CustomAnnotation(payload)`。
- Step 3：PSI/LightTree raw builder 构造所有声明/参数 `CfirAnnotationCall`，append 到 `annotations`，记录 index/original identity/metadata，修正 PSI FQN。
- Step 4：实现 two-phase `MacroDemandClassification`，支持 multi-session aggregation 与逐 session freeze。
- Step 5：改 `MacroFragmentParser` API 与默认 PSI/LightTree reparse：`CUSTOM_ANNOTATION` 使用 annotation-only parse，返回并校验 `CfirAnnotationCall` payload。
- Step 6：stable splicer 按 annotation carrier replace-at-index，做 metadata migrate、slot routing fail-fast、duplicate owner/index failure、owner/parameter + annotation conflict failure。
- Step 7：共享 annotation resolve 入口接入 ordinary resolve、lazy/LL resolve，删除 `psi == null` 时重建 PSI annotation 的路径。
- Step 8：analysis/checker 切 CFIR annotations：public list、argument conversion、Deprecated、Java/ObjC、Mock、custom-place、APILevel、Syscap。
- Step 9：cache 修正：`hashSurfaces` 不含 `surfaceId`，`hashResultSnapshot` 包含 nested declarations 与 annotation slot snapshot，进入 `MacroExpansionCacheKey.stableHash()`。

## Test Plan
- Providers/classifier：two-phase lifecycle、multi-session aggregation、failure path freeze/register、final decisions frozen、same-package wins including builtin names、`@!` fixed custom、plain `@IfAvailable` builtin non-macro、unknown annotation no construction unresolved。
- Parser：`CUSTOM_ANNOTATION` receives full snapshot for `@Anno[...]` and `@!Anno[...]`，payload is `CfirAnnotationCall`，tokens-only path is rejected。
- Raw PSI/LightTree：all declaration/parameter annotations enter `annotations`; FQN/index/original identity/metadata stable; two annotations on same owner route independently。
- Frontend/splice：shared classification drives artifact preparation and expand; replace-at-index; slot routing fail-fast; strict/degraded failure policies; owner/parameter + annotation conflict。
- Resolve/analysis/checker：ordinary and LL/lazy resolve use shared annotation resolve; public annotations have `psi == null` and CFIR arguments; checker semantics read CFIR annotations。
- Cache：surfaceId changes do not change stable hash; annotation replacement content changes do change cache key。

## Assumptions
- 本轮只处理声明与参数 annotation，不扩展表达式位置 annotation。
- Annotation 参数重建唯一 delimiter 是 `[]`，不使用 `hasParenthesis` 推断。
- 普通 import 本身不是宏证据；外部 artifact 是否可把普通 annotation 解析成 macro，只能在 final decisions 阶段基于已发现 artifact definitions 判断。
