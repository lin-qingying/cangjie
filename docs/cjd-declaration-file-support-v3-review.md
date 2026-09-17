# `.cj.d` 声明文件支持设计 v3 —— 复核报告

> **处置状态（2026-09-16 回填）**：本报告的全部条目**已处置完毕**，逐条记录见设计稿的
> [`附录 C.5 v3 → v3.1：复核报告的处置`](./cjd-declaration-file-support-v3.md#c5-v3--v31复核报告的处置)。
> 其中 3 项在处置时**未完全按本报告的建议执行**，理由已写明：
> **A-1**（改采用"`DEFAULT` 与 `ABSTRACT` 同源派生"这一第三解，无需 `.cj.d` 分支）、
> **A-4 判定依据**（改用文件种类而非本报告建议的 `configuration.compileCjd`，因为会话级布尔无法表达 IDE 的混合会话）、
> **A-4 A 表**（`CfirMemberBodyDeclarationChecker` 经核实**不应**打标记，否则会掩盖护栏缺陷）。
> 另有 8 项在复核范围之外的新发现，见 C.5.4。
>
> 复核对象：`docs/cjd-declaration-file-support-v3.md`（3468 行 → 修订后 4161 行）
> 复核依据：`external/cangjie_compiler`（官方实现镜像，只读）+ 本仓当前工作树
> 复核方式：对文档第 1 章的官方语义断言逐条回到官方源码定位；对第 2/4 章的本仓断言逐条回到本仓源码定位；
> 对文档自称"穷举""实测"的统计做全量 grep 复核。
> 复核日期：2026-09-16

---

## 0. 结论摘要

**总体判断：这是一份事实质量很高的设计稿。** 官方语义红线 R1–R14、13 处官方取证、3 条 `DIFF` 中的关键一条（`DIFF-1` 官方越界 UB）经逐条复核**全部成立**；本仓侧的文件行号、结构描述绝大多数准确。附录 A/B 的取证密度明显高于一般设计稿。

但方案存在 **1 处框架级遗漏、1 处自相矛盾、1 处逻辑错误、1 处施工层错误**，以及若干会破坏"实测/穷举"承诺的计数错误。按严重度分级：

| 级别 | 编号 | 问题 | 是否阻断施工 |
|---|---|---|---|
| **必修** | A-1 | 官方 `SetDefaultFunc` 在声明模式下会为 interface 无体函数合成 `Attribute::DEFAULT`——文档的"四状态表"与"护栏 2 结论：不改"均未覆盖 | 是（会导致 `.cj.d` interface 语义错误） |
| **必修** | A-2 | `ParsingContext.DECLARATION_FILE` 在 4.2.3 说"不新增"，在 4.2.5 又说"仍然需要"并直接使用 | 是（施工者无法判断） |
| **必修** | A-3 | 4.4.2 提出的 `convertToSourceFiles` 条件改法**达不到它自己声明的目的**（对 `.cj.d` 是空操作） | 是（照抄会得到一个失效的修复） |
| **必修** | A-4 | F5/4.4.5 的 `interface CfirChecker` **在本仓不存在**；"分派处 1 处"落在**生成代码**里，文档全文未提生成器 | 是（施工者会去改"请勿手动修改"的文件） |
| 补正 | B-1 | `ParsingContext` 统计错误（文档 14 预设 / 11 死预设；实际 **11 预设 / 9 死预设**），且文档自身算术不自洽 | 否（但 C-5 施工目标错误） |
| 补正 | B-2 | 缺体诊断上报点"穷举"漏 `CangJieParsing.kt:3004`（实际 17 处，文档覆盖 16 处） | 否 |
| 补正 | B-3 | OP1 的"覆写只有这一处"不实（实际 2 处）；`CangJieFileType.INSTANCE` 计数口径不一致 | 否（结论仍成立，且漏了一条**支持** OP1 的论据） |
| 补正 | B-4 | 4.9.2 的 `createFile` 改动用的是 v2 的构造参数，与 v3 的 4.2.2 冲突；且在 v3 下这一处**根本不用改** | 是（代码无法编译） |
| 澄清 | C-1 | `CangJieParser.parse` 的"宏调用"判定与 `CjSourceKind.MACRO_CALL` 构成两个真源（J2 反面） | — |
| 澄清 | C-2 | "`.cj` 与 `.cj.d` 在同一 module 并存"是官方不存在的模式，其权威声明归属未定义 | — |
| 澄清 | C-3 | `analysis/decompiled` 与 `cfir-serialization` 两条 `.cjo → 声明` 路径的注解一致性未覆盖 | — |
| 澄清 | C-4 | `DIFF-5`（sidecar 不做宏展开）的缓解论证尚未对真实 SDK 样本验证 | — |

---

## 1. 必修项

### A-1 【框架级遗漏】官方在声明模式下会为 interface 无体函数合成 `Attribute::DEFAULT`

#### 事实

官方 `ParserImpl::SetDefaultFunc`（`external/cangjie_compiler/src/Parse/ParseDecl.cpp:874-898`）：

```cpp
void ParserImpl::SetDefaultFunc(ScopeKind scopeKind, AST::Decl& decl) const
{
    if (scopeKind != ScopeKind::INTERFACE_BODY) {
        return;
    }
    if (decl.astKind == ASTKind::FUNC_DECL) {
        bool defaultFunc = !decl.TestAttr(Attribute::FOREIGN) && !decl.TestAttr(Attribute::ABSTRACT);
        if (defaultFunc) {
            decl.EnableAttr(Attribute::DEFAULT);          // ← 声明模式下这条会命中
        }
    } else if (decl.astKind == ASTKind::PROP_DECL) {
        if (decl.TestAttr(Attribute::ABSTRACT)) {
            return;                                        // ← 属性被 R4 挡住，无差异
        }
        ...
    }
}
```

调用点 **不带任何 `parseDeclFile` 门禁**：
- `ParseDecl.cpp:1529`（FuncDecl 路径，紧邻 `CheckFuncBody`，后者在第 1533 行）
- `Parser.cpp:392`（PropDecl 路径）

全仓 `parseDeclFile` 分支点穷举（`grep -rn "parseDeclFile" src/ include/`）只有 7 处：`ParseDecl.cpp:539 / 582 / 1550`、`Parser.cpp:398`、`ParserDiag.cpp:1086 / 1113 / 1218`。**`SetDefaultFunc` 不在其中。**

#### 后果

`.cj.d` 中 `interface I { func f(): Int64 }`：

| | `.cj` | `.cj.d`（官方） | `.cj.d`（本仓 v3 方案） |
|---|---|---|---|
| body | 无 | 无 | 无 |
| `ABSTRACT` | **有**（`ParseDecl.cpp:1550`，`scopeKind==INTERFACE_BODY`） | **无**（`:1550` 的 `!parseDeclFile` 挡住） | 无（护栏 1，正确） |
| `DEFAULT` | 无（`SetDefaultFunc` 见 `ABSTRACT` 即不置位） | **有**（`:874` 命中） | **无** |

⇒ 文档 1.3.2 的"`FuncDecl` 第四种状态"表只列了 `ABSTRACT` 一维，**没有第五种状态**；1.3.5「唯一差别是属性位：不含 `ABSTRACT` / `HAS_BROKEN`」是一张**只列"少了的"属性、没列"多了的"属性**的清单。

`Attribute::DEFAULT` 在官方 Sema 中被大量消费，且**影响 ABI**：
- `Sema/InheritanceChecker/StructInheritanceChecker.cpp:776 / 825 / 895 / 914 / 1163`
- `Sema/DefaultImplementHandler.cpp:362`、`Sema/TypeChecker.cpp:2554`、`Sema/TypeCheckExtend.cpp:105`
- `Sema/InheritanceChecker/MergeInheritedMemberHelper.cpp:190`
- **`Modules/ASTSerialization/ASTWriter.cpp:1244`**（`decl.TestAttr(Attribute::DEFAULT)` 参与 raw mangled name 判定）

#### 与本仓的冲突

本仓对应实现：

```kotlin
// psi2cfir: PsiRawCfirBuilder.kt:3902-3911
private fun isDefaultInterfaceMember(declaration: CjDeclaration): Boolean {
    if (containerSymbolIfAny !is CfirInterfaceSymbol) return false
    return when (declaration) {
        is CjNamedFunction -> !declaration.hasModifier(CjTokens.FOREIGN_KEYWORD) && declaration.hasBody()
        ...
    }
}

// light-tree2cfir: LightTreeRawCfirDeclarationBuilder.kt:3270-3272
private fun isDefaultInterfaceAccessor(node, modifiers) =
    isInInterfaceMemberContext() && !modifiers.isAbstract && hasSyntaxBody(node)
```

`.cj` 与 `.cj.d` 在 `hasBody()` / `hasSyntaxBody()` 上**都为 false** ⇒ 本仓两条路径在两种模式下都判为"非 default"。文档 4.5.2 护栏 2 由此得出「两条路径判定结果**本来就一致** ⇒ 护栏在此处无任何行为差异，加护栏只是噪声 ⇒ **确认不改**」。

**这个推理比较的是"本仓把 `.cj` 解析一遍 vs 把 `.cj.d` 解析一遍"，而不是"本仓 vs 官方"。** 在文档自设的"精确复刻官方"目标下，该结论不成立：官方在此处**新增**了一个属性位，本仓没有。

#### 建议

二选一，并写进文档：

1. **复刻**：`isDefaultInterfaceMember` / `isDefaultInterfaceAccessor` 对 `.cj.d` 的 interface 无体成员返回 `true`。
   注意这不是"加护栏"（`&& !isDeclarationFile`），而是**反方向的新增判定**——这也正是护栏 2 被误判为"噪声"的原因。
2. **不复刻**：登记为 `DIFF-6`，显式说明"本仓认为声明文件的无体成员不构成默认实现"，并给出理由与影响面。
   （该取向在工程上更自然，但与文档"与官方一致"的整体姿态需要对齐。）

无论选哪条，**1.3.2 的状态表与 1.3.5 的"唯一差别"都必须改写**，因为它们现在是错的。

> 附带确认：**属性（PropDecl）路径无差异** —— `SetDefaultFunc` 的 `PROP_DECL` 分支见 `ABSTRACT` 即 return，
> 而 `.cj.d` 的类/接口无 `{}` 成员属性按 R4 仍带 `ABSTRACT`，故不置 `DEFAULT`。文档 R3/R4 的不对称判断本身是对的。

---

### A-2 【自相矛盾】`ParsingContext.DECLARATION_FILE` 到底建不建

文档内部两处直接冲突：

| 位置 | 原文 |
|---|---|
| 4.2.3（`:1056-1057`） | 「**不**向 `ParsingContext` 增加 `isDeclarationFile` 字段，也**不**新增 `DECLARATION_FILE` 预设。」 |
| 4.2.5（`:1184-1188`） | `private fun fileParsingContext(): ParsingContext = when (sourceKind) { SOURCE -> ParsingContext.DEFAULT; DECLARATION -> ParsingContext.DECLARATION_FILE; MACRO_CALL -> ParsingContext.ANNOTATION_ONLY }` |
| 4.2.5（`:1212`） | 「**`ParsingContext.DECLARATION_FILE` 仍然需要**，但角色变了」 |

更进一步：按 D2 的规定「解析器内部所有需要判断模式的地方读 `sourceKind`（构造属性），**不再读 `parseContext`**」，那么 `DECLARATION_FILE` 里**没有任何字段可设**——官方声明模式在解析层只改了 `parseDeclFile` 一个开关（我已穷举 7 处，全部是诊断/属性位抑制，不涉及任何其它 parse 行为）。⇒ `DECLARATION_FILE` 与 `DEFAULT` **完全等价**，`fileParsingContext()` 的三分支里有一支是恒等映射，这个预设在 v3 框架下是**语义空壳**。

**建议**：删掉 `DECLARATION_FILE`，`fileParsingContext()` 只映射 `SOURCE → DEFAULT` 与 `MACRO_CALL → ANNOTATION_ONLY`，并在注释里写明"DECLARATION 不产生上下文差异，因为声明模式完全由 `sourceKind` 承载"。这同时让 3.2 分层表、8.3-F2/F3、8.6 净效果表的表述一致。

---

### A-3 【逻辑错误】4.4.2 的条件改法达不到它声明目的

文档原文（`:1530`）：

> `convertToSourceFiles` 分支（`:139-151`）的 `virtualFile.extension == CangJieFileType.EXTENSION` 判断要改成 `sourceKind == SOURCE`，
> 否则声明文件会误入 `applyCfirProcessSourcesExtension` 插件扩展路径。

本仓现状（`compiler/frontend/src/org/cangnova/cangjie/frontend/sources/GroupedCjSources.kt:139-151`）：

```kotlin
convertToSourceFiles = { virtualFile ->
    val sources = listOf<CjSourceFile>(CjVirtualFileSourceFile(virtualFileCreator.create(virtualFile)))
    if (virtualFile.extension == CangJieFileType.EXTENSION) {   // "cj"
        sources
    } else {
        applyCfirProcessSourcesExtension(...) ?: sources          // ← 插件扩展路径
    }
}
```

按文档改成 `sourceKind == SOURCE` 后的实际行为：

| 输入 | `sourceKind` | 新条件 | 走向 |
|---|---|---|---|
| `a.cj` | SOURCE | true | 直连（不变） |
| `a.cj.d` | **DECLARATION** | **false** | **仍然是 `applyCfirProcessSourcesExtension`** ✗ |

⇒ 改动对 `.cj.d` 是**空操作**，并没有"把声明文件带离插件扩展路径"。

**正确改法**是"Cangjie 家族文件走直连分支"，例如：

```kotlin
if (virtualFile.extension == CangJieFileType.EXTENSION || virtualFile.sourceKind().isDeclaration) {
    sources
} else {
    applyCfirProcessSourcesExtension(...) ?: sources
}
```

另外一个需要指出的伪代码失真：文档把 else 分支写成 `else -> { /* 保持既有 else 分支：非 .cj 一律拒绝 */ }`，但现状的 else 分支除了判 `isCangJie` 外还会
`ensurePluginsConfigured()` 并对 `isExplicit && !isCangJie` 上报 `Source entry is not a Cangjie file: <path>`。
伪代码把这个副作用抹平了，照抄会改变行为。

---

### A-4 【施工层错误】F5 落在生成代码里，且 `CfirChecker` 类型不存在

#### 事实 1：`CfirChecker` 在**本仓不存在**

实测（`grep -rln "CfirChecker" --include=*.kt cfir analysis compiler`，排除 `bin/`、`build/`）只有两个文件命中，
且都不是类型定义，而是注释与生成器内部说明：
- `cfir/checkers/checkers-component-generator/src/.../Generator.kt:191-193`（注释）
- `cfir/resolve/src/.../ResolutionStage.kt:10`（KDoc 里提到 `CfirCheckerSink`）

真实的 checker 基类是 **4 个相互独立的 `abstract class`**：

| 基类 | 路径 |
|---|---|
| `CfirDeclarationChecker<D : CfirDeclaration>` | `cfir/checkers/src/.../checkers/declaration/CfirDeclarationChecker.kt:13` |
| `CfirExpressionChecker<E : CfirStatement>` | `cfir/checkers/src/.../checkers/expression/CfirExpressionChecker.kt:8` |
| `CfirTypeChecker<T : CfirTypeRef>` | `cfir/checkers/src/.../checkers/type/CfirTypeChecker.kt:8` |
| `CfirLanguageVersionSettingsChecker` | `cfir/checkers/src/.../checkers/config/CfirLanguageVersionSettingsChecker.kt:7` |

⇒ 4.4.5 / 8.3-F5 给出的 `interface CfirChecker { val requiresImplementation: Boolean get() = false }` 无法编译。
（好在 A 表 12 个 checker **全部是 declaration checker**，所以把属性加到 `CfirDeclarationChecker<D>` 上即可覆盖；但这必须写清楚。）

#### 事实 2：唯一的"分派处"是**生成文件**

唯一的分派迭代点（`cfir/checkers/gen/org/cangnova/cangjie/cfir/analysis/checkers/declaration/DeclarationCheckersDiagnosticComponent.kt` 末尾）：

```kotlin
private inline fun <reified E : CfirDeclaration> Array<CfirDeclarationChecker<E>>.check(
    element: E,
    context: CheckerContext
) {
    for (checker in this) {
        try {
            context(context, reporter) { checker.check(element) }
        } catch (e: Exception) { ... }
    }
}
```

该文件第 15-18 行：

```
/*
 * 本文件由生成器自动生成
 * 请勿手动修改
 */
```

生成来源：`cfir/checkers/checkers-component-generator`，由 `cfir/checkers/build.gradle.kts` 的
`generatedDiagnosticContainersAndCheckerComponents()` 挂载。

⇒ 文档所说的「分派处统一过滤（**唯一的豁免开关**）：`if (configuration.compileCjd && checker.requiresImplementation) return`」
必须落在**生成器模板**（`checkers-component-generator`）或**基类的模板方法**里，**不能手改生成物**。
文档全文未出现 `checkers-component-generator`，且 7.4-C 的核对项"分派处过滤生效"会把施工者引向一个写着"请勿手动修改"的文件。

#### 事实 3：`DeclarationCheckers` 的路径也写错了

文档 4.4.5 / 8.3-F5 引用 `cfir/checkers/src/.../declaration/DeclarationCheckers.kt:13`。
实测该文件**不在 `src/`**，而在 `cfir/checkers/gen/org/cangnova/cangjie/cfir/analysis/checkers/declaration/DeclarationCheckers.kt`，
且是生成物（19 个槽位 + `EMPTY` 伴生对象 + 每个槽位的 `allXxxCheckers` 展开数组）。
文档"槽位全部按声明种类划分、注册是集合粒度"的判断是对的，但**取证路径错**，且未意识到它同样是生成物。

---

## 2. 需要补正项

### B-1 `ParsingContext` 统计错误（D2 / F2 / F3 的核心论据）

文档 4.2.3 理由 1、8.3-F2、8.6 表、5.2-C5、7.4-F 一致声称：

> 「`ParsingContext` 有 **20 个字段**、**14 个预设，实际只使用 2 个**；其余 **11 个**（…）**零使用点**」
> 「顺带清理（破坏性但正确）：**11 个死预设**应当删除」

实测 `psi/src/org/cangnova/cangjie/parsing/AbstractCangJieParsing.kt`：

| 项 | 文档 | 实测 |
|---|---|---|
| data class 主构造字段 | 20 | **15**（`disableMacroParsing` / `enableCustomAnnotation` / `allowParseAnnotationsInValueParameter` / `shouldReportError` / `allowLetExpression` / `parseTypeArguments` / `isParseOperator` / `isExpression` / `preferBlock` / `stopAtConditionOperatorInLetInitializer` / `suppressLiteralTrailingLambdaInLetCondition` / `collapse` / `isDoubleArrow` / `backToken` / `processStringInterpolation`）+ 类体 1 个 `expressionFirst` |
| 活跃预设 | 14 | **11**：`DEFAULT`(:158) `ANNOTATION_ONLY`(:161) `LEGACY`(:168) `REPORT`(:173) `SILENT`(:176) `IF_WHILE_CONDITION`(:180) `MATCH_EXPRESSION_MODE`(:183) `FUNCTION_LITERAL_BLOCK`(:186) `FUNCTION_LITERAL_COLLAPSED`(:189) `MACRO_BACK_TOKEN`(:192) `NO_STRING_INTERPOLATION`(:195)（`STRICT`(:165) 已注释） |
| `DEFAULT` 使用次数 | 6 | **6** ✓（`CangJieParser.kt:88/104/120/138/184` + `CangJieParsing.kt:1312`） |
| `ANNOTATION_ONLY` 使用次数 | 1 | **1** ✓（`CangJieParsing.kt:1282`） |
| **零使用点预设** | **11** | **9** |

并且文档自身算术不自洽：`14 − 2 = 12 ≠ 11`。

跨模块复核（`compiler` / `cfir` / `analysis` / `lsp` / `common` / `tests` 全量 grep `ParsingContext.`）**零命中**，
⇒ 「只在 psi 内使用、其余 9 个预设是死代码」这个**结论是对的**，只是数量错了。

**影响**：C-5 的独立提交目标"清理 11 个"应为 **9 个**；8.6 表的"保留 11 个未使用的预设"同样要改。
鉴于 D2/F2/F3 的全部论证都建立在这组统计上，建议把统计命令与输出直接写进文档（可复核），而不是只给结论。

### B-2 缺体诊断上报点的"穷举"不完整

文档 4.2.6 / 8.3-F1：本仓 **12 处**上报（A 组 7 + B 组 5），另有 **4 个排除点**（`:635`、`:3994`、`:4021`、`:4124`）。

实测（`grep -n "parsing.error.function.body.expected\|parsing.error.expecting.symbol" psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt`）：

| 消息 | 行号 | 数量 |
|---|---|---|
| `parsing.error.function.body.expected` | 3595, 3642, 3755, 3815, 4089, 4145, 4157 | **7** |
| `parsing.error.expecting.symbol "{"` | 635, 2838, 2879, **3004**, 3777, 3876, 3994, 4021, 4124, 4137 | **10** |
| 合计 | | **17** |

- A 组 7 与 B 组 5 的**行号与分组与实测完全一致** ✓（这一点做得很扎实）
- 排除清单**漏了 `:3004`** —— 它是 `parseEnumBody()` 里"缺 enum body 的 `{`"：

```kotlin
private fun parseEnumBody() {
    ...
    } else {
        error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "{"))   // :3004
    }
    body.done(ENUM_BODY)
}
```

**实际影响有限**：`:3004` 用的是 `expecting.symbol`，不会被错误地路由进 `reportMissingBody`，所以不会造成误豁免。
但 F1 的整个论点是"**不穷举就无法收敛为一个点**"，这处遗漏与"排除清单只有 4 条"的表述应当补正为 5 条。

### B-3 OP1 的爆炸半径枚举不准确（结论仍成立）

文档 OP1 / 8.7 条 1：

> 「`psi` 内 `getFileType()` 的覆写**只有这一处**（`CjFile.kt:66`）；另一处 `CangJieMacroCallFileType.kt:34` 是 **`FileType` 自身的接口实现**，与 `CjFile` 无关。」
> 「`CangJieFileType.INSTANCE` 的引用共 **6 处**：…」（随后列出了 7 个位置）

实测：

1. `psi/src/org/cangnova/cangjie/macro/file/CangJieMacroCallFileType.kt:34` **正是 `CjMacroCallFile.getFileType()` 的覆写**，返回 `CangJieMacroCallFileType`：

```kotlin
class CjMacroCallFile(private val provider: FileViewProvider) : CjFile(provider, isCompiled = true) {
    override fun getFileType(): FileType {
        return CangJieMacroCallFileType          // ← 与 CjFile.kt:66 完全同类
    }
}
```
   ⇒ 覆写共 **2 处**，不是 1 处。
   **但结论不变**：`CjMacroCallFile` 自己覆写了，不受基类改动影响。
   **而且这处恰好是支持 OP1 的先例**——"子类返回自己真实的 `FileType`"在本仓已有实现，是 OP1 决策的现成正向论据，文档漏掉了。

2. `CangJieFileType.INSTANCE` 实测 **7 处**：`utils/virtualFileUtil.kt:39`、`psi/CangJieDeclarationUseScopePolicy.kt:47`、`psi/CjCodeFragment.kt:187/199/307`、`psi/CjFile.kt:66`、`psi/CjPsiFactory.kt:760`。
   文档 OP1 说"共 6 处"却列了 7 个；8.7 条 1 说"其余 **5 处**"却列了 6 个。两处计数口径不一。
   （事实判断本身正确：这 7 处**无一**读取 `PsiFile.getFileType()`——`virtualFileUtil` 自己查注册表，其余是构造 `LightVirtualFile` 时传参。）

### B-4 4.9.2 的 `createFile` 改动与 v3 的 4.2.2 冲突，且在 v3 下这一处不用改

文档 4.9.2 给出的目标代码：

```kotlin
return when (viewProvider.fileType) {
    is CangJieDeclarationFileType -> CjFile(viewProvider, isCompiled = false, isDeclarationFile = true)  // ← v2 参数
    is CangJieFileType -> CjFile(viewProvider, false)
    else -> PsiPlainTextFileImpl(viewProvider)
}
```

但 v3 的 4.2.2 定义里 `CjFile` **没有** `isDeclarationFile` 构造参数：

```kotlin
open class CjFile(viewProvider: FileViewProvider, isCompiled: Boolean = false, val isCodeFragment: Boolean = false)
    : CjCommonFile(viewProvider, isCompiled), CjSourceKindCarrier {
    override val sourceKind: CjSourceKind
        get() = if (viewProvider.fileType is CangJieDeclarationFileType) CjSourceKind.DECLARATION else CjSourceKind.SOURCE
    val isDeclarationFile: Boolean get() = sourceKind.isDeclaration       // 派生属性，不可赋值
}
```

两个后果：

1. **代码无法编译**（`isDeclarationFile` 是只读派生属性，不是构造参数）。
2. **在 v3 下 `createFile` 根本不需要改**：`sourceKind` 从 `viewProvider.fileType` 推导，
   而 `CangJieDeclarationFileType` 继承 `CangJieFileType`，`.cj.d` 会被 `is CangJieFileType ->` 接住并创建普通 `CjFile`，
   该 `CjFile` 读 `viewProvider.fileType` 即得 `DECLARATION`。**"声明分支必须放在前面"这个顺序要求也随之消失**（它是 v2 的布尔构造参数方案才有的约束）。

连带需要重写的：4.9.2 的"v2 更正"整段（描述的是"会得到 `isDeclarationFile = false` 的普通 CjFile"这一 v2 世界）、
5.1-P5 的验收项、**7.4-E 的前 3 条**（"声明分支位于 `is CangJieFileType` 之前"在 v3 下是有害的误导）、6.6 相关断言。

> 顺带确认：`CjFile` 的构造 / 子类点确实是 **3 处** ✓（`CangJieMacroCallFileType.kt:20`、`CjCodeFragment.kt:53`、`CangJieParserDefinition.kt:110`），
> `createFile` 实测在 `CangJieParserDefinition.kt:106-113` ✓，`CjFile` 定义在 `psi/CjFile.kt:392` ✓。

---

## 3. 建议澄清项（设计层面，非事实错误）

### C-1 `CangJieParser.parse` 里"是否宏调用文件"构成了两个真源

4.2.4 给出的 v3 目标代码保留了独立判定：

```kotlin
if (psiFile is CjMacroCallFile || psiFile.name.endsWith(".cj.macrocall")) {
    cjParsing.parseOnlyAnnotationFile()
} else {
    cjParsing.parseFile()
}
```

而 4.2.5 的 `fileParsingContext()` 又按 `sourceKind` 把 `MACRO_CALL` 映射到 `ANNOTATION_ONLY`。
⇒ "这个文件是宏调用"同时有 **`CjSourceKind.MACRO_CALL`（由 `CjMacroCallFile` 覆写）** 与 **`name.endsWith(".cj.macrocall")`** 两个来源，
正是文档 J2 判据的反面信号。既然 4.1.1 已经为"消除 `when` 缺口"引入了第三个枚举值，入口处就应当据此收敛：

```kotlin
when (sourceKind) {
    CjSourceKind.MACRO_CALL -> cjParsing.parseOnlyAnnotationFile()
    else -> cjParsing.parseFile()          // 模式的差异已由构造属性承载
}
```

否则 v3 主张的"模式由文件种类派生、编译器帮你保证穷尽性"在入口处仍留一个手工分支。

### C-2 "`.cj` 与 `.cj.d` 在同一 module 并存"是官方不存在的模式

官方 R1 是"两模式**互斥输入**"：`-d` 时 `.cj` 落到 `HandleNoExtension`（`Option.cpp:744/750/753`），非 `-d` 时 `.cj.d` 不被收。
但本仓的 IDE/LSP 场景里，`.cj.d`（SDK 头文件）与 `.cj`（源码）**天然共存于同一模块**。

文档 4.4.1 只定义了**收集**层面的互斥（由 `compileCjd` 决定收哪种），但没有定义**同一分析会话里两种文件同时可见时，谁提供权威声明**：

- D7 说 `.cj.d` 不进声明提供者聚合 ⇒ 权威来自 `.cjo`；
- DIFF-3 说 `.cj.d` 仍建 Stub 索引 ⇒ 它又确实参与符号体系；
- 4.9.3 的处置是把源码根下的 `*.cj.d` **排除**出 Stub 索引。

请补一条验收：**被排除出索引的 `.cj.d`，在 IDE 中打开、编辑、补全、跨文件跳转时是否仍然正常工作？**
6.6 目前只断言了"不出现在项目 Stub 索引符号中"，没有断言"打开后能力不退化"。这两件事不等价。

### C-3 两条 `.cjo → 声明` 路径的注解一致性

4.8.1 承认本仓有两条并行路径（`cfir/cfir-serialization` 与 `analysis/decompiled`），处置是"复用同一个 `CjdSidecarLocator` + `DeclarationMatchKey`"。
但 6.4 的生命周期矩阵只覆盖了**跨 provider**（"同一 `.cjo` 被两个 provider 加载"），
**没有覆盖同一分析会话内两条路径各自物化同一 `.cjo`** 的情况。

D5 选择了"写回 `annotation.annotations`（而非读取侧 union）"，其直接后果就是：**物化次数决定了注解被追加的次数**。
请明确二选一并在测试中固化：
- 两条路径产出**同一个**声明对象（则 4.6.6 的证据链需要扩展说明覆盖到两条路径）；
- 两条路径产出**两份**对象（则补一条"两份对象的注解集合逐项相等"的用例，防止两条路径分叉）。

### C-4 `DIFF-5` 的风险缓解尚未验证

官方在 sidecar 路径上对 `.cj.d` **确实**跑宏展开（`CompileStrategy.cpp:311-323`，全程 `compileCjd = true`），
并且这个开关被透传到 7 处宏实参重解析 / 重解析点（`MacroEvaluation.cpp:296/310/365/387/562/834/866`、`MacroExpansion.cpp:364`，我已逐条核对）。
本仓选择不做（`DIFF-5`），缓解论证是"`@APILevel` / `@SysCap` / `@Hide` 写在宏调用之外的显式声明上"。

但文档 C.4 自己把"真实 `.cjo` + `.cj.d` fixture"列为待验证项 ⇒ **这条缓解论证尚未对任何真实 SDK 样本验证过**。
建议 P4 开工前先用一份真实 SDK 的 `.cj.d` 做统计（含宏调用的顶层/成员声明占比、注解是否落在宏调用之外），
用数据决定 `DIFF-5` 是否可接受，而不是先按假设实施。

---

## 4. 复核通过项清单

以下断言经逐条回到源码核实，**全部准确**，可直接作为施工依据。

**官方侧（`external/cangjie_compiler`）**

| 断言 | 取证 | 结论 |
|---|---|---|
| R3 / P6：`.cj.d` 无体函数不打 `ABSTRACT`、不报缺体 | `ParseDecl.cpp:1548-1562`（`isMember && !parseDeclFile`） | ✓ 逐字准确 |
| R4 / P4：`.cj.d` 无 `{}` 类成员属性**仍**打 `ABSTRACT`；`DiagMissingPropertyBody` 被抑制 | `Parser.cpp:369-381` + `:396-406` | ✓（并补充：该诊断在 `.cj` 侧还会置 `HAS_BROKEN`，`.cj.d` 侧一并跳过） |
| P1 / P2 / P3 | `ParserDiag.cpp:1218 / 1086 / 1113` | ✓ 行号与语义准确 |
| P5：finalizer / constructor 无体不打 `HAS_BROKEN` | `ParseDecl.cpp:539 / 582` | ✓ |
| `ParseFuncBody` 无 `{` 时仍构造 `FuncBody`，`body` 为空 | `ParseDecl.cpp:1694-1711` | ✓ |
| R5：声明模式只写 `.cjo`，跳过 desugar / 泛型实例化 / 溢出策略 / CHIR / codegen，`PerformMangling` 沿用基类 | `CjdCompilerInstance.h:22-63` | ✓ 逐字准确 |
| R6：Sema 6 类豁免 | `DeclAttributeChecker.cpp:289 / 334`、`InitializationChecker.cpp:503 / 1517`、`TypeChecker.cpp:2220`、`StructInheritanceChecker.cpp:839` | ✓ 6 处全部存在，语义与描述相符 |
| R2：字面后缀匹配 + 要求非空文件名 | `FileUtil.cpp:230-245` | ✓ 逐字准确 |
| R1：`.cj` / `.cj.d` 输入互斥 | `Option.cpp:739-755` | ✓ 逐字准确 |
| R8：`rfind(".cjo")` 截断路径（`DIFF-4` 的官方缺陷） | `ImportManager.cpp:289-295` | ✓ 成立 |
| R9 / R10 / R11：读不到静默跳过、跑 Parse+MacroExpand 不跑 Sema、局部 Package 不注册 | `CompileStrategy.cpp:300-332` | ✓ 逐字准确 |
| R12 / R13：只搬 annotations、方向 `.cj.d → .cjo`、只匹配导出声明、跳过 `MAIN_DECL`/`BUILTIN_DECL` | `MergeAnnoFromCjd.cpp:477-571` | ✓ 逐字准确 |
| `DIFF-1`：内层循环用外层约束数作上界 ⇒ 越界 | `MergeAnnoFromCjd.cpp:396`（`for (size_t j = 0; j < l->generic->genericConstraints.size(); ++j)`） | ✓ **成立，高价值发现** |
| `Rune → UInt8` 归一 | `MergeAnnoFromCjd.cpp:147-148` | ✓ |
| 类型别名导致匹配失败（已知限制） | `MergeAnnoFromCjd.cpp:169` | ✓ |
| prop 在声明模式下不写 setter/getter 索引 | `ASTWriter.cpp:916-935` | ✓ |
| `-d` → `COMPILE_CJD` → `compileCjd` | `Options.inc:246`、`FrontendOptions.cpp:46` | ✓ |
| `parseDeclFile` 文档语义 | `Parser.h:73-86` | ✓ 逐字准确 |
| 合并结果在 APILevel 检查后被清除 | `CheckAPILevel.cpp:284-304, 708-709` | ✓（文档描述与此一致） |

**本仓侧**

| 断言 | 取证 | 结论 |
|---|---|---|
| `CjSourceFile` 只有 `name` / `path` / `getContentsAsStream()`，无种类概念 | `common/src/org/cangnova/cangjie/CjSourceFile.kt:16-24` | ✓ |
| `CjSourceFile` 有 4 个实现类 | 同文件 `:30 / :57 / :84 / :111` | ✓ |
| `CangJieFileType` 是 `open class`，`EXTENSION="cj"`，`getName()==displayName` | `psi/src/.../lang/CangJieFileType.kt:10,23,32,37-40` | ✓（4.2.1"必须覆写 `getName()`"的推理成立） |
| 本仓 `psi` 无 `HAS_BROKEN` 概念 | 全量 grep 无命中 | ✓（P5 = N/A） |
| 解析入口 `parse(builder, psiFile)` 已是文件感知，两分支 | `psi/.../parsing/CangJieParser.kt:147-172` | ✓ |
| `CjFileElementType.doParseContents` 传 `psi.containingFile` | `psi/.../stubs/elements/CjFileElementType.kt:92-98` | ✓ |
| `parseFile()` / `parseOnlyAnnotationFile()` 除硬编码 `with` 外逐字相同 | `CangJieParsing.kt:1280-1327` | ✓ |
| `CangJieLightParser.parseWith` 无上下文透传；`reportErrors` 扫 `ERROR_ELEMENT` | `psi/.../parsing/CangJieLightParser.kt:46 / 59-76 / 91-108` | ✓（v2 的"硬阻塞"判断正确，v3 的消解方案正确） |
| `CjFile` 构造点全仓仅 3 处 | `CangJieMacroCallFileType.kt:20`、`CjCodeFragment.kt:53`、`CangJieParserDefinition.kt:110` | ✓ |
| `getFileType()` 硬编码在 `CjCommonFile`（非 `CjFile`） | `psi/.../psi/CjFile.kt:66` | ✓ |
| PSI 侧函数/属性隐式 abstract 同在一个 `when` | `PsiRawCfirBuilder.kt:3887-3899` | ✓（"护栏只加函数分支"的告警必要且正确） |
| LightTree 侧已天然拆成两个独立函数 | `LightTreeRawCfirDeclarationBuilder.kt:3287-3293`（函数）/ `:3300-3308`（属性） | ✓ |
| `CangJieLightParser` 另有 `parseAnnotationOnly` 入口 | `CangJieLightParser.kt:55-61` | ✓ |
| `deserializeDecl` 的行序（`:132/147/152-153/154/155-167/168`）与合并点约束 | `CfirDeclDeserializer.kt:131-175` | ✓ 行号准确 |
| `declCache` 一包一 context、provider 级 `contextCache`、每次新建 context | `CfirDeserializationContext.kt:30`、`AbstractCfirDeserializedSymbolProvider.kt:56, 173-188, 397` | ✓ 证据链成立 |
| `analysis/decompiled/decompiler-to-stubs` 已依赖 `cfir-serialization` | `build.gradle.kts:14` | ✓（4.8.1 的"无需新增模块依赖"成立） |
| `deveco` 与 `intellij-ide` 的收集器副本正文一致 | `diff` 比对 `:100-112` 完全一致 | ✓（OP3 判断成立） |
| `virtualFileUtil.isCangJieFileType()` 两条判定且对 `.cj.d` 为 false | `psi/.../utils/virtualFileUtil.kt:34-40` | ✓ 结论成立 |
| IDE `cangjie-filetypes.xml` 仅 2 条注册（`CangJie` / `cjo`） | `intellij-ide/.../META-INF/cangjie-filetypes.xml`（14 行） | ✓（4.9.1"源码中无 `cjd` 草稿"成立） |
| `GroupedCjSources` 现状把 `.cj.d` 拒绝（`extension=="d"` 落 else） | `GroupedCjSources.kt:118-138` | ✓ 前提成立（但改法见 A-3） |
| `parsing.error.expecting.identifier` 键存在 | `CangJieParsingBundle.properties:118` | ✓（8.3-F1 的示例代码可直接使用） |
| `parsing.error.field.requires.type.or.initializer` 调用点 | `CangJieParsing.kt:2297` | ✓ |

未发现 `external/` 下任何取证错误 —— 13 处官方取证点全部命中且语义无误，这一点值得肯定。

---

## 5. 对方案的整体评价

### 优点

1. **取证密度高、可复核性强**。第 1 章的每一条语义断言都给了 `文件:行`，且经得起逐条回查；
   本仓侧同样如此。这在本仓现存的三份设计稿里是唯一做到的。
2. **`DIFF-*` 显式登记**的机制很好。`DIFF-1` 是一处真实的官方越界 UB，属于高质量发现；
   `DIFF-2`（确定性匹配替代 `unordered_map + find_if`）的方向也正确——我已确认官方两处都用 `std::unordered_map` 且迭代顺序不确定。
3. **8.5 / 8.7 两张"不要动"清单**把"设计上不建议破坏"与"施工时不要顺手改"分开，切中本仓此前多次误伤的痛点。
4. **框架正确性判据 J1–J5 与 F6 的自否**（主动否决自己的"抽公共类"直觉）表明这套判据没有被机械套用。

### 主要短板

**共同根因：v3 在 v2 的基础上做了大幅框架改写，但第 4 章只做了一半的同步。**

具体表现：
- 4.2.3（v3）说"不新增 `DECLARATION_FILE`"，4.2.5 是 v2 残留（A-2）；
- 4.2.2（v3）删掉了布尔构造参数，4.9.2 / 5.1-P5 / 7.4-E 是 v2 残留（B-4）；
- 4.4.5 / 8.3-F5（v3）是**重写**的框架级方案，但从未与本仓真实的 checker 框架对照过（A-4）；
- 8.3-F2 的 `F3` 结论（"不放进 `ParsingContext`"）与 4.2.5 的实际代码互相矛盾（A-2）。

**建议在实施前做一次"v3 一致性回扫"**：把所有出现 `isDeclarationFile =`（构造参数形式）、`ParsingContext.DECLARATION_FILE`、
`parseDeclarationFile()`、`CfirChecker` 的位置全部列出来逐处判定"是 v3 还是 v2 残留"。
按 A-2 / A-4 / B-4 三处的模式看，很可能还有未被本报告覆盖的残留（本报告只覆盖了我实际 grep 到的部分）。

### 与官方语义的差距（本报告新增 / 修正的部分）

| 项 | 文档 | 本报告 |
|---|---|---|
| 声明模式下函数获得的属性位 | "不含 `ABSTRACT`/`HAS_BROKEN`" | 还需补一条：**interface 作用域下会获得 `DEFAULT`**（A-1） |
| 总差异条数 | R1–R14 共 14 条红线 | 14 条均成立；但 1.3 的"抑制点穷举"与 1.3.2/1.3.5 的属性位描述不完整（A-1） |
| 护栏 2 | "确认不改，加护栏只是噪声" | 该结论在"对齐官方"目标下不成立，需显式决策（A-1） |

---

## 6. 建议的处置顺序

1. **先做 A-1 的决策**（复刻 `DEFAULT` 还是登记 `DIFF-6`）——它决定 4.5.2 护栏 2、6.2 的 P2 用例、7.4-C 的核对项怎么写。
2. **修 A-2 / A-4 / B-4**（三处 v2/v3 残留与施工层错误）——它们会直接导致施工者改错文件或写出无法编译的代码。
3. **修 A-3**（`convertToSourceFiles` 条件）——否则 P3 的源收集改动是一个失效的修复。
4. **补正 B-1 / B-2 / B-3** 的数字与清单——不阻断，但会让"实测"承诺失守。
5. **澄清 C-1 ~ C-4**，其中 C-1 / C-2 建议写进设计稿正文，C-3 / C-4 建议转成 P4 的准入条件。
6. 最后做一次**全文一致性回扫**（见第 5 节）。
