# 宏生成声明的 references 支持：设计方案（已审查）

> 状态：已实现并通过子代理审查（有条件通过，意见已吸收）
> 相关：`docs/plans/macro-expansion-in-analysis.md`、`docs/macro-expansion-host-integration.md`

## 1. 问题

`textDocument/references` 对宏生成声明（例如宏展开出的 `generatedByMacro()`）返回空。

## 2. 根因（审查后精确化）

`AnalysisApiCangjieAnalysisFacade.references` 原先通过：

```kotlin
findTargetElements(document, snapshot.psiFile, position)
    .mapNotNull(::targetKeyFor)
```

获取目标键。平台 target 提取经 `CaCfirReference.getResolvedToPsi` →
`CfirDeclaration.findReferencePsi`（`analysis-api-cfir`）把**宏生成声明还原成宏来源 PSI**
（`@Make` / `carrier`），于是目标键是 carrier 的 `CallableId`；而扫描侧
`targetKeyForReferenceLike` 用 `resolveToSymbol()` 得到生成符号的键，两者不相等 → references 为空。

## 3. 设计（已实现）

### 3.1 目标键优先从引用表达式解析

`AnalysisApiCangjieAnalysisFacade.references`：

```kotlin
val targetKeys = semanticSupport.analyzeSnapshot(document) { snapshot ->
    val directReference = semanticSupport.referenceTargetKey(
        session = this,
        document = document,
        file = snapshot.psiFile,
        position = params.position,
    )
    if (directReference != null) {
        setOf(directReference)
    } else {
        semanticSupport.findTargetElements(document, snapshot.psiFile, params.position)
            .mapNotNull(semanticSupport::targetKeyFor)
            .toSet()
    }
}
```

`referenceTargetKey` 直接取当前位置的 `CjReferenceExpression`，走
`CaSession.targetKeyFor(reference)`：先 `resolveToSymbol()`，符号为 `CaCallableSymbol` 且有
`callableId` 时生成 `AnalysisApiLspTargetKey.Callable`；解析失败才回退 PSI。

### 3.2 声明键与符号键统一（审查意见）

- `targetKeyFor(CjCallableDeclaration)` 改用 `callableIdForName(nameAsSafeName)`
  （成员函数得到含 `classId` 的 `CallableId`，不再是 `CallableId(package, null, name)`）；
- `CjVariable` 等模式声明在 `callableIdForName` 抛 `UnsupportedOperationException` 时退回
  `Local` 键；
- `localTargetKey` 使用 `nameIdentifier` 范围（与声明扫描一致），不再用整声明范围；
- `targetKeyFor(reference)` 不再用 `runCatching` 吞掉 `ProcessCanceledException`，改为
  `catch (PCE) throw`、其余异常返回 null。

### 3.3 includeDeclaration 语义（审查修正）

`includeDeclaration` 分支只扫描工作区 PSI 声明并比较目标键；生成声明没有物理 PSI，
因此**不会**为生成声明虚构位置，也不会自动回到宏 surface。行为是：

- usage 点：由 CFIR 符号键匹配；
- 声明位置：仅当目标键能匹配到某个物理声明时才加入；
- 生成声明的 navigation 来源仍由 definition 的 `findReferencePsi` 语义决定（回到 `@Make`）。

## 4. 已知限制

- 生成函数来自 `dummy.cj.macrocall` 合成 fragment 文件，其 `CallableId` 当前落在
  `FqName.ROOT` 伪包；LSP 层已用“引用所在文件的包”重写键，跨包同名不再撞键
  （协议测试覆盖）；跨包 **import 使用**生成函数时仍按使用点包重写，语义上是已知近似；
- `useLightTree=true` 时生成符号的键空间会切换为 CFIR 真 `callableId`，当前测试只覆盖
  `useLightTree=false`；
- `documentHighlight` 已同步支持生成符号（协议测试覆盖）；
- `rename` 未支持宏生成声明（生成代码无法物理重写），属预期限制；
- 生成声明没有 cdoc，hover 内容可能为空。

## 5. 测试（已落地）

`LspMacroExpansionProtocolIntegrationTest`（Linux + CANGJIE_HOME）：

1. 真实 SDK 宏包 + 真实 `LSPMacroServer`；
2. A.cj：`@Make`、`generatedByMacro()` 使用点；B.cj：跨文件使用点；
3. definition 目标行必须是 `@Make`/`carrier`（生成声明无 PSI）；
4. hover 请求成功；
5. pull diagnostics 无 `MACRO_`/`UNRESOLVED`；
6. references（includeDeclaration=true）包含 A.cj 与 B.cj 的 `generatedByMacro` 行；
7. references（includeDeclaration=false）仍包含使用点；
8. 成员函数回归：`includeDeclaration=true` 时同时包含声明与 `c.member()` 使用点。

## 6. 审查结论

两个子代理均为**有条件通过**，主要意见已吸收：

- 架构审查：统一声明/符号键、PCE 重抛、`localTargetKey` 用 nameIdentifier、修正文档中
  “生成声明 CallableId 是 sample 包 / includeDeclaration 回到宏 surface”的错误描述；
- 测试审查：补 `includeDeclaration=false`、跨文件、成员函数声明回归；指出
  `@EnabledOnOs(LINUX)` + `CANGJIE_HOME` 在默认 CI 会 skip（仍需专用 job 提供 SDK 才能生效），
  以及临时目录未清理、无 `@Timeout` 等稳定性问题（未全部处理）。

## 7. 未采用方案

- CFIR 目标键 + 扫描展开 CFIR：语义最完整，但需新目标键模型与位置映射，代价大；
- 按名字扫描 PSI：会误匹配同名符号；
- 不做 references：不满足验收。
