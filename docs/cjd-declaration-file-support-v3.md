# `.cj.d` 声明文件支持设计

> 状态：**设计稿 v3 定稿（已通过复核并修订，可进入施工）**
> 适用范围：主仓库（`common` / `psi` / `compiler` / `cfir` / `analysis` / `lsp`）+ `intellij-ide`
> 证据基线：`external/cangjie_compiler`（官方实现镜像，只读） + 本仓当前工作树
> 复核记录：[`cjd-declaration-file-support-v3-review.md`](./cjd-declaration-file-support-v3-review.md)（2026-09-16）
>
> **v3 的立场**：本稿不再采用"在既有框架上逐点加条件"（纯增量）的路线。
> 按 [3.1.1](#311-首要原则框架正确性优先于改动最小化) 的 5 条框架正确性判据重新设计后，
> 需要**逐点人工判断**的位置从 17 处降到 1 处、Sema 豁免点从 12 处降到 1 处、"文件种类"的事实来源从 4 处降到 1 处。
> 完整的评估过程、破坏性改动清单与"不建议改动"清单见 [第 8 章](#8-框架正确性评估与破坏性改动索引)；
> 版本演进与旧稿结论更正见 [附录 C](#附录-c版本变更记录)。
>
> **本稿有 5 处破坏性改动，其中仅 1 处（5 条错误文案）有外部可见行为变化**，
> 提交切分与风险控制见 [5.2](#52-破坏性改动的提交切分)。
>
> ⚠️ **v3.1 修订（2026-09-16，基于复核报告）**：本稿在送审后收到一份逐条回查的复核报告，
> 其中 1 处**框架级遗漏**（官方在声明模式下会为 interface 无体函数合成 `Attribute::DEFAULT`）、
> 3 处**施工阻断级错误**（不存在的 `CfirChecker`、落在生成代码里的"分派处"、失效的源收集改法）、
> 以及若干计数与路径错误。**全部已修入正文**，逐条记录见 [C.5](#c5-v3--v31复核报告的处置)。
> 复核同时确认：R1–R14 语义红线、13 处官方取证、`DIFF-1`（官方越界 UB）**逐条成立**。
>
> **行号约定**：本文行号以本仓工作树快照（2026-09-16）为准。行号仅作定位辅助，
> 施工与评审时应以「符号名 + 语义锚点」为准；若代码已变动，以符号名为唯一依据。

---

## 0. 文档定位

本文回答一个问题：**如何在不破坏本仓现有架构的前提下，按官方仓颉编译器的语义，完整支持 `.cj.d` 声明文件。**

本文的每一条语义断言都标注了官方源码取证位置（`文件:行`）。设计决策部分标注了本仓取证位置。凡属"官方实现存在缺陷、本仓有意采用更严格实现"的地方，一律显式标注 `DIFF-*`，不允许静默偏离。

本文不涉及实现排期与人力，仅给出可供施工的规格。

---

## 1. 官方编译器语义基线

### 1.1 术语：`.cj.d` 的两种形态必须分开

`.cj.d` 在官方编译器中出现在**两个完全不同的场景**，共用同一个"声明模式解析器"，但流程、产物、生命周期完全不同：

| | 形态 A：声明文件独立编译 | 形态 B：声明伴随合并（sidecar merge） |
|---|---|---|
| 触发方式 | 命令行显式传 `.cj.d` 文件 | 普通编译时，依赖包 `.cjo` 同目录存在同名 `.cj.d` |
| 命令行 | `cjc -d a.cj.d ...` | `cjc a.cj`（无额外开关） |
| 官方开关 | `-d` → `Options::ID::COMPILE_CJD` → `globalOptions.compileCjd = true` | 运行期在 `ParseAndMacroExpandCjd()` 内临时置 `compileCjd = true`，用完立即置回 `false` |
| 产物 | `<pkg>.cjo`（**只写 cjo，不写 bchir / 无 CHIR / 无机器码**） | 无产物；注解被搬进已加载的 `.cjo` AST |
| 是否跑 Sema | 是（带 6 类豁免） | **否**（只 Parse + MacroExpand + 合并） |
| `.cj.d` 的 package 是否进入 importManager | 是（它就是本次编译的源包） | **否**（局部 `OwnedPtr<Package>`，用完即弃） |
| 取证 | `Options.inc:245-247`、`FrontendOptions.cpp:46`、`CjdCompilerInstance.h:22-63` | `CompileStrategy.cpp:293-332` |

> **最重要的一条边界**：形态 B 中 `.cj.d` **绝不作为第二份库声明**进入符号体系。官方为它单独 `MakeOwned<Package>` 一个**局部**包，既不注册进 `ImportManager`，也不写盘（`CompileStrategy.cpp:318-330`）。它存在的唯一目的是把注解搬到 `.cjo` 的声明上。

---

### 1.2 形态 A：独立声明编译的精确流程

#### 1.2.1 输入分类规则

官方在 `GlobalOptions::ProcessInputs` 用一张真值表决定输入文件归属（`Option.cpp:733-755`）：

```cpp
std::string ext = GetFileExtension(value);
if (ext == OBJECT_EXTENSION || ext == ARCHIVE_EXTENSION) { ... }
else if (ext == CJ_EXTENSION && !compileCjd) { HandleCJExtension(...); }      // .cj 仅在非声明模式收
else if (ext == BC_EXTENSION)      { ... }
else if (ext == CJO_EXTENSION)     { ... }
else if (HasCJDExtension(value) && compileCjd) { HandleCJDExtension(...); }   // .cj.d 仅在声明模式收
else { HandleNoExtension(...); }
```

⇒ **`.cj` 与 `.cj.d` 输入互斥**。声明模式编译时，`.cj` 会落到 `HandleNoExtension` 分支被当作未识别输入。

#### 1.2.2 后缀判定规则

```cpp
// FileUtil.cpp:230-245
bool HasCJDExtension(std::string_view path)
{
    auto fileIt = path.rbegin();
    auto fileEnd = path.rend();
    auto extensionIt = CJ_D_FILE_EXTENSION.rbegin();   // ".cj.d" 的反向迭代
    auto extensionEnd = CJ_D_FILE_EXTENSION.rend();
    while (fileIt != fileEnd && extensionIt != extensionEnd) {
        if (*fileIt != *extensionIt) { return false; }
        ++fileIt; ++extensionIt;
    }
    // exclude .cj.d file with no filename
    return extensionIt == extensionEnd && fileIt != fileEnd;
}
```

⇒ 判定是**字面后缀匹配**（区分大小写），且要求 `.cj.d` 之前有非空文件名（`.cj.d` 本身不算）。常量 `CJ_D_FILE_EXTENSION = ".cj.d"`（`ConstantsUtils.h:45`）。

> 注意：这**不是**"扩展名等于 `d`"。`VirtualFile.extension` 对 `foo.cj.d` 返回 `"d"`，两者语义不同，本仓实现时不能混用。

#### 1.2.3 编译流水线裁剪

官方为声明模式单独提供 `CjdCompilerInstance`（`CjdCompilerInstance.h:22-63`），从 `FrontendTool.cpp:200-206` 分流：

```cpp
if (NeedCreateIncrementalCompilerInstance(globalOptions)) { IncrementalCompilerInstance }
else if (globalOptions.compileCjd) { CjdCompilerInstance }
else { DefaultCompilerInstance }
```

`CjdCompilerInstance` 的精确裁剪：

```cpp
CjdCompilerInstance(...) : DefaultCompilerInstance(...) { buildTrie = false; }

bool PerformDesugarAfterSema()     override { return true; }   // 跳过
bool PerformGenericInstantiation() override { return true; }   // 跳过
bool PerformOverflowStrategy()     override { return true; }   // 跳过
bool PerformCHIRCompilation()      override { return true; }   // 跳过
bool PerformCodeGen()              override { return true; }   // 跳过
bool PerformCjoAndBchirSaving()    override {                // 只写 cjo
    for (auto& srcPkg : GetSourcePackages()) { ret = ret && SaveCjo(*srcPkg); }
}
// PerformMangling 沿用基类（mangled name 属于 ABI，必须算）
```

下游串接（`FrontendTool.cpp:128-154`）：

```cpp
if (!instance.Compile(CompileStage::CHIR)) { return false; }
res = instance.PerformCodeGen() && res;              // Cjd → 恒 true
res = instance.PerformCjoAndBchirSaving() && res;    // Cjd → 只 SaveCjo
```

⇒ 声明模式下实际执行的是：**Parse → ImportPackages → MacroExpand → Sema → Mangling → SaveCjo**。

#### 1.2.4 其它 `compileCjd` 分支点（形态 A 专有）

| 位置 | 行为 |
|---|---|
| `CompilerInstance.cpp:154` `IsNeedSaveIncrCompilationLogFile` | `compileCjd` → 返回 false，不写增量编译日志 |
| `CompilerInstance.cpp:330` `NeedUpdateAndWriteCachedInfoToDisk` | `compileCjd` → 返回 false，不写缓存 |
| `CompilerInstance.cpp:449` `PerformIncrementalScopeAnalysis` | `compileCjd` → 直接 `return true`，跳过 AST Diff |
| `ImportManager.cpp:223 / 243 / 258` | 构造 `ASTWriter` 时带上 `opts.compileCjd` |
| `ASTWriter.cpp:915-935` `SavePropDecl` | `config.compileCjd == true` 时**不写 setter/getter 索引**（丢弃访问器实体引用） |
| `MacroEvaluation.cpp:296/310/365/387/562/834/866` | 宏实参重解析与宏可求值判定也传 `compileCjd` |
| `MacroExpansion.cpp:364` | 宏展开后的重解析同样带 `compileCjd` |

⇒ **宏系统必须同样感知声明模式**，否则含宏的 `.cj.d` 会在宏展开阶段报"缺函数体"。

#### 1.2.5 Sema 的 6 类豁免

声明模式下 Sema **整体照跑**，只在以下 6 处豁免：

| 位置 | 豁免内容 | 保留的检查 |
|---|---|---|
| `DeclAttributeChecker.cpp:284-291` | `CheckAttributesForPropAndFuncDeclInClass` 整体 return：不检查「非抽象类中成员被标 abstract」、「abstract 成员可见性」、「open 成员可见性」、「open 类中 finalizer」 | — |
| `DeclAttributeChecker.cpp:332-336` | `CheckPropDeclAttributes`：`pd.TestAttr(ABSTRACT) \|\| opts.compileCjd` 时 return，不要求 `var` 属性同时具备 getter/setter | — |
| `InitializationChecker.cpp:503` | 跳过 `INITIALIZED` 属性检查 | — |
| `InitializationChecker.cpp:1517` | 构造函数初始化检查提前返回 | — |
| `TypeChecker.cpp:2215-2230` | `CheckWhetherHasProgramEntry`：跳过 `main` 入口缺失诊断 | 语法 / import / 类型 / 泛型约束 / 可见性 / 继承与 override / 注解参数 / ABI 与签名检查**全部保留** |
| `StructInheritanceChecker.cpp:830-842` | `DiagnoseForUnimplementedInterfaces` 中 `opts.compileCjd` 的成员视为 `ignored`，不报"未实现接口成员" | 同上 |

---

### 1.3 形态 A 的解析语义（`.cj.d` 独有的语法宽容度）

官方 `Parser` 的声明模式开关叫 `parseDeclFile`（`Parser.h:73-86`）：

```cpp
/// \param parseDeclFile true if this parser is used to parse .cj.d file. When parsing these files, do not report
/// an error if the func is missing its body.
```

它是一个**纯诊断抑制开关**，一共只作用于 6 个点。除此之外，`.cj.d` 与 `.cj` 走**完全相同的词法、语法与 PSI 结构**（同一个 `Parser`、同一个 `ParserImpl`）。

#### 1.3.1 抑制点清单（穷举）

| # | 位置 | 抑制的行为 |
|---|---|---|
| P1 | `ParserDiag.cpp:1216-1225` `DiagMissingBody` | 函数 / 构造函数 / finalizer / 属性访问器缺函数体时，不报 `parse_missing_body` |
| P2 | `ParserDiag.cpp:1084-1091` `DiagConstVariableExpectedInitializer` | `const` 变量缺初始化不报错 |
| P3 | `ParserDiag.cpp:1111-1118` `DiagExpectedInitializerForToplevelVar` | 顶层 `var`/`let` 缺初始化不报错 |
| P4 | `Parser.cpp:396-406` `DiagMissingPropertyBody` | 属性缺 `{}` 不报 `parse_expected_character` |
| P5 | `ParseDecl.cpp:537-542` / `580-585` | finalizer / constructor 无体时**不打 `Attribute::HAS_BROKEN`** |
| P6 | `ParseDecl.cpp:1533-1562` `CheckFuncBody` | 整块跳过（见 1.3.2） |

#### 1.3.2 P6 是本设计最关键的一条：声明函数**不是**抽象函数

```cpp
// ParseDecl.cpp:1548-1562
const bool isMember = fb && !fb->body && !decl.TestAttr(Attribute::FOREIGN) &&
    !decl.TestAttr(Attribute::INTRINSIC) && scopeKind != ScopeKind::UNKNOWN_SCOPE;
if (isMember && !parseDeclFile) {
    if (scopeKind == ScopeKind::CLASS_BODY || scopeKind == ScopeKind::INTERFACE_BODY) {
        decl.EnableAttr(Attribute::ABSTRACT);                          // ← 只有 .cj 才打
        if (!decl.funcBody->retType && !fb->TestAttr(Attribute::HAS_BROKEN)) {
            ParseDiagnoseRefactor(parse_abstract_func_must_have_return_type, lastToken.End());
        }
    } else {
        if (!fb->TestAttr(Attribute::HAS_BROKEN)) {
            DiagMissingBody("function", ...);
        }
    }
}
```

`(isMember && !parseDeclFile)` 这个合取意味着：**`.cj.d` 中无函数体的类成员函数，不打 `ABSTRACT`、也不报错。**

`FuncDecl` 因此存在**第四种状态**：

| 状态 | 有无 body | `ABSTRACT` | `DEFAULT` | 语言是否接受 | 出现场景 |
|---|---|---|---|---|---|
| 实现 | 有 | 否 | **是**（仅 interface 体内） | 是 | `.cj` |
| 纯虚 | 无 | **是** | 否 | 是 | `.cj` 的 class / interface 成员 |
| 非法 | 无 | 否 | 否 | **否**（报错） | `.cj` 的其它 scope |
| **声明** | 无 | **否** | **是**（仅 interface 体内） | 是 | **`.cj.d`** |

⇒ 把 `.cj.d` 的无体函数一律标成 `abstract` 是**语义错误**：`abstract` 会引出一整套继承/override/实例化约束（不可实例化、必须被重写、可见性限制），而 `.cj.d` 的声明对应的是**有真实实现的外部声明**。

> ⚠️ **上表的 `DEFAULT` 列是 v3 定稿新增的。** 初稿只列了 `ABSTRACT` 一维，并由此得出错误印象：
> 声明模式相对 `.cj` 是"**只少不多**"（少掉 `ABSTRACT` / `HAS_BROKEN`）。
> 实际官方在声明模式下**去掉一个属性的同时会新增另一个**——`interface` 体内的无体函数会获得 `Attribute::DEFAULT`。
> 这是一处**框架级遗漏**，单列于 [1.3.5](#135-p6-的连带后果interface-无体函数会获得-default)，并改写了 4.5.2 的护栏结论。

#### 1.3.3 属性（prop）是**例外**：仍会打 `ABSTRACT`

```cpp
// Parser.cpp:340-394  ParsePropDecl
if (Skip(TokenKind::LCURL)) {
    ParsePropBody(modifiers, *ret);
} else if (scopeKind == ScopeKind::CLASS_BODY || scopeKind == ScopeKind::INTERFACE_BODY ||
    scopeKind == ScopeKind::UNKNOWN_SCOPE) {
    ret->EnableAttr(Attribute::ABSTRACT);          // ← 不区分 parseDeclFile
    if (ret->type) { ret->end = ret->type->end; }
} else {
    DiagMissingPropertyBody(*ret);                 // ← 这里才被 P4 抑制
    ret->end = lastToken.End();
}
```

⇒ `.cj.d` 中 `class C { prop p: Int64 }` **仍然获得 `ABSTRACT`**。这不是官方疏漏——在仓颉里"无 `{}` 的类成员属性"本身就是抽象属性这一正常语义，`.cj.d` 只是抑制了它本该在非 class/interface 作用域下产生的诊断。

**这是函数与属性的语义不对称，本仓必须精确复刻，不能"统一处理"。**

#### 1.3.4 属性访问器

`ParsePropMemberBody` 尾部（`ParseDecl.cpp:352-354`）对无体访问器调用 `DiagMissingBody`（→ 被 P1 抑制）：

```cpp
if (!ret->funcBody->body) {
    DiagMissingBody(!ret->identifier.Valid() ? "" : "function", " '" + ret->identifier + "'", lastToken.End());
}
```

⇒ `.cj.d` 中 `prop p: Int64 { get() }`（getter 无体）合法。

#### 1.3.5 P6 的**连带后果**：`interface` 无体函数会获得 `DEFAULT`

官方 `SetDefaultFunc`（`ParseDecl.cpp:874-898`）**不带 `parseDeclFile` 门禁**——
全仓 `parseDeclFile` 分支点只有 7 处（`ParseDecl.cpp:539 / 582 / 1550`、`Parser.cpp:398`、`ParserDiag.cpp:1086 / 1113 / 1218`），**不含它**：

```cpp
void ParserImpl::SetDefaultFunc(ScopeKind scopeKind, AST::Decl& decl) const
{
    if (scopeKind != ScopeKind::INTERFACE_BODY) { return; }
    if (decl.astKind == ASTKind::FUNC_DECL) {
        bool defaultFunc = !decl.TestAttr(Attribute::FOREIGN) && !decl.TestAttr(Attribute::ABSTRACT);
        if (defaultFunc) { decl.EnableAttr(Attribute::DEFAULT); }   // ← .cj.d 下会命中
    } else if (decl.astKind == ASTKind::PROP_DECL) {
        if (decl.TestAttr(Attribute::ABSTRACT)) { return; }         // ← 属性被 R4 挡住
        decl.EnableAttr(Attribute::DEFAULT);
        for (auto& setter : pd->setters) { setter->EnableAttr(Attribute::DEFAULT); }
        for (auto& getter : pd->getters) { getter->EnableAttr(Attribute::DEFAULT); }
    }
}
```

两处调用点是 `ParseDecl.cpp:1529`（FuncDecl）与 `Parser.cpp:392`（PropDecl），**都排在各自的属性推断之后**：

```text
ParseFuncDecl:
  CheckFuncBody(scopeKind, *ret);   // :1518  ← .cj.d 下 !parseDeclFile 挡住 ABSTRACT
  ...
  SetDefaultFunc(scopeKind, *ret);  // :1529  ← 读到的 ABSTRACT 是"没被打上"的那个
```

⇒ 因果是**串联**的：P6 抑制了 `ABSTRACT` → `SetDefaultFunc` 判 `!ABSTRACT` 成立 → **`DEFAULT` 被置上**。
`.cj` 的 interface 无体函数得到 `ABSTRACT`，`.cj.d` 的同一写法得到 `DEFAULT` —— **两种模式下属性位集合完全不重叠**。

`.cj.d` 下的完整对照（这张表才是"声明模式到底改了什么"的完整答案）：

| 声明 | `ABSTRACT` | `DEFAULT` | 说明 |
|---|---|---|---|
| `interface I { func f(): Int64 }` | 否（P6） | **是** | 官方在此处把无体接口函数**假定为有默认实现** |
| `class C { func f(): Int64 }` | 否（P6） | 否 | `DEFAULT` 只在 `INTERFACE_BODY` 作用域置位 |
| `interface I { prop p: Int64 }` | **是**（R4，无门禁） | 否（被 `ABSTRACT` 挡住） | 属性路径**无差异** ✓ |
| `interface I { prop p: Int64 { get() } }` | 否（走了 `{` 分支） | **是**（prop 与全部 accessor） | 与 `.cj` 一致 |

**为什么必须复刻**：`DEFAULT` 不是展示性标记，它参与 ABI 与 Sema ——
`ASTWriter.cpp:1244`（参与 raw mangled name 判定）、`DefaultImplementHandler.cpp:362`、`TypeChecker.cpp:2554`、
`TypeCheckExtend.cpp:105`、`StructInheritanceChecker.cpp:776 / 825 / 895 / 914 / 1163`、`MergeInheritedMemberHelper.cpp:190`。
本仓的 `.cj.d` 要描述的是**已编译库**，属性位与官方不一致会在 `.cjo` / mangled name 层面产生事实错误。

**本仓现状**：两条路径的 `DEFAULT` 都由 `hasBody()` 派生，而不是"读 `ABSTRACT`"：

```kotlin
// psi2cfir: PsiRawCfirBuilder.kt:3902-3911
is CjNamedFunction -> !declaration.hasModifier(CjTokens.FOREIGN_KEYWORD) && declaration.hasBody()

// light-tree2cfir: LightTreeRawCfirDeclarationBuilder.kt:3245-3249
isInInterfaceMemberContext() && !modifiers.isForeign && !modifiers.isAbstract && hasSyntaxBody(node)
```

在 `.cj` 下 `hasBody()` 与 `!ABSTRACT` **恰好等价**（无体 ⇒ 隐式 abstract），所以这条差异长期不可见；
到 `.cj.d` 就暴露：两条路径都判"非 default"，与官方相反。

**修法（框架级，不是打补丁）**：**让 `isDefault` 与 `isImplicitAbstract` 同源。**

官方是**先定 `ABSTRACT`、再由它推出 `DEFAULT`**（同一函数体内顺序执行）。本仓却用两个独立谓词分别推导这一对事实 ⇒
一旦二者的"等价前提"（`.cj` 无体 ⇔ abstract）被打破就会分叉，而且是**静默分叉**。
而这两处判定在本仓**本来就相邻**（`PsiRawCfirBuilder.kt:3860` 与 `:3872`；
`LightTreeRawCfirDeclarationBuilder.kt:805-806`、`:959-960`），改成同源是**纯局部**改动：

```kotlin
// 先算"有效 abstract"（显式修饰符 ∨ 隐式推断），DEFAULT 由它取反派生
val isAbstractEffective = hasModifier(CjTokens.ABSTRACT_KEYWORD) ||
        isImplicitAbstractClassLikeMember(declaration, owner)

buildDeclarationStatus(
    ...
    isAbstract = isAbstractEffective,
    isDefault = isInInterfaceMemberContext() && !isForeign && !isAbstractEffective,
)
```

派生结果与官方逐格一致：

| 场景 | `isImplicitAbstract` | `DEFAULT`（派生） | 官方 | ✓ |
|---|---|---|---|---|
| `.cj` interface 有体函数 | false | 是 | 是 | ✓ |
| `.cj` interface 无体函数 | true | 否 | 否 | ✓ |
| **`.cj.d` interface 无体函数** | false（**护栏 1** 抑制后） | **是** | **是** | ✓ |
| **`.cj.d` interface 无体 prop** | true（R4 保留） | 否 | 否 | ✓ |

⇒ **这是本条的完整解法**：**不需要任何 `.cj.d` 专属分支**。4.5.2 的护栏 1 一旦生效，
`DEFAULT` 就自动正确 —— 因为"有效 abstract"这个唯一真源同时喂给了两个属性位。
反过来，这也说明 4.5.2「护栏 2：确认不改，加护栏只是噪声」**推理方向反了**：
该处需要的不是"加一个 `!isDeclarationFile` 护栏"，而是**删掉重复谓词、改为派生**（见 4.5.2 改写版）。

> **残留差异（已知限制，与 `.cj.d` 无关）**：accessor 级 `DEFAULT`。
> 官方在 prop 非抽象时会把 `DEFAULT` 一并置到该 prop 的**全部** getter/setter 上（`:891-894`），
> 本仓 accessor 级别独立用 `hasSyntaxBody` 判定（`LightTreeRawCfirDeclarationBuilder.kt:3270-3273`）。
> 差异只在"**接口属性带 `{}` 但 accessor 无体**"这一**本就报错**的输入上出现（`.cj` 下报 `DiagMissingBody`，属 `HAS_BROKEN` 场景）；
> 该输入在 `.cj.d` 中也是罕见写法，本次不处理，登记为既有差异。

#### 1.3.6 解析后的 AST 形态

声明模式下 **AST 结构与非声明模式完全一致**（同一个 `Parser`、同一套 `ParseFuncBody`），
差异**只在属性位**，且**有减有增**：

```text
FuncDecl（interface 体内）
  funcBody: FuncBody        ← 非空
    body: Block? = nullptr  ← 空
  attributes: -ABSTRACT  -HAS_BROKEN  +DEFAULT        ← 见 1.3.5

FuncDecl（class 体内）
  attributes: -ABSTRACT  -HAS_BROKEN                  ← 不新增 DEFAULT

PropDecl（class / interface 体内）
  attributes: 不变（ABSTRACT 仍保留）                  ← 见 1.3.3
```

`ParseFuncBody`（`ParseDecl.cpp:1668-1713`）在无 `{` 时仍构造 `FuncBody`，只是不设置 `body`，
并把 `end` 定位到 `retType->end` 或 `)` 之后。

> ⚠️ v3 初稿此处写作"唯一差别是属性位：**不含** `ABSTRACT` / `HAS_BROKEN`"，
> 只列了**减少**的属性、漏了 interface 场景下**新增**的 `DEFAULT`（1.3.5）。本版已更正。

---

### 1.4 形态 B：sidecar 合并的精确流程

#### 1.4.1 `.cj.d` 路径推导规则

```cpp
// ImportManager.cpp:289-295
void ImportManager::SaveDepPkgCjdPath(const std::string& fullPackageName, const std::string& cjoPath)
{
    // Replace the suffix directly. Ensure that .cjo and .cj.d are in the same path.
    std::string cjdPath = cjoPath.substr(0, cjoPath.rfind(SERIALIZED_FILE_EXTENSION));
    cjdPath = cjdPath + CJ_D_FILE_EXTENSION;
    cjdFilePaths.emplace(fullPackageName, cjdPath);
}
```

⇒ **规则只有一条：把 `.cjo` 路径的 `.cjo` 后缀替换为 `.cj.d`，同目录同名。**

调用点在 `ResolveImportedPackageForFile`（`ImportManager.cpp:297-310`）：每解析一个 import，就从 `cjoManager->GetPackageCjo(import)` 拿到 `(fullPackageName, cjoPath)` 并登记。键是**全限定包名**，因此同一包多次登记会被覆盖（幂等）。

#### 1.4.2 触发时机

```cpp
// CompileStrategy.cpp:293-298
bool CompileStrategy::ImportPackages() const
{
    auto ret = ci->ImportPackages();
    ParseAndMacroExpandCjd();      // ← 紧跟依赖加载之后
    return ret;
}
```

⇒ 时机是 **`.cjo` 全部加载完成之后、本包 Sema 之前**。这保证了：合并后的注解对后续的 APILevel / SysCap 检查可见。

#### 1.4.3 处理流程

```cpp
// CompileStrategy.cpp:300-332
void CompileStrategy::ParseAndMacroExpandCjd() const
{
    auto cjdPaths = ci->importManager.GetDepPkgCjdPaths();   // [fullPackageName, cjdPath]
    for (auto cjdInfo : cjdPaths) {
        auto sourceCode = FileUtil::ReadFileContent(cjdInfo.second, failedReason);
        if (!failedReason.empty() || !sourceCode.has_value()) { continue; }   // 读不到 → 静默跳过

        ci->invocation.globalOptions.compileCjd = true;                       // 临时开声明模式
        SourceManager& sm = ci->diag.GetSourceManager();
        auto fileId = sm.AddSource(cjdInfo.second, sourceCode.value(), cjdInfo.first);
        auto fileAst = Parser(fileId, sourceCode.value(), ci->diag, ci->diag.GetSourceManager(),
                              false, true).ParseTopLevel();                   // parseDeclFile = true
        auto pkg = MakeOwned<Package>(cjdInfo.first);                         // ← 局部包，不注册
        fileAst->curPackage = pkg.get();
        pkg->files.emplace_back(std::move(fileAst));

        MacroExpansion me(ci);
        me.Execute(*pkg.get());                                              // 对 .cj.d 做宏展开
        ci->invocation.globalOptions.compileCjd = false;                      // 立即关掉

        auto originPkg = ci->importManager.GetPackage(cjdInfo.first);         // 来自 .cjo
        if (!originPkg) { InternalError(cjdInfo.first + " cannot find origin ast"); }
        MergeCusAnno(ci->diag, originPkg, pkg.get());                         // target = cjo, source = cjd
    }
}
```

四个必须复刻的细节：

1. **读不到 `.cj.d` 就静默跳过**（`continue`），不是错误。这意味着 SDK 里可以只放部分 `.cj.d`。
2. `.cj.d` **要跑宏展开**，且全程 `compileCjd = true`。
3. `.cj.d` **不跑 Sema**。
4. `.cj.d` 的 Package 是**局部对象**，不进入 `ImportManager`、不参与符号解析、不写盘。

#### 1.4.4 合并算法规格

入口签名（`MergeAnnoFromCjd.cpp:475`）：

```cpp
void Cangjie::MergeCusAnno(DiagnosticEngine& diag, Ptr<Package> target /* .cjo */, Ptr<Package> source /* .cj.d */);
```

**阶段 1 —— 顶层声明挂接**（`MergeAnnoFromCjd.cpp:477-518`）

```text
topDeclMapping: Map<Decl, Decl?> = {}

for file in source.files:
  for d in file.decls:
    if d is MAIN_DECL or d.annotations.isEmpty(): continue     // ① 无注解不参与
    topDeclMapping[d] = null

for file in target.files:
  for t in file.decls:
    if t is BUILTIN_DECL or !t.IsExportedDecl(): continue      // ② 只匹配导出的声明
    found = topDeclMapping.firstWhere { it.value == null && IsSameDeclByIdentifier(it.key, t) }
    if found != null:
      found.value = t
      t.annotations += found.key.annotations                   // ③ move
      found.key.annotations.clear()
    else:
      Debugln("[apilevel]", identifier, filePath + ":" + line, "not find in cj.d")
```

**阶段 2 —— 成员声明挂接**（`MergeAnnoFromCjd.cpp:527-549`）

```text
memberMapping: Map<Decl, Decl?> = {}
for m in declPair.first.GetMemberDeclPtrs():  memberMapping[m] = null
for m in declPair.second.GetMemberDeclPtrs():
  found = memberMapping.firstWhere { IsSameDeclByIdentifier(it.key, m) }
  if found != null:
    found.value = m
    m.annotations += found.key.annotations
    found.key.annotations.clear()
  else: Debugln("[apilevel] member", m.identifier, "not find in .cj.d")
```

**阶段 3 —— 函数参数挂接**（`MergeAnnoFromCjd.cpp:550-567`）

```text
for (s, t) in memberMapping:
  if t == null or !s.IsFunc(): continue
  for i in s.funcBody.paramLists[0].params.indices:
    t.funcBody.paramLists[0].params[i].annotations += s.funcBody.paramLists[0].params[i].annotations
    s.funcBody.paramLists[0].params[i].annotations.clear()
```

**阶段 4 —— 完整性校验**（`MergeAnnoFromCjd.cpp:569-571`）

```cpp
if (diag.GetErrorCount() == 0) { FullMoveCheck(source); }   // 仅 Debug：遍历 source，任何残留注解都 Debugln
```

注释明确写了方向（`MergeAnnoFromCjd.cpp:357`）：

```cpp
// NOTE: l from .cj.d, r from .cjo.
```

#### 1.4.5 匹配谓词规格

顶层/成员匹配统一走 `IsSameDeclByIdentifier(l /*cj.d*/, r /*cjo*/)`（`MergeAnnoFromCjd.cpp:358-461`）：

**前置归一化**

```cpp
if (l->astKind == ASTKind::VAR_WITH_PATTERN_DECL)
    return IsSamePattern(StaticCast<VarWithPatternDecl>(l)->irrefutablePattern.get(), r);

auto lId   = l->astKind == ASTKind::PRIMARY_CTOR_DECL ? "init" : l->identifier.Val();
auto lKind = l->astKind == ASTKind::PRIMARY_CTOR_DECL ? ASTKind::FUNC_DECL : l->astKind;

bool fastQuit = lId != r->identifier.Val()
             || lKind != r->astKind
             || (l->generic == nullptr) != (r->generic == nullptr);
```

**泛型约束比较（`MergeAnnoFromCjd.cpp:370-401`）**

- `typeParameters.size()` 必须相等；
- 逐位 `identifier` 必须相等；
- `genericConstraints.empty()` 必须一致；
- `l` 的约束数不得多于 `r`（扩展声明会重复插入上界）；
- 逐条比较 `type`，逐条比较 `upperBounds`（数量 + 类型）。

> **DIFF-1**：官方内层循环写了 `for (size_t j = 0; j < l->generic->genericConstraints.size(); ++j)`，上界用的是**外层约束个数**而非 `lgc[i]->upperBounds.size()`，在 `upperBounds.size() < genericConstraints.size()` 时会越界访问 `rgc[i]->upperBounds[j]`。本仓实现必须用 `lgc[i]->upperBounds.size()` 作为上界（语义等价的正确写法），并在测试中覆盖"约束数 > 上界数"的用例。

**按声明种类分派**

| `l.astKind` | 额外比较 |
|---|---|
| `FUNC_DECL` | `IsSameFuncByIdentifier(l.funcBody, r.funcBody)` |
| `PRIMARY_CTOR_DECL` | 归一后同上（`r` 为 `FUNC_DECL` 且 identifier == `"init"`） |
| `EXTEND_DECL` | `extendedType` 类型相等；`inheritedTypes` 数量 + 逐位类型相等 |
| `MACRO_EXPAND_DECL` | `invocation.fullName` 与 `invocation.identifier` 均相等 |
| `VAR_DECL` / `PROP_DECL` | `l.type` 非空时：`r.type` 存在则比 `Type vs Type`，否则比 `Type vs Ty` |
| 其它 | 无额外比较（仅前置归一化） |

**函数谓词 `IsSameFuncByIdentifier`（`MergeAnnoFromCjd.cpp:271-302`）**

```text
generic 存在性一致；generic 存在时 typeParameters 数量与逐个名字相等；
paramLists[0].params 数量相等；
逐参比较 identifier；
逐参比较类型：r.type 存在 → IsSameType(l.type, r.type)；否则 IsSameType(l.type, r.ty)
```

> 参数类型比较里那句注释很关键：
> `// The type in the rb may be empty because the imported declaration does not contain the type node.`
> ⇒ `.cjo` 反序列化出来的参数**可能没有类型节点**，只有 `Ty`。本仓 sidecar 匹配必须同时支持"比类型节点"与"比已解析类型"两条路径。

**类型谓词 `IsSameType`（`MergeAnnoFromCjd.cpp:26-269`）覆盖的形状**

`REF_TYPE`、`QUALIFIED_TYPE`、`FUNC_TYPE`、`TUPLE_TYPE`、`OPTION_TYPE`、`VARRAY_TYPE`、`CONSTANT_TYPE`、`PRIMITIVE_TYPE`；未列出的形状**一律返回 `true`**（`default: break;` 后落到 `return true`）。

已有记载的已知限制（`MergeAnnoFromCjd.cpp:169`）：

```cpp
// If the identifier is the name after the type alias, the comparison will not succeed.
```

⇒ **类型别名会让匹配失败**。这是设计上必须承认的边界。

`PRIMITIVE_TYPE` 在 Type/Type 分支先做 `Rune → UInt8` 名称归一化，但随后仍比较 `kind`（`MergeAnnoFromCjd.cpp:147-154`），不能将两种类型视为等价。Type/Ty 分支直接比较原始名称（`:254-262`）。

**不参与匹配的因素**（本仓必须一致）

- 返回类型（仓颉不允许仅返回类型不同的重载，因此安全）
- 修饰符（`public` / `private` / `open` / `static` / `mut` …）
- 命名参数标记与默认值
- 属性的 getter/setter 形态

**不参与合并的注解位置**（本仓必须一致，并显式记录为已知限制）

- 类型参数上的注解（`TypeParameter.annotations`）—— 官方三个阶段均未覆盖
- 属性 getter/setter 上的注解
- 函数体内的局部声明注解（`.cj.d` 无 body，不适用）

#### 1.4.6 合并后的清理

```cpp
// CheckAPILevel.cpp:284-304
void ClearAnnoInfoOfDepPkg(ImportManager& importManager)
{
    auto clearAnno = [](Ptr<Node> node) {
        auto decl = DynamicCast<Decl>(node);
        if (!decl) { return VisitAction::WALK_CHILDREN; }
        auto isCustomAnno = [](auto& a) { return a->kind == AnnotationKind::CUSTOM; };
        decl->annotations.erase(
            std::remove_if(decl->annotations.begin(), decl->annotations.end(), isCustomAnno), decl->annotations.end());
        return VisitAction::WALK_CHILDREN;
    };
    for (auto& cjdInfo : importManager.GetDepPkgCjdPaths()) {
        auto depPkg = importManager.GetPackage(cjdInfo.first);
        if (!depPkg) { continue; }
        Walker(depPkg, clearAnno).Walk();
    }
}
```

调用点在 `APILevelAnnoChecker::Check(Package&)` 的**末尾**（`CheckAPILevel.cpp:708-709`）。

⇒ 语义是：**合并进来的注解属于"检查期临时状态"，APILevel 检查一结束就把依赖包上的 `CUSTOM` 注解读掉**，避免污染共享的 `.cjo` AST 后续阶段。

#### 1.4.7 合并结果的消费方

`.cj.d` 的注解最终服务于 APILevel / SysCap：

- `APILevelAnnoChecker`（`CheckAPILevel.cpp`）按作用域收集 `@APILevel` / `@Hide` / `@IfAvailable`，其中 `@IfAvailable` 在检查后被脱糖成 `if (DeviceInfo.sdkApiVersion >= level)`。
- 注解解析入口 `APILevelAnnoChecker::Parse` 直接读 `decl.annotations`。
- 强依赖 import：`ChkIfImportDeviceInfo` / `ChkIfImportBase`（`CheckAPILevel.cpp:252-282`）要求显式 `import ohos.device_info.*` / `ohos.base.*`。

⇒ 本仓的对应消费方是 `CfirDeclarationAvailabilityProvider`（`cfir/providers/.../CfirDeclarationAvailabilityProvider.kt:85-129`）与 `CfirApiLevelRefHigherChecker`（`cfir/checkers/.../expression/CfirApiLevelRefHigherChecker.kt:30-129`）：前者从 `declaration.annotations` 里按 `annotationClassId`（`ohos.labels.APILevel` / `ohos.labels.Hide`）取 `since` / `syscap`，后者按 `Hide → APILevel → Syscap` 的优先级报诊断。**注解一旦合并到声明上，这两个消费方无需改动。**

---

### 1.5 官方语义红线

以下 14 条是本设计的**验收基线**，每条都可回溯到官方源码。实现中任何一条被违反，即为设计缺陷。

| # | 红线 | 取证 |
|---|---|---|
| R1 | `.cj` 与 `.cj.d` 是互斥输入，同一模式下不同时收集 | `Option.cpp:744, 750` |
| R2 | `.cj.d` 判定为**字面后缀**匹配，不是"扩展名为 `d`" | `FileUtil.cpp:230-245` |
| R3 | `.cj.d` 中无体**函数**不得标记 `abstract`，只抑制诊断 | `ParseDecl.cpp:1550` |
| R4 | `.cj.d` 中无 `{}` 的**类/接口成员属性**仍然标记 `abstract`（与函数不对称） | `Parser.cpp:371-390` |
| R5 | `.cj.d` 编译只产出 `.cjo`；跳过 desugar/sema 后阶段/泛型实例化/溢出策略/CHIR/codegen | `CjdCompilerInstance.h:22-63` |
| R6 | `.cj.d` 仍完整执行 Sema，仅豁免 1.2.5 表中的 6 项 | 见 1.2.5 |
| R7 | 宏实参重解析与宏展开重解析同样走声明模式 | `MacroEvaluation.cpp:310/562`、`MacroExpansion.cpp:364` |
| R8 | sidecar 路径 = `.cjo` 路径去掉 `.cjo` 后缀后拼 `.cj.d`（同目录同名） | `ImportManager.cpp:289-295` |
| R9 | sidecar 中 `.cj.d` 读不到 → 静默跳过，不报错 | `CompileStrategy.cpp:306-309` |
| R10 | sidecar 的 `.cj.d` 走 Parse + MacroExpand，**不走 Sema** | `CompileStrategy.cpp:300-331` |
| R11 | sidecar 的 `.cj.d` package 不进入 ImportManager，不作为第二份库声明 | `CompileStrategy.cpp:318-330` |
| R12 | 合并只搬 `annotations`，方向恒为 `.cj.d` → `.cjo`，搬完即从源清除 | `MergeAnnoFromCjd.cpp:506-509, 542-545, 562-565` |
| R13 | 只匹配目标侧**导出**的声明；只处理源侧**带注解**的声明；跳过 `MAIN_DECL` 与 `BUILTIN_DECL` | `MergeAnnoFromCjd.cpp:486, 495` |
| R14 | 合并产生的注解在 APILevel 检查结束后从依赖包清除 | `CheckAPILevel.cpp:708-709, 284-304` |
| **R15** | **R3 的连带后果**：`.cj.d` 的 interface 无体函数会**获得 `DEFAULT`** —— `SetDefaultFunc`（`:874-898`）以 `!ABSTRACT` 为条件且无 `parseDeclFile` 门禁，而 `ABSTRACT` 已被 R3 抑制 ⇒ 本仓 `DEFAULT` 必须与 `ABSTRACT` **同源派生**，不得各写一个谓词 | `ParseDecl.cpp:874-898`、`:1518`（`CheckFuncBody`）、`:1529`（`SetDefaultFunc`）；见 1.3.5 |

---

## 2. 本仓现状与差距

### 2.1 关键覆盖点盘点

| 能力 | 本仓现状 | 取证 |
|---|---|---|
| 源文件种类 | `CjSourceFile` 只有 `name` / `path` / `getContentsAsStream()`，**无种类概念** | `common/src/org/cangnova/cangjie/CjSourceFile.kt:16-25` |
| 文件类型注册 | 只有 `CangJie`(`extensions="cj"`) 与 `cjo`(`extensions="cjo"`)；`psi` 内 `declarations/` 包是历史残留（`CjDeclarationsFile.kt` 是空壳） | `intellij-ide/modules/ide/base/src/main/resources/META-INF/cangjie-filetypes.xml:3-12` |
| 文件类型类 | `open class CangJieFileType : LanguageFileType(CangJieLanguage)`，`EXTENSION="cj"` — **是 `open`，可继承** | `psi/src/org/cangnova/cangjie/lang/CangJieFileType.kt:10,37` |
| 解析入口 | `CangJieParser.Companion.parse(builder: PsiBuilder, psiFile: PsiFile)` — **已是文件感知入口**，已按 `.cj.macrocall` 分支 | `psi/src/org/cangnova/cangjie/parsing/CangJieParser.kt:147-172` |
| 解析入口的驱动 | `CjFileElementType.doParseContents` → `parse(builder, psi.containingFile)` | `psi/src/org/cangnova/cangjie/psi/stubs/elements/CjFileElementType.kt:92-98` |
| 解析上下文 | `data class ParsingContext(...)`，已有 `ANNOTATION_ONLY` / `LEGACY` 预设 | `psi/src/org/cangnova/cangjie/parsing/AbstractCangJieParsing.kt:85-168` |
| 缺函数体诊断 | `parseFunction` 的豁免条件为 `isInterfaceMethod \|\| isAbstractDetected \|\| isForeignDetected` | `psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt:3867-3877` |
| 缺属性体诊断 | `parseProperty` 只看 `classdetector.isAbstractDetected`（**看的是 class 而非成员**），且**无 foreign 豁免** | `psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt:2759-2786` |
| 函数体解析 | `parseFunctionBody()` 非 `{` 即报错 | `psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt:4149-4159` |
| PSI body 可空 | **已完备**：`CjDeclarationWithBody.bodyExpression: CjExpression?`、`hasBody()`；Stub 已序列化 `hasBody`/`hasBlockBody` | `psi/src/org/cangnova/cangjie/psi/CjDeclarationWithBody.kt:32-80`；`psi/.../stubs/elements/CjFunctionElementType.kt:60,87,107-111` |
| CFIR body 可空 | **已完备**：`CfirFunction.body: CfirBlock?` | `cfir/cfir-tree/gen/.../declarations/CfirFunction.kt:40` |
| PSI→CFIR 无体 | `buildCfirBody` 已 `if (!hasBody()) return null` | `cfir/raw-cfir/psi2cfir/src/.../PsiRawCfirBuilder.kt:3555-3562` |
| LightTree→CFIR 无体 | `extractBody` 无 `BLOCK` 子节点即返回 null | `cfir/raw-cfir/light-tree2cfir/src/.../LightTreeRawCfirDeclarationBuilder.kt:3082` |
| **隐式 abstract 推断** | `isImplicitAbstractClassLikeMember`：`when` 有**两条分支**——`is CjNamedFunction -> !declaration.hasBody()`（**声明模式会被误判，必须加护栏**）与 `is CjProperty -> body == null && getter == null && setter == null`（**声明模式下语义正确，绝不能加护栏**，见 R4）。LightTree 侧拆成两个独立函数：`isImplicitAbstractClassLikeFunction`（需护栏）/ `isImplicitAbstractClassLikeProperty`（不加护栏） | PSI：`cfir/raw-cfir/psi2cfir/src/.../PsiRawCfirBuilder.kt:3887-3899`；LightTree：`LightTreeRawCfirDeclarationBuilder.kt:3287-3308` |
| 源收集（LightTree） | `when (virtualFile.extension) { "java" -> false; CangJieFileType.EXTENSION -> true; else -> fileType == CangJieFileType.INSTANCE }` | `compiler/frontend/src/.../sources/GroupedCjSources.kt:114-138` |
| 源收集（PSI） | `virtualFile.extension == CangJieFileType.EXTENSION \|\| fileType == CangJieFileType.INSTANCE` | `compiler/frontend/src/.../environment/coreEnvironmentUtils.kt:57-74` |
| 配置项 | Key + 扩展属性两段式（`CompilerConfigurationKey` + `CommonConfigurationKeys`）。`useFir` 是这里的 `CompilerConfiguration` 扩展属性，**与 CLI 参数无关** | `compiler/config/src/.../CommonConfigurationKeys.kt:57-63, 178-180` |
| CLI 参数 | DSL 表驱动（`compilerArguments.kt`）。**实测无漂移**：DSL 4 条 = 生成物 6 条 − 生成器合成 2 条 | DSL：`compiler/arguments/src/.../description/compilerArguments.kt:10-46`；生成物：`compiler/frontend/gen/.../CommonCompilerArguments.kt:7-44`；合成注入：`compiler/frontend-arguments-generator/src/.../Main.kt:116` |
| 前端流水线 | `AbstractFrontendPipeline` **无任何子类**；`CfirFrontendPipelinePhase` **无任何调用方**；`ArgumentsPipelineArtifact → ConfigurationPipelineArtifact` 桥接缺失 | `compiler/frontend/src/.../pipeline/AbstractFrontendPipeline.kt:12-41` |
| `.cjo` 注解反序列化 | **已支持**：`deserializeAnnotations` → `CfirAnnotationCall` | `cfir/cfir-serialization/src/.../deserialize/CfirDeclDeserializer.kt:319-340, 148-165` |
| `.cjo` 写出 | `CjoPackageWriter` 存在但**无生产调用点**，且**不写注解** | `cfir/cfir-serialization/src/.../cjo/CjoPackageWriter.kt:15-33, 136-163` |
| 注解消费 | `CfirDeclarationAvailabilityProvider.ownApiLevelInfo` / `findAnnotations` | `cfir/providers/src/.../declarations/CfirDeclarationAvailabilityProvider.kt:85-129` |
| APILevel/SysCap 检查 | `CfirApiLevelRefHigherChecker` 已实现 `Hide → APILevel → Syscap` 三分支 | `cfir/checkers/src/.../expression/CfirApiLevelRefHigherChecker.kt:30-129` |
| IDE 文件收集 | 白名单 = `CangJieBuiltInFileType` / `extension == "cjo"` / `isCangJieFileType()` | `intellij-ide/modules/ide/base/src/main/kotlin/.../CaIdeScopeCangJieFileCollector.kt:107-111` |
| 依赖模型 | `CjDependency.Binary` 只有 `cjoPath` + `libPath`，**无 declaring 文件概念** | `intellij-ide/modules/domain/project-model/src/main/kotlin/.../CjDependency.kt:305-339` |
| 构建产物落地 | `.cjo` 作为独立 `LibraryRoot(COMPILED)` 加入 `classesRoots` | `intellij-ide/modules/domain/project-model/src/main/kotlin/.../CjWorkspaceModelSync.kt:1375-1380` |
| 构建入口 | 走 `cjpm`（`SDK/tools/bin/cjpm build`），不直接调 `cjc` | `intellij-ide/modules/domain/package-manager/src/main/kotlin/.../CjpmCommandExecutor.kt:48-83` |
| 外部参考实现 | `external/cangjie_deveco_plugins` 用**独立 ANTLR 文法 + 独立 Language + `patterns="*.cj.d"`** | `external/cangjie_deveco_plugins/lsp-client/src/main/resources/META-INF/cangjie-lsp.xml:28-31` |

### 2.2 一句话结论

本仓**已具备**：PSI/Stub 的 body 可空设计、CFIR 的 `body: CfirBlock?`、`.cjo` 注解反序列化、APILevel/SysCap 检查器、文件感知的解析入口。

本仓**完全缺失**：`.cj.d` 文件类型、声明解析模式、源收集的模式区分、声明模式配置项、declaration CFIR 护栏、sidecar 注解合并、依赖模型的 sidecar 表达。

**最大的架构风险不在"识别扩展名"，而在 `isImplicitAbstractClassLikeMember`——它会系统性地把 `.cj.d` 的每个无体成员误判为"隐式抽象"，从而对所有下游语义（override、实例化、可见性、文档、补全）产生连锁错误。**

---

## 3. 总体设计

### 3.1 设计原则

#### 3.1.1 首要原则：框架正确性优先于改动最小化

**取舍顺序是**：先问"框架是否表达了该语义"，再问"改动有多大"。

**允许并鼓励必要的破坏性改动**：当既有框架没有表达某个语义时（例如"缺体"不是一个概念、
checker 缺少"实现完备性"这个分类维度），正确做法是**补足框架**，而不是在每个使用点加条件判断。
后者的典型症状是 —— 单点改动看着最小，但**每新增一个同类实体都要重新人工枚举一遍"这里要不要改"**，
这样的框架无法保证正确性。

**框架正确性 5 条判据**（本设计逐条自检，逐项结论见第 8 章）：

| # | 判据 | 反面信号 |
|---|---|---|
| **J1** | 新增同类实体时，**不需要**重新枚举判断点 | N 个上报点各加一个 `if` |
| **J2** | 同一事实**只有一个**来源 | 同一概念有 3~4 处表达 |
| **J3** | 局部作用域**不会清除**全局语义 | 文件级属性放在可被局部替换的载体里 |
| **J4** | 新增实现者（checker / 解析器）**无须知道**本特性的存在 | 写新 checker 时必须记得"要给 `.cj.d` 加豁免" |
| **J5** | 同源路径**共享同一机制** | PSI 与 LightTree 各写一套模式判别 |

**适用边界**：这条原则**不适用于"纯新增能力"**（新 FileType、新数据来源、新文件格式）。
判断标准是 —— **它是否在旧结构里塞入了与旧结构语义不匹配的新语义**。
本设计中的新 FileType、sidecar 合并、`.cjo` 格式等属于纯新增，保持增量即可（见 8.5）。

#### 3.1.2 其余原则

1. **语义优先于对称**。函数与属性在声明模式下的处理不对称（R3 vs R4），不为了代码整齐而抹平。
2. **一种语义，一个开关**。声明模式是**语言语义模式**，不是性能策略，绝不复用 `BodyBuildingMode.LAZY_BODIES`（后者是"延迟构建 body"，语义上 body 仍然存在）。
3. **两种形态分开实现**。形态 A 是"输入模式"，形态 B 是"注解供给"，共享解析器与匹配谓词，但不共享生命周期。`.cj.d` 绝不作为普通 `.cj` 进入源码集。
4. **不在消费侧打补丁**。`.cj.d` 的支持落在"解析 → 建 CFIR → 加载 `.cjo`"这几条供给链上，`CfirApiLevelRefHigherChecker` 等消费方不做任何改动。
5. **单一真源**。文件种类判定只允许存在一份实现（后缀判定 + PSI 标记），禁止在各模块重复写 `endsWith(".cj.d")`。
6. **可裁剪、可降级**。任一形态可独立交付；形态 B 不可用时，形态 A 的解析与 IDE 编辑能力仍然成立。

### 3.2 分层模型

```text
┌─ 文件种类：唯一真源 ───────────────────────────────────────────────┐
│  CjSourceKind = SOURCE | DECLARATION | MACRO_CALL                 │
│      ↑ 由 FileType 推导（CjFile.sourceKind，只读）                 │
│      ↑ 由文件名推导（CjSourceFile 的 3 个非 PSI 实现）             │
│  CompilerConfiguration.compileCjd : Boolean                       │
│      └ 编译输入模式（"本次收哪种文件"），与种类正交，不参与解析     │
└───────────────┬──────────────────────────────┬────────────────────┘
                │                              │
   形态 A：声明编译                     形态 B：sidecar 合并
   （显式输入 .cj.d）                    （.cjo 旁的 .cj.d）
                │                              │
   ┌────────────▼──────────────┐    ┌──────────▼────────────────┐
   │ psi 解析模式               │    │ CjdSidecarLocator/Index    │
   │  文件 → CangJieParsing     │    │  · 路径推导 .cjo → .cj.d   │
   │    .sourceKind（构造属性） │    │  · 轻量解析 + 签名索引     │
   │  · ParsingContext 不变      │    │  · 匹配谓词（R12/R13）     │
   │  · reportMissingBody()     │    ├───────────────────────────┤
   │    ← 唯一缺体抑制点         │    │ CfirDeclDeserializer       │
   ├───────────────────────────┤    │  · merge 进 annotations    │
   │ compiler/frontend          │    │  · provider 级失效         │
   │  · 源收集两级互斥           │    ├───────────────────────────┤
   │  · Phase 裁剪              │    │ CfirDeclarationChecker    │
   ├───────────────────────────┤    │  · requiresImplementation  │
   │ cfir/raw-cfir              │    │    ← 唯一豁免开关           │
   │  · 护栏（仅函数分支）        │    ├───────────────────────────┤
   ├───────────────────────────┤    │ CfirDeclarationAvailability│
   │ intellij-ide               │    │  · 消费（不改）             │
   │  · toolchain 调 cjc -d     │    └───────────────────────────┘
   └───────────────────────────┘
```

**与 v1/v2 分层图的关键差异**：

| 项 | v1/v2 | v3 |
|---|---|---|
| 种类枚举 | `SOURCE \| DECLARATION` | `SOURCE \| DECLARATION \| MACRO_CALL`（完整 ⇒ `when` 无缺口） |
| 解析模式载体 | `ParsingContext.isDeclarationFile` 字段（可被局部 `with` 覆盖） | `CangJieParsing.sourceKind` **构造属性（不可变）** |
| `CjFile` 侧表达 | `isDeclarationFile: Boolean` 构造参数 | 由 `viewProvider.fileType` 推导，`isDeclarationFile` 仅为派生属性 |
| 缺体抑制点 | 7 处逐点判断 | **1 处**（`reportMissingBody`） |
| Sema 豁免点 | 12 个 checker 各自 early-return | **1 处**（生成器模板里按 `requiresImplementation` + 文件种类过滤，4.4.5） |

### 3.3 关键决策记录

#### D1：`.cj.d` 使用**独立 FileType** 但**复用 `CangJieLanguage`**

- **决策**：新增 `CangJieDeclarationFileType : CangJieFileType`（继承，因为 `CangJieFileType` 已是 `open class`），注册 `patterns="*.cj.d"`，`language="CangJie"`；**不新增 `lang.parserDefinition`**。
- **理由**：
  - 复用 `CangJieLanguage` 后，语法高亮、折叠、注释器、括号匹配、`lang.syntaxHighlighterFactory` 等 language 级扩展**全部自动继承**，无需重复注册。官方参考实现因为使用**独立语言** `CangjieDeclaration`，被迫把 `syntaxHighlighterFactory` / `semanticHighlighter` / `externalAnnotator` 逐一重注册（`cangjie-lsp.xml:43,48,64`），代价明显。
  - 本仓的 `CangJieParserDefinition.createFile` 是 `when (viewProvider.fileType)`，天然支持多 FileType 分派。
  - 本仓的解析入口 `CangJieParser.Companion.parse(builder, psiFile)` 已经是**文件感知**的，不需要靠 Language 来区分解析模式。
- **必须用 `patterns` 而非 `extensions`**：`extensions` 按最后一个 `.` 取后缀，`foo.cj.d` 会被解释成扩展名 `d`。参考实现 `CangjieDeclarationFile.java:54` 的 `getDefaultExtension()` 也返回 `"cj.d"`。
- **反例（被否决）**：新建 `CangJieDeclarationLanguage`。否决理由是它会让所有 language 级扩展需要二次注册，而收益（解析模式隔离）已被 `psiFile` 感知入口覆盖。

#### D2：解析模式是**解析器的不可变构造属性** —— 不放进 `ParsingContext`，也不用全局状态

- **决策**：`CangJieParsing` 构造时接收 `sourceKind: CjSourceKind` 并持有为只读属性（4.2.3）；
  **`ParsingContext` 一个字段都不加、也不新增 `DECLARATION_FILE` 预设** —— 它承载的是"用哪套文法"，
  与"文件是什么种类"正交（4.2.5 的范畴说明）；
  `CangJieParser.Companion.parse(builder, psiFile)` 从 `psiFile` 取出种类后随构造传入，并按种类**穷尽分派**入口（4.2.4 / C-1）。
- **理由**：
  1. **`ParsingContext` 实例会被局部 `with` 整体替换**（每个预设都是全新实例），
     把文件级恒定属性放进去，任何局部 `with` 都会**静默清除**它；症状是"某些位置的诊断突然出现"，极难定位。
  2. **实测该机制的实际职责与设计意图不符**：11 个预设全仓只用到 2 个（`DEFAULT` / `ANNOTATION_ONLY`），
     其余 9 个零使用点 ⇒ 它实际上就是被当作"文件级模式枚举"在使用（4.2.3 理由 1）。
  3. 模式随构造注入后，**不存在"调用点必须选对入口"的隐含契约**，也**不需要新增第三个入口**。
- **反例（被否决）**：`ParsingContext.isDeclarationFile` 字段 + `DECLARATION_FILE` 预设（v1/v2 方案，理由见上）。
- **反例（被否决）**：用 `ThreadLocal` 或在 `CangJieLexer` 里埋开关。否决理由是不可重入、对宏展开重解析不安全（宏展开会在同一线程上对另一份文本重新建树）。
- **对既有代码的正向影响**：`CangJieLightParser` 的 `parseWith`（`CangJieLightParser.kt:63-79`）原本无上下文入口，
  在 v2 方案下是硬阻塞；v3 下它只需多传一个 `sourceKind` 参数（默认值 `SOURCE` 保证既有调用点零改动）。

#### D3：`.cj.d` 中无体函数**不获得 `abstract`**，但无体类成员属性**仍然获得 `abstract`**

- **决策**：严格对齐 R3 + R4。
- **理由**：见 1.3.2 / 1.3.3。这是官方语义的不对称，抹平任何一边都是错的。
- **实现（v3）**：
  - 诊断层面：两类都由 `reportMissingBody` 统一抑制（4.2.6），**抑制逻辑只有一处**。
  - 属性标记层面：`parseProperty` 的分支**保持不变** —— 它在 class/interface 作用域下"无 `{}` 即 abstract"是
    `.cj` 的既有正确语义，`.cj.d` 沿用同一行为（R4）。
  - 函数标记层面：在 CFIR 层处理（D4），**且只作用于函数分支**。
- **注意**：`reportMissingBody` 只统一"**诊断**抑制"，不改变"**是否打 `abstract`**"的判定 ——
  两者的落点与作用域都不同，不能合并。

#### D4：CFIR 层必须显式区分"声明模式"，且护栏**只作用于函数分支**

- **决策**：
  - **PSI 路径**：`isImplicitAbstractClassLikeMember`（`PsiRawCfirBuilder.kt:3887-3899`）的
    **`CjNamedFunction` 分支**（`:3895`）加 `!isDeclarationFile` 护栏；
    **`CjProperty` 分支（`:3896`）保持不动**（R4 要求 `.cj.d` 中无 `{}` 的类成员属性仍获得 `abstract`）。
  - **LightTree 路径**：只给 `isImplicitAbstractClassLikeFunction`（`LightTreeRawCfirDeclarationBuilder.kt:3287-3293`）加护栏；
    `isImplicitAbstractClassLikeProperty`（`:3300-3308`）保持不动。
- **理由**：这是本仓特有的风险点。本仓在 CFIR 层**重新推断**了官方的语法层属性
  （官方在 `CheckFuncBody` 里由解析器直接 `EnableAttr(ABSTRACT)`），而声明模式下官方**不打**这个属性。
  若不加固护栏，`.cj.d` 的每个无体成员都会被下游当成隐式抽象，触发 override 缺失、`open` 可见性、实例化约束等一连串级联误报。
- **⚠️ 护栏作用域本身就是正确性前提**（详见 4.5.2 护栏 1）：
  PSI 侧的函数与属性在**同一个 `when`** 内 —— 若按"给这个函数加护栏"的直觉在 `when` 之外加判断，
  会**连属性分支一起挡掉**，直接违反 R4。
- **反例（被否决）**：在解析层给 `.cj.d` 的无体函数打一个自定义 `Attribute`。否决理由是它会污染 PSI/Stub 的持久化布局（stub 版本升级成本高），而模式信息本来就可以从文件种类稳定获得。

#### D5：形态 B 的注解合并**写回 `CfirDeclaration`**，而不是只在读取侧 union

- **决策**：在 `CfirDeclDeserializer.convertDecl` 完成、`publishAnnotationInfo()` 之前，把 sidecar 注解追加进 `result` 的 annotations 列表。
- **理由**：
  - 官方语义是"注解**成为** `.cjo` 声明的一部分"，不是"读取时叠加"。若只在 `CfirDeclarationAvailabilityProvider` 读取处 union，则 `CfirBuiltInAnnotationSemanticsChecker`（注解参数语义检查）、注解渲染、文档展示、light declaration 等所有其它消费者都看不到合并结果，导致行为分叉。
  - 本仓的 `.cjo` 声明是**惰性物化 + 按 `declIndex` 缓存**的（`CfirDeserializationContext.declCache`），合并写回后天然只发生一次。
- **代价与对策**：需要与官方 `ClearAnnoInfoOfDepPkg` 等价的清理语义。本仓不采用"全局遍历清除"（因为声明是惰性物化的，遍历不到未物化的声明），改用 **provider 生命周期**：sidecar 索引与合并结果绑定在 `CfirDeserializationContext` / `AbstractCfirDeserializedSymbolProvider` 上，`.cj.d` 变更时整体失效重建（见 4.6.4）。

#### D6：形态 B 的匹配采用**精确签名键**，而非官方的"第一个未匹配"

- **决策**：把 `.cj.d` 侧与 `.cjo` 侧各自构建 `DeclarationMatchKey`，用哈希表精确匹配；不采用官方 `unordered_map` + `value == null` + `find_if` 的组合。
- **理由**：
  - 官方实现的匹配顺序依赖 `std::unordered_map` 迭代顺序，**不确定**，在存在同名重载 + 类型别名（`MergeAnnoFromCjd.cpp:169` 已记载别名会导致比较失败）时匹配结果不可预测。
  - `DeclarationMatchKey` 的构成严格依照 1.4.5 的匹配谓词，语义等价但确定性更强。
- **DIFF-2**：这是**有意的严格化**。需要在测试中同时覆盖"官方实现会漏配、本仓能配上"的用例，并在评审时确认该差异可接受。
- **不改变**：类型别名导致匹配失败的边界**保持与官方一致**（不做归一化展开），因为它涉及类型解析，超出 sidecar 的职责。

#### D7：`.cj.d` **不作为独立声明源**进入 IDE 声明提供者聚合

- **决策**：`CaIdeScopeCangJieFileCollector.isCangJieScopeCandidate()` **不加入** `.cj.d` 白名单；`.cj.d` 也不作为独立 `LibraryRoot` 进 `classesRoots`。
- **理由**：直接对齐 R11。若把 `.cj.d` 与 `.cjo` 都作为独立 provider 加入，会产生同包两套声明、重复符号、overload 数量错误、导航目标不稳定、分析缓存重复构建。
- **代价**：IDE 中打开 `.cj.d` 时，"跨文件符号补全"依赖 sidecar 合并（D5）把注解挂到 `.cjo` 声明上——**补全/导航的能力约束在声明自身与已加载的 `.cjo` 上，不来自 `.cj.d` 的独立索引**。
- **`.cj.d` 自身仍拥有完整 PSI**：可高亮、可导航到自身内部符号、可有语法与注解参数诊断。这两件事是正交的。

#### D8：`CjSourceKind` 由 `common` 声明、`psi` 实现（依赖倒置）；`CjFile` 直接持有枚举

- **决策**：
  - `CjSourceFile` 增加 `val sourceKind: CjSourceKind`，**带默认实现**（按文件名后缀判定）；
  - `common` 定义标记契约 `CjSourceKindCarrier`，`psi` 的 `CjFile` 实现它；
  - `CjFile` **直接持有** `sourceKind`（由 `viewProvider.fileType` 推导），`isDeclarationFile` 退化为**派生属性**（4.2.2）。
- **理由**：依赖方向是 `psi → common`，反向不成立 ⇒ `common` 无法判断"这个 PsiFile 是不是声明文件"。
  - `common` 定义**标记契约**（只读属性接口），`psi` 侧 `CjFile` 实现它，`CjPsiSourceFile` 通过类型判定读取。
  - 接口属性带默认实现 → 三个非 PSI 实现类无需改写；且 `logical`（JVM default method）在本仓已全局开启（`-Xjvm-default=all`）。
- **`CjSourceKindCarrier` 是必需品，不是过渡抽象**（容易被误判，已在 4.1.3 显式论证）：
  判据是"如果没有这个契约，是否还存在别的正确实现方式" —— 没有（只能反向依赖，那是错的）。
- **`CjFile` 侧用枚举而非 Boolean 构造参数**：实测 `CjFile` 构造 / 子类点全仓**仅 3 处**
  （`CangJieMacroCallFileType.kt:20`、`CjCodeFragment.kt:53`、`CangJieParserDefinition.kt:110`）
  ⇒ 直接改的代价极低，不需要为"避免触碰构造点"而保留一个可能与 FileType 不一致的 Boolean 字段。
- **反例（被否决）**：只在 `common` 里做后缀判定。否决理由是内存文件、测试夹具、重命名文件会判定失败（当前 PSI 上下文已经知道答案，不应再猜）。
- **反例（被否决）**：`CjFile(isDeclarationFile = true)` 构造参数（v2 方案）。否决理由违反 J2 ——
  会让"种类"的真源分裂为"构造参数"与"FileType"两处。

#### D9：形态 A 的"独立编译"在本仓**不走 `compiler/frontend` 流水线**

- **决策**：
  - **产出 `.cjo`** 由外部 `cjc -d` 完成（IDE toolchain 拼命令行），不在本仓自研 `.cjo` 写出。
  - `compiler/frontend` 只承担"按声明模式收集源 + 构建 declaration CFIR"的能力，供 **IDE / LSP 直接分析 `.cj.d`** 使用。
- **理由**：
  - 本仓 `.cjo` **本来全部来自外部 `cjc`**：`CjoPackageWriter` 除定义与单测外无调用点，且不写 `annotations`（`CjoPackageWriter.kt:15-33`）。
  - `AbstractFrontendPipeline` 无子类、`CfirFrontendPipelinePhase` 无调用方（见 2.1 表），自研 CLI 流水线是另一个独立课题，不应与 `.cj.d` 支持捆绑。
  - `compiler/frontend` 的源收集与 CFIR 构建能力**已经被测试与 IDE 路径使用**（`analyse.kt:CfirSession.buildCfirFromCjFiles` / `buildCfirViaLightTree`），是可靠的落点。

#### D10：诊断呈现 —— 对声明模式**只抑制、不新增**；但修正被误用的既有诊断

- **决策**：
  1. `.cj.d` 中"无函数体"在**解析期不再产生诊断**（对齐 P1/P4/P6）⇒ IDE 语法高亮层不会冒出红色波浪线；
  2. **不新增任何"声明模式"专有诊断**；
  3. **但会修正 5 处被误用的既有诊断文案**（B 组，见 4.2.6）—— 它们本该报"缺标识符 / 缺参数列表"，
     却复用了"函数体缺失"。这属于**修正既有缺陷**，不是"为声明模式新增诊断"。
- **理由**：官方没有为声明模式引入任何新诊断，只是抑制旧诊断。引入新诊断会破坏与官方的一致性。
  而第 3 条恰恰是让"抑制"能收敛为**一个点**的前提：不把这两类语义区分开，就只能逐点人工判断（4.2.6）。
- **补充**：`HAS_BROKEN` 不打（对齐 P5），因此不会有"破损节点"降级路径被触发。
- **影响面**：第 3 条是本设计**唯一**改变既有用户可见行为的改动（5 条错误文案），
  建议单独提交、单独评审，并在提交信息中给出改动前后对照。

---

## 4. 详细设计

### 4.1 `common`：源文件种类

#### 4.1.1 新增 `CjSourceKind`

文件：`common/src/org/cangnova/cangjie/CjSourceKind.kt`

```kotlin
package org.cangnova.cangjie

/**
 * 源文件种类。
 *
 * 与 [org.cangnova.cangjie.source.CjSourceElementKind]（真/假来源）正交：
 * 后者描述"这个元素是否来自真实源码"，本类型描述"这个文件承载的是实现还是声明"。
 */
enum class CjSourceKind {
    /** `.cj` 实现源文件。 */
    SOURCE,

    /** `.cj.d` 声明源文件。 */
    DECLARATION,

    /** `.cj.macrocall` 宏调用文件（由 `CangJieMacroCallFileType` 承载）。 */
    MACRO_CALL,
    ;

    val isDeclaration: Boolean get() = this == DECLARATION

    companion object {
        /** 官方 `CJ_D_FILE_EXTENSION`。 */
        const val DECLARATION_SUFFIX: String = ".cj.d"

        /** 官方 `CJ_EXTENSION`。 */
        const val SOURCE_SUFFIX: String = ".cj"

        /** `.cj.macrocall` 后缀（宏调用文件）。 */
        const val MACRO_CALL_SUFFIX: String = ".cj.macrocall"

        /**
         * 按文件名判定种类。
         *
         * 对齐官方 `HasCJDExtension`：字面后缀匹配，且 `.cj.d` 之前必须存在非空文件名。
         * 因此 `.cj.d` 与 `a.cj.d` 判定不同 —— 前者返回 [SOURCE]。
         */
        fun fromFileName(name: String): CjSourceKind = when {
            name.length > MACRO_CALL_SUFFIX.length && name.endsWith(MACRO_CALL_SUFFIX) -> MACRO_CALL
            name.length > DECLARATION_SUFFIX.length && name.endsWith(DECLARATION_SUFFIX) -> DECLARATION
            else -> SOURCE
        }
    }
}
```

> **v3 说明：为什么要包含 `MACRO_CALL`**（v2 只有两个值）。
> 因为本设计把"**文件语义模式由文件种类派生**"作为唯一机制（4.2.3）：入口分派、诊断抑制、属性位推导
> 都必须能从 `sourceKind` 出发给出确定答案。`.cj.macrocall` 是既有的第三类文件
> （`CangJieMacroCallFileType`，走 `parseOnlyAnnotationFile`），若不纳入枚举：
> ① 入口分派只能退回"调用点自己判断"（现状的 `psiFile is CjMacroCallFile || name.endsWith(".cj.macrocall")`
> 就是这种双真源，见 C-1）；
> ② 任何以 `sourceKind` 为被检对象的 `when` 都会出现分支缺口，编译器**帮不上忙**。
> ⇒ 枚举完整 ⇒ `when` 无缺口 ⇒ 编译器帮你保证穷尽性。
>
> 注意 `MACRO_CALL` 在**源收集**中恒被拒绝（4.4.1）—— 它只是"能被识别的种类"，不是"能被收集的源"。

> **R2 落地**：`name.length > DECLARATION_SUFFIX.length` 精确对应官方的 `fileIt != fileEnd` 尾部检查。

#### 4.1.2 扩展 `CjSourceFile`

文件：`common/src/org/cangnova/cangjie/CjSourceFile.kt`（修改）

```kotlin
interface CjSourceFile {
    val name: String
    val path: String?

    /**
     * 源文件种类。
     *
     * 默认按文件名后缀判定；对能自证种类的实现（例如 PSI 文件），
     * 应覆写为读取显式标记。
     */
    val sourceKind: CjSourceKind get() = CjSourceKind.fromFileName(name)

    fun getContentsAsStream(): InputStream
}
```

四个既有实现类的覆写策略：

| 实现类 | `sourceKind` |
|---|---|
| `CjPsiSourceFile` | `(psiFile as? CjSourceKindCarrier)?.sourceKind ?: CjSourceKind.fromFileName(psiFile.name)` |
| `CjVirtualFileSourceFile` | 默认实现（按 `name`） |
| `CjIoFileSourceFile` | 默认实现（按 `name`） |
| `CjInMemoryTextSourceFile` | 默认实现，但允许构造函数显式注入（见下） |

`CjInMemoryTextSourceFile` 增加可选构造参数：

```kotlin
class CjInMemoryTextSourceFile(
    override val name: String,
    override val path: String?,
    val text: CharSequence,
    private val explicitKind: CjSourceKind? = null,
) : CjSourceFile {
    override val sourceKind: CjSourceKind get() = explicitKind ?: CjSourceKind.fromFileName(name)
    override fun getContentsAsStream(): InputStream = ByteArrayInputStream(text.toString().toByteArray())
}
```

> 这是 D8 中"内存文件可被正确判定"的落点。

#### 4.1.3 标记契约 `CjSourceKindCarrier`（依赖倒置的必需品）

文件：`common/src/org/cangnova/cangjie/CjSourceKindCarrier.kt`

```kotlin
package org.cangnova.cangjie

/**
 * 能自证源文件种类的对象。
 *
 * 由 `psi` 模块的 PSI 文件实现；`common` 只声明契约，不反向依赖 `psi`。
 */
interface CjSourceKindCarrier {
    val sourceKind: CjSourceKind
}
```

**它不是在"为避免破坏而引入的过渡抽象"，而是依赖方向的必然结果** —— 容易误判，故显式说明：

- 实测：`CjPsiSourceFile`（`common/src/org/cangnova/cangjie/CjSourceFile.kt:30`）包装 `PsiFile`，
  而 `CjFile` 定义在 `psi` 模块。依赖方向是 `psi → common`，因此 `CjPsiSourceFile` **无法**引用 `CjFile`。
- 而"这个文件是 `.cj` 还是 `.cj.d`"的**权威答案在 PSI 侧**（`CjFile.sourceKind`）。
- ⇒ 必须在 `common` 定义一个 `psi` 可实现的契约，让 `CjPsiSourceFile` 通过类型判定取值。
  另外三个实现类（虚拟文件 / IO 文件 / 内存文本）与 PSI 无关，按文件名判定即可。

> **判据**：区分"必需品"与"过渡壳"的标准是 —— **如果没有这个契约，是否还存在别的正确实现方式？**
> 这里没有（只能反向依赖，那是错的）。故保留。
> 对照：v2 曾为"避免破坏 4 个 `CjSourceFile` 实现类"而给接口属性加默认实现 —— 那条同样不是过渡壳，
> 因为默认实现表达的是"能按文件名自证就自证"这一**真实语义**，而非纯粹的兼容妥协。

#### 4.1.4 `CfirFile` 的种类暴露

`CfirFile.sourceFile: CjSourceFile?`（`cfir/cfir-tree/gen/.../declarations/CfirFile.kt:32`）已经能通过 `sourceFile?.sourceKind` 表达种类，**无需新增字段**。

建议在 `cfir/cfir-tree` 的扩展文件中补一个便捷属性（不修改生成代码）：

```kotlin
/** 该 CFIR 文件是否来自 `.cj.d`。 */
val CfirFile.isDeclarationFile: Boolean
    get() = sourceFile?.sourceKind?.isDeclaration == true
```

---

### 4.2 `psi`：声明文件类型与解析模式

#### 4.2.1 新增文件类型

文件：`psi/src/org/cangnova/cangjie/lang/declarations/CangJieDeclarationFileType.kt`

```kotlin
package org.cangnova.cangjie.lang.declarations

import org.cangnova.cangjie.lang.CangJieFileType

/**
 * `.cj.d` 声明文件类型。
 *
 * 复用 [org.cangnova.cangjie.lang.CangJieLanguage]，
 * 因此词法、语法、PSI、以及全部 language 级扩展（高亮、折叠、注释、括号匹配）
 * 无需重复注册即可生效。
 */
object CangJieDeclarationFileType : CangJieFileType() {
    /** 官方 `CJ_D_FILE_EXTENSION`。 */
    const val DECLARATION_EXTENSION: String = "cj.d"

    override fun getName(): String = "CangJieDeclaration"

    override fun getDescription(): String = "CangJie declaration file"

    override fun getDefaultExtension(): String = DECLARATION_EXTENSION
}
```

> 说明：`CangJieFileType` 的 `getName()` 返回 `CangJieLanguage.displayName`（即 `"CangJie"`），与 `CangJie` FileType 重名，`FileTypeRegistry` 需要唯一名，因此必须覆写（参考实现同样用 `"CangjieDeclaration"`）。
>
> ⚠️ **`open class` 继承是承载性约束，不是风格选择**（B-4）：
> `CangJieDeclarationFileType` **必须**继承 `CangJieFileType`，否则两件事会同时坏掉 ——
> ① `CangJieParserDefinition.createFile` 的 `is CangJieFileType ->` 不再命中，`.cj.d` 会 fallthrough 到
> `PsiPlainTextFileImpl`（退化成纯文本 PSI）；
> ② 4.2.2 的 `sourceKind` 推导（`viewProvider.fileType is CangJieDeclarationFileType`）虽有独立判定，
> 但 `CjFile` 根本不会被创建，判定无从发生。
> 继承关系同时带来一个好处：**既有 `createFile` 无需任何改动**（4.9.2）。
> **v2 实测确认**：`CangJieLanguage` 是 `object CangJieLanguage : Language("CangJie")`（`CangJieLanguage.kt:33`），**未覆写 `displayName`**
> ⇒ `getName()` 实际返回 `Language` 默认实现的 id，即 `"CangJie"`，与 XML 中 `<fileType name="CangJie">` 一致。
> 因此 `CangJieDeclarationFileType` **必须**覆写 `getName()` 返回不同字符串，否则会与既有 `CangJie` FileType 在 `FileTypeRegistry` 中重名。
> 复用现成的 `psi/src/org/cangnova/cangjie/lang/declarations/` 包——该包当前只有 `.cjo` 的 `CangJieBuiltInFileType`（对象名在文件 `CangJieDeclarationsFileType.kt:37` 内，文件名与对象名**不符**）与空壳的 `CjDeclarationsFile.kt`。
>
> **文件重命名决策（v2 闭环，v1 为"可选，不阻塞"）**：**本次不改文件名**。
> 理由：① Kotlin 不要求文件名与顶层声明同名，`CangJieBuiltInFileType` 在现名下编译正常；
> ② 重命名属于与 `.cj.d` 特性无关的纯整理，按 `AGENTS.md` §11「不要在 feature/bugfix 提交中混入无关重构」应当剥离；
> ③ 本特性本来就要在该目录**新增** `CangJieDeclarationFileType.kt`，届时包内会有两个命名风格的文件，但这是**可接受的过渡状态**。
> ⇒ 登记为独立技术债即可，不进入本特性的任何阶段。

#### 4.2.2 扩展 `CjFile`

文件：`psi/src/org/cangnova/cangjie/psi/CjFile.kt`（修改）

```kotlin
open class CjFile(
    viewProvider: FileViewProvider,
    isCompiled: Boolean = false,
    val isCodeFragment: Boolean = false,
) : CjCommonFile(viewProvider, isCompiled), CjSourceKindCarrier {

    /**
     * 文件种类。
     *
     * 由 view provider 的 FileType 推导，是"是否为声明文件"的**唯一真源**。
     *
     * 为什么用枚举字段而不是 `isDeclarationFile: Boolean` 构造参数：
     * ① 种类是枚举，天然可扩展（4.1.1 已含 `MACRO_CALL`）；
     * ② Boolean 版本会把"事实来源"分裂成"构造参数 / FileType / 配置项"多处（见 4.2.3 理由 2）。
     */
    override val sourceKind: CjSourceKind
        get() = if (viewProvider.fileType is CangJieDeclarationFileType) {
            CjSourceKind.DECLARATION
        } else {
            CjSourceKind.SOURCE
        }

    /**
     * 兼容既有调用点的**派生**属性（不是独立事实，不得赋值）。
     * 派生自 [sourceKind]，因此永远与 FileType 一致。
     */
    val isDeclarationFile: Boolean get() = sourceKind.isDeclaration
}
```

`CjMacroCallFile`（`psi/src/org/cangnova/cangjie/macro/file/CangJieMacroCallFileType.kt:20`）作为子类自证种类：

```kotlin
override val sourceKind: CjSourceKind get() = CjSourceKind.MACRO_CALL
```

> **为什么不在 `CjFile` 里判宏调用类型**：那会让 `CjFile` 反向引用它的子类构造类型（`CjMacroCallFile` 继承自 `CjFile`）。
> 由子类覆写是天然而无环的做法。

> **v3 说明：构造点少，不需要兼容层**。
> 实测 `CjFile` 的构造 / 子类点全仓**仅 3 处**：
> `CangJieMacroCallFileType.kt:20`（`CjMacroCallFile`）、`CjCodeFragment.kt:53`（`CjCodeFragment`）、
> `CangJieParserDefinition.kt:110`（`CjFile(viewProvider, false)`）。
> 因此**直接改**即可 —— v2 曾提出的 `isDeclarationFile: Boolean` 构造参数方案（为"避免触碰构造点"）没有必要，
> 它会留下"构造参数与 FileType 可能不一致"的隐患。

同时把 `getFileType()` 从硬编码改为尊重 view provider：

```kotlin
// 现状（CjFile.kt:66）
override fun getFileType(): FileType = CangJieFileType.INSTANCE

// 改为
override fun getFileType(): FileType = viewProvider.fileType
```

> **风险提示**：`getFileType()` 返回 `CangJieFileType.INSTANCE` 是当前的行为契约，改动的爆炸半径需要评估。若不改，`.cj.d` 的 `CjFile` 会自称是 `.cj` 类型，导致 `FileTypeRegistry` 相关判断（如保存时的文件类型推断、图标、`isCangJieFileType()`）出现偏差。
> **决策（v1 此处结论已翻转）**：**本次就改**为 `viewProvider.fileType`。
> v1 曾建议"不改，列为独立技术债"。该建议被推翻，理由是爆炸半径**已经枚举清楚**而非"未知"：
> `psi` 内 `getFileType()` 覆写共 **2 处**（`CjFile.kt:66` 与 `CangJieMacroCallFileType.kt:34`，
> 后者是 `CjMacroCallFile` 自覆写、**不受基类改动影响**，且它本身就是"子类返回自身真实 FileType"的现成正向先例）；
> `CangJieFileType.INSTANCE` 的 **7 处**引用**无一**读取 `PsiFile.getFileType()`；
> 且 `CjFile` 只可能由 `createFile` 的两个分支创建（4.9.2），故 `viewProvider.fileType` 的取值域**只有
> `CangJieFileType` 与 `CangJieDeclarationFileType` 两个值**，改动后 `.cj` 行为不变、`.cj.d` 得到期望值。
> 完整论证与验收断言见 7.3-OP1 与 6.6。**不改反而会留下 `psiFile.getFileType() ≠ viewProvider.fileType` 的自相矛盾状态。**
>
> **与 `sourceKind` 的一致性**：两者都从 `viewProvider.fileType` 推导 ⇒ 天然一致，不存在"字段与类型打架"的可能。

#### 4.2.3 解析模式是**解析器的构造属性**，不进 `ParsingContext`

**这是本设计区别于"在既有结构上加字段"路线的关键点，先说结论与理由。**

**决策**：`CangJieParsing` 在构造时即持有文件种类 `sourceKind: CjSourceKind`（**不可变**）；
**不**向 `ParsingContext` 增加 `isDeclarationFile` 字段，也**不**新增 `DECLARATION_FILE` 预设。

**理由（三条，均为实测）**：

1. **`ParsingContext` 的实际职责与它的设计意图不符。**
   全仓统计（**B-1 复核后修正**）：`ParsingContext` 有 **15 个字段**（主构造；`strictMode` 已被注释掉）、
   **11 个活跃预设，实际只使用 2 个** ——
   `DEFAULT`（6 次：`CangJieParser.kt:88/104/120/138/184` + `CangJieParsing.kt:1312`）与
   `ANNOTATION_ONLY`（1 次：`CangJieParsing.kt:1282`）；
   其余 **9 个**（`LEGACY` `:168` / `REPORT` `:173` / `SILENT` `:176` / `IF_WHILE_CONDITION` `:180` /
   `MATCH_EXPRESSION_MODE` `:183` / `FUNCTION_LITERAL_BLOCK` `:186` / `FUNCTION_LITERAL_COLLAPSED` `:189` /
   `MACRO_BACK_TOKEN` `:192` / `NO_STRING_INTERPOLATION` `:195`）**零使用点**。
   ⇒ 它在实践中的真实职责就是「**给一个文件级入口选一种解析模式**」。
   继续往里加第三种模式，等于用一个"名义上是细粒度局部开关、实际是模式枚举"的结构去承载文件级属性 —— **语义错配**。

   > **复核命令（可复现，建议直接贴进实现注释）**：
   > ```bash
   > # 预设清单（含行号）
   > grep -n "= ParsingContext(" psi/src/org/cangnova/cangjie/parsing/AbstractCangJieParsing.kt
   > # 全仓使用点（排除定义文件本身）
   > grep -rn "ParsingContext\.[A-Z_]" --include=*.kt psi cfir analysis compiler lsp common \
   >   | grep -v "/build/" | grep -v "AbstractCangJieParsing.kt" \
   >   | sed 's/.*ParsingContext\.\([A-Z_]*\).*/\1/' | sort | uniq -c | sort -rn
   > # 期望输出：6 DEFAULT / 1 ANNOTATION_ONLY —— 其余预设零命中
   > ```
   > 统计口径说明：**只统计"预设"的引用**（`ParsingContext.XXX`），
   > 不含 `ParsingContext(...)` 构造调用（后者是预设定义本身与新实例创建）。

2. **`ParsingContext` 实例会被局部 `with` 整体替换，文件级属性放进去会被静默清除。**
   `IF_WHILE_CONDITION = ParsingContext(allowLetExpression = true)` 这类预设都是**全新实例**，
   其 `isDeclarationFile` 会回到默认 `false`。
   今天没有调用点，所以不会立刻发作；但一旦任何人（包括本特性的后续维护者）在文件解析路径上包一层局部 `with`，
   **声明模式会静默失效**，症状是"某些位置的诊断突然冒出来"，极难定位。
   ⇒ **文件级恒定属性不能放在随时可能被局部替换的载体里。**

3. **`CangJieParsing` 目前根本不持有文件信息。**
   实测 `createForTopLevel(builder, languageModuleName)` → `CangJieParsing(builder, isTopLevel, isLazy, languageModuleName)`
   （`CangJieParsing.kt:159-171`）—— 解析器**不知道自己在解析哪个文件**。
   这正是"模式只能靠调用点选择、或硬编码在函数体里"的根因。

**改动**：

```kotlin
class CangJieParsing(
    builder: SemanticWhitespaceAwarePsiBuilder,
    isTopLevel: Boolean,
    isLazy: Boolean,
    languageModuleName: String,
    /** 当前解析文件的种类。文件级恒定，创建后不可变。 */
    val sourceKind: CjSourceKind,
) : AbstractCangJieParsing { ... }
```

```kotlin
fun createForTopLevel(
    builder: SemanticWhitespaceAwarePsiBuilder,
    languageModuleName: String = "",
    sourceKind: CjSourceKind = CjSourceKind.SOURCE,
): CangJieParsing = CangJieParsing(builder, true, true, languageModuleName, sourceKind)

fun createForTopLevelNonLazy(
    builder: SemanticWhitespaceAwarePsiBuilder,
    languageModuleName: String = "",
    sourceKind: CjSourceKind = CjSourceKind.SOURCE,
): CangJieParsing = CangJieParsing(builder, true, false, languageModuleName, sourceKind)
```

**访问方式**：解析器内部所有需要判断模式的地方读 `sourceKind`（构造属性），**不再读 `parseContext`**。
默认参数保证既有调用点（测试夹具等）无需改动即可编译。

**顺带清理（建议独立提交，不属本特性功能）**：删除 **9** 个零使用点的 `ParsingContext` 预设。
保留死代码会让下一个实现者误以为"这套机制支持细粒度模式"，从而继续往上堆字段 —— 这是一次性收敛。

#### 4.2.4 接入解析入口

文件：`psi/src/org/cangnova/cangjie/parsing/CangJieParser.kt`（修改，实测 `:147-172`）

```kotlin
@NotNull
@JvmStatic
fun parse(psiBuilder: PsiBuilder, psiFile: PsiFile): ASTNode {
    psiBuilder.setDebugMode(true)

    val languageModuleName = if (psiFile is CjFile) {
        psiFile.parsedLanguageModuleName = null
        psiFile.parserLanguageModuleName
    } else ""

    // 模式由文件推导：一次确定，贯穿本次解析，且不可被局部上下文覆盖
    val sourceKind = (psiFile as? CjFile)?.sourceKind ?: CjSourceKind.fromFileName(psiFile.name)

    val cjParsing = createForTopLevel(
        SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder),
        languageModuleName,
        sourceKind,                       // ← 模式随构造注入
    )

    // 入口分派：穷尽 when，无 else —— 新增文件种类时编译器会强制你在此表态
    when (sourceKind) {
        CjSourceKind.MACRO_CALL -> cjParsing.parseOnlyAnnotationFile()
        CjSourceKind.SOURCE,
        CjSourceKind.DECLARATION -> cjParsing.parseFile()
    }

    if (psiFile is CjFile) psiFile.parsedLanguageModuleName = cjParsing.languageModuleName
    return psiBuilder.treeBuilt
}
```

> **关键点**：这里能直接拿到 `psiFile`，是因为 `CjFileElementType.doParseContents` 传的是 `psi.containingFile`（`CjFileElementType.kt:97`）。因此**不需要**为 `.cj.d` 新建 Language 或 ParserDefinition（D1）。
>
> **⚠️ 更正：原 `name.endsWith(".cj.macrocall")` 判定已删除（C-1）**
>
> v3 初稿在此保留了现状的
> ```kotlin
> // ❌ 已删除
> if (psiFile is CjMacroCallFile || psiFile.name.endsWith(".cj.macrocall")) { ... }
> ```
> 但 4.2.5 又按 `sourceKind` 把 `MACRO_CALL` 映射到 annotation-only 入口 ——
> "这个文件是宏调用"因此同时有 **`CjSourceKind.MACRO_CALL`** 与 **文件名后缀** 两个来源，
> 正是本文档 J2 判据（同一事实是否有多个来源）的反面信号。
> 既然 4.1.1 已经为"消除 `when` 缺口"引入了第三个枚举值，入口处就应当据此收敛。
>
> **为什么可以安全删除**：`MACRO_CALL` 的判定已经有两重覆盖 ——
> `CjMacroCallFile` 覆写 `sourceKind`（4.2.2），且 `CjSourceKind.fromFileName` 也识别 `.cj.macrocall`（4.1.1）。
> 原后缀兜底要防的场景（文件类型未注册时）由 `fromFileName` 接管，**能力没有丢失，只是搬进了唯一真源**。
>
> **注意：仍然只有两个入口，不新增第三个。**
> `DECLARATION` 与 `SOURCE` 共用 `parseFile()` —— 二者**文法完全相同**，区别只在构造属性 `sourceKind`（4.2.5）。
> 这正是 4.2.3 舍弃 v2 "第三个入口"方案的直接体现：**声明文件不需要自己的入口，它需要的是自己的 `sourceKind`**。
>
> **两分支 → 穷尽 `when` 的收益**：若将来加入第四种文件种类（如 `.cj.sig`），
> 编译器会在此处直接报 "when expression must be exhaustive"，**而不是静默走错分支**。
>
> **改动性质**：对 `CangJieParser.parse` 的**增量修改**（新增 1 行取值、1 个实参、把 if-else 换成穷尽 `when`），
> 不触及解析逻辑本身。

#### 4.2.5 文件级入口：模式从构造属性派生

文件：`psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt`（修改，实测 `:1310-1327`）

**实测现状**（注意 `parseFile` 是 **no-arg** 的，`with(ParsingContext.DEFAULT)` 硬编码在函数体内）：

```kotlin
fun parseFile() {
    with(ParsingContext.DEFAULT) {
        val fileMarker = mark()
        parsePreamble()
        while (!eof()) { parseTopLevelDeclaration() }
        checkUnclosedBlockComment()
        fileMarker.done(CJ_FILE)
    }
}
```

`parseOnlyAnnotationFile()`（`:1280-1297`）与之**逐字相同**，唯一差别是硬编码了
`with(ParsingContext.ANNOTATION_ONLY)`。

**v3 改动**：只抽出一个共享的文件体，**两处硬编码的 `with` 都原样保留**：

```kotlin
/**
 * 文件级入口：完整仓颉文件语法（`.cj` 与 `.cj.d` 共用）。
 *
 * ⚠️ 声明模式**不在这里出现**：它不改变任何语法入口参数（`ParsingContext` 一个字段都不动），
 * 只影响少数诊断与属性位，由构造属性 [sourceKind] 承载（4.2.3）。
 */
fun parseFile() {
    with(ParsingContext.DEFAULT) {
        parseFileBody()
    }
}

/**
 * 文件级入口：annotation-only 语法。
 *
 * 两类调用方：① `.cj.macrocall` 文件（由 `sourceKind == MACRO_CALL` 派生，见 4.2.4）；
 * ② 宏展开产物的**片段重解析**（`CangJieLightParser.parseAnnotationOnly`，与文件种类无关）。
 */
fun parseOnlyAnnotationFile() {
    with(ParsingContext.ANNOTATION_ONLY) {
        parseFileBody()
    }
}

/** 文件体解析（两个入口共享；抽取前两处实现逐字相同）。 */
private fun parseFileBody() {
    val fileMarker = mark()
    parsePreamble()
    while (!eof()) { parseTopLevelDeclaration() }
    checkUnclosedBlockComment()
    fileMarker.done(CJ_FILE)
}
```

> **⚠️ 更正：不存在 `ParsingContext.DECLARATION_FILE`（A-2）**
>
> v3 初稿在此处给过一段 `fileParsingContext()`：
> ```kotlin
> // ❌ 已删除
> private fun fileParsingContext(): ParsingContext = when (sourceKind) {
>     CjSourceKind.SOURCE      -> ParsingContext.DEFAULT
>     CjSourceKind.DECLARATION -> ParsingContext.DECLARATION_FILE
>     CjSourceKind.MACRO_CALL  -> ParsingContext.ANNOTATION_ONLY
> }
> ```
> 它与 4.2.3「**不**新增 `DECLARATION_FILE` 预设」**直接冲突**（同一份文档给出两个相反结论），且即使补齐定义也是**语义空壳**：
>
> - 官方声明模式在解析层只改了 6 个抑制点（1.3.1），**全部是诊断 / 属性位抑制**，不涉及任何 `ParsingContext` 字段所表达的解析行为；
> - 而抑制已由构造属性 `sourceKind` 承载（D2 明令"解析器内部需要判断模式的地方读 `sourceKind`，**不再读 `parseContext`**"）。
>   ⇒ `DECLARATION_FILE` 里**没有任何字段可设**，与 `DEFAULT` **恒等**。
> - 把一个恒等映射挂在 `when` 里，比删掉它更糟：它暗示"声明模式在语法层面有差异"，会误导后来者去找那个不存在的差异。
>
> **根因是分类维度混淆**：`ParsingContext` 承载的是**语法入口参数**（`disableMacroParsing`、`preferBlock`…），
> 属于"用哪套文法解析"；而"这个文件是 `.cj` 还是 `.cj.d`"是**文件语义模式**，属于"解析出来的东西意味着什么"。
> 两者**正交**：官方用同一个 `Parser`、同一套文法解析两种文件，只在语义处置上分叉。
> v2 把后者塞进前者的容器里，就是范畴错误 —— 这也是它必须靠"调用点选对入口"来保证正确的原因。
>
> **v3 定稿的分工**（下表替代 `fileParsingContext()`）：

| 维度 | 承载者 | 取值来源 | 举例 |
|---|---|---|---|
| **语法入口参数** | `ParsingContext` | **入口函数各自硬编码**（与 v1 现状一致，不改） | `DEFAULT`（完整文件）/ `ANNOTATION_ONLY`（annotation-only 片段） |
| **文件语义模式** | `CangJieParsing.sourceKind`（构造属性） | 文件本身 | `SOURCE` / `DECLARATION` / `MACRO_CALL` |

**入口数量保持 2 个，不新增**：

| 入口 | 服务对象 | `ParsingContext` | 选择者 |
|---|---|---|---|
| `parseFile()` | `.cj` **与** `.cj.d`（文法相同） | `DEFAULT` | 由 `sourceKind` 派生（4.2.4） |
| `parseOnlyAnnotationFile()` | `.cj.macrocall` 文件；宏展开片段重解析 | `ANNOTATION_ONLY` | 文件级由 `sourceKind` 派生；片段级由调用点显式指定 |

> v2 提出的"新增第三个入口 `parseDeclarationFile()`"已被舍弃（理由见 4.2.3）：
> 声明文件的**文法与 `.cj` 完全相同**，它不需要自己的入口，它需要的是**自己的 `sourceKind`**。

**对 `CangJieLightParser` 的影响（v2 的阻塞点由此消解）**：

实测 `CangJieLightParser.parseWith`（`CangJieLightParser.kt:63-79`）直接 `cjParsing.parseAction()`，
没有上下文透传。在 v2 方案下这是**硬阻塞**（LightTree 拿不到声明模式，且其自带的 `reportErrors`
会因 `ERROR_ELEMENT` 而暴露结构不完整的 `FUNC`/`PROPERTY` 节点 ⇒ sidecar 签名键出错）。

v3 下只需给两个入口各加一个参数（**透传的是 `sourceKind`，不是 `ParsingContext`**）：

```kotlin
/** 文件级：sourceKind 来自被解析的文件。 */
fun parse(
    builder: PsiBuilder,
    errorListener: LightTreeParsingErrorListener? = null,
    languageModuleName: String = "",
    sourceKind: CjSourceKind = CjSourceKind.SOURCE,      // ← 新增
): FlyweightCapableTreeStructure<LighterASTNode> =
    parseWith(builder, errorListener, languageModuleName, sourceKind) { parseFile() }

/** 片段级：sourceKind 由调用方按"该片段来源文件"传入（宏展开产物属于展开它的那个文件）。 */
fun parseAnnotationOnly(
    builder: PsiBuilder,
    errorListener: LightTreeParsingErrorListener? = null,
    languageModuleName: String = "",
    sourceKind: CjSourceKind = CjSourceKind.SOURCE,      // ← 新增
): FlyweightCapableTreeStructure<LighterASTNode> =
    parseWith(builder, errorListener, languageModuleName, sourceKind) { parseOnlyAnnotationFile() }

// parseWith 内部：
val cjParsing = CangJieParsing.createForTopLevelNonLazy(
    SemanticWhitespaceAwarePsiBuilderImpl(builder),
    languageModuleName,
    sourceKind,                                          // ← 透传给构造属性
)
```

⇒ **不需要引入任何"上下文透传"机制**：模式只多一个参数，且默认值与既有行为一致（`SOURCE`），
既有调用点（`LightTreeRawCfirDeclarationBuilder.kt:1477` 等）零改动即可编译。

> **规则（写进实现注释）**：**凡进入解析器，必带"被解析代码归属的文件种类"。**
> 文件级解析由文件决定；片段级重解析由**片段的来源文件**决定 —— 因为宏展开产物是那个文件的一部分，
> 在 `.cj.d` 中展开出的代码同样不该被当成"有体实现"。这样就不存在"某个调用点忘了传"的第三态。

#### 4.2.6 诊断抑制：统一上报入口 + 修正被误用的诊断语义

**这一节是 v3 相对"逐点加 `if`"路线最大的改动。**

##### 现状与问题

本仓有 **12 处**上报 `parsing.error.function.body.expected` / `expecting.symbol "{"`。
逐点读代码后发现它们**语义并不相同**，分两类：

| 类别 | 数量 | 位置特征 | 触发文本 | 是否属于"缺体" |
|---|---|---|---|---|
| **A：合法无体声明** | 7 | 名字 / 参数列表 / 返回类型**已解析完**，只是缺 body | `class C { func f(): Int64 }` | **是** |
| **B：语法错误恢复** | 5 | **关键字 `advance()` 之后立即** `if (at(RBRACE))` | `class C { func }`（缺函数名） | **否** |

**B 组是既有的诊断语义误用**：`parseFunction`（`:3808-3817`）在跳过 `func` 关键字后立刻判断 `at(RBRACE)`，
此时连函数名都没有，却复用了"函数体缺失"的文案。它们的真实问题是**缺标识符 / 缺参数列表**。

⇒ 这正是"逐点加 `if (!isDeclarationFile)`"方案必须人工分组、且极易出错的原因：
**同一个诊断消息被两种语义共用了。**

##### v3 方案：两步

**第一步 —— 建立统一上报入口**（`CangJieParsing` 内）：

```kotlin
/**
 * 上报"声明体缺失"类诊断。
 *
 * 【唯一】的缺体抑制点：声明文件（`.cj.d`）中函数 / 构造 / 访问器 / 属性的体允许不存在，
 * 对齐官方 `Parser::parseDeclFile`（P1 / P4 / P6）。
 *
 * 只用于"名字、参数列表、类型都已解析完，仅缺 body"的情形。
 * 残缺输入（如 `class C { func }`，连函数名都没有）在各自的上报点走
 * "缺标识符 / 缺参数列表"诊断，**不经过这里**。
 *
 * @param report 具体诊断的上报动作。消息按 owner 不同而不同（函数 / 属性 / 访问器各有措辞），
 *   但"是否抑制"只在 reportMissingBody 内部判定一次 —— 新增体 owner 时不需要再判断模式。
 */
context(parseContext: ParsingContext)
private fun reportMissingBody(report: () -> Unit) {
    if (sourceKind.isDeclaration) return
    report()
}

/** reportMissingBody 的默认文案重载：函数 / 构造 / 访问器体缺失。 */
context(parseContext: ParsingContext)
private fun reportMissingBody() = reportMissingBody {
    error(CangJieParsingBundle.message("parsing.error.function.body.expected"))
}
```

> **⚠️ P1 实施更正（相对本节初稿）**：初稿把签名写成 `reportMissingBody(owner: String)`，
> 且方法体忽略 `owner`、固定上报 `function.body.expected`。该写法有两个缺陷：
> ① `owner` 是死参数；② 会把 **P4 的属性诊断**（`unimplemented.abstract.property` +
> `missing prop body`）也塌缩成"Function body expected"，**丢失真实信息**
> （P4 的语义是"非抽象类里出现了无体属性"，与"函数体缺失"不是同一句话）。
> ⇒ 定稿改为**"抑制判定在唯一入口、诊断消息按 owner 传入"**：
> `reportMissingBody` 是唯一决定"要不要抑制"的地方（框架正确性的落点），
> 而消息文本本来就是 owner 相关的（与任何其它 `error(msg)` 调用无异）。
> 这样 P4 与 A 组共用同一个抑制入口，同时各自保留准确的措辞。

**第二步 —— 两类不同动作**：

| 类别 | 动作 |
|---|---|
| **A 组 7 处** | 改为调用 `reportMissingBody(...)` |
| **B 组 5 处** | **修正诊断语义**，改用各自真实缺失的诊断（缺标识符 / 缺参数列表），**不经过 `reportMissingBody`** |

**A 组 7 处（改为统一入口调用）**

| # | 解析函数 | 原诊断行 | 触发文本 | P1 落点 |
|---|---|---|---|---|
| 1 | `parsePropertyGet` | `:2838` | `prop p: Int64 { get() }` | `reportMissingBody()` |
| 2 | `parsePropertySet` | `:2879` | `prop p: Int64 { set(v) }` | `reportMissingBody()` |
| 3 | `parseMainFunc` | `:3777` | `main()` 无体 | **该分支已删除**，改为直接调用 `parseFunctionBody()` |
| 4 | `parseFunction` | `:3876` | `class C { func f(): Int64 }`（**最典型**，对应官方 P6） | `reportMissingBody()` |
| 5 | `parseInitFunctionBody` | `:4145` | `init(x: Int64)` 无体 | `reportMissingBody()` |
| 6 | `parseInitFunctionBody` | `:4137` | `init(): Type` 的 `COLON` 恢复路径 | **声明模式早退**（见下） |
| 7 | `parseFunctionBody` | `:4157` | 通用函数体缺失（被 #3/#4/#5 调用） | `reportMissingBody()` |

> **P1 实施更正（三处，均为实测后收敛）**
>
> **① 抑制入口仍是 1 个，但"上报点"从 7 收到 6。**
> 第 3 处（`parseMainFunc`）原本是
> `if (at(LBRACE)) parseFunctionBody() else error(...)` —— 与 `parseFunctionBody()` 内部
> `if (at(LBRACE)) parseBlock() else reportMissingBody()` **逐字等价**（差别只有消息文本）。
> ⇒ 直接塌缩为一次 `parseFunctionBody()` 调用，`main` 不再有自己的上报点。
> 这不是"顺手去重"：它让"缺体上报点"的数量与"体 owner 的数量"解耦 —— 新增 owner 只要调用
> `parseFunctionBody()` 就自动正确（J1）。
>
> **② 第 6 处必须是"早退"，不是"抑制 error"。**
> `parseInitFunctionBody` 的 `at(COLON)` 分支是一段**恢复逻辑**：
> ```kotlin
> val error = mark()
> while (!at(LBRACE)) advance()      // 一路 advance 到 '{'
> error.error(...)
> ```
> 在 `.cj.d` 中"体缺失"是常态、**不存在恢复目标 `{`**：若只抑制那行 `error` 而保留循环，
> 循环会一路 `advance()` 到 EOF **吞掉整个文件剩余内容**，且留下的 `mark()` 没有 `done/error`
> 使标记不平衡（正是本节末那段提示警告的后果，但比它更严重）。
> ⇒ 声明模式下必须**整块跳过**，并消费掉返回类型以免级联错误：
> ```kotlin
> if (at(COLON)) {
>     if (sourceKind.isDeclaration) {
>         advance()      // COLON
>         parseTypeRef() // 消费返回类型，保持后续成员对齐
>         return
>     }
>     ...原有恢复逻辑...
> }
> ```
> 推论：**"抑制诊断"与"跳过恢复"是两件事**。凡"抑制点"同时伴随恢复动作（`mark`+`advance` 循环），
> 都必须按"早退"处理，而不是包一层 `if`。
>
> **③ 文案变更范围比初稿声明的大：A 组 4 处 + B 组 5 处 = 9 处。**
> 初稿称"唯一改变文案的改动是 B 组 5 处"。实际统一后：
> `parsePropertyGet` / `parsePropertySet` / `parseFunction` / `parseMainFunc` 四处
> 由 `Expecting '{'` 变为 `Function body expected`（`main` 因塌缩而并入）；
> **P4 的属性诊断（`unimplemented.abstract.property` + `missing prop body`）保持原样**，
> 因为它们表达的是"非抽象类里出现了无体属性"，与"函数体缺失"不是同一句话（见第一步的更正说明）。
> ⇒ 提交时的改动前后对照应覆盖这 9 处。

**B 组 5 处（修正诊断语义，不豁免）**

| # | 解析函数 | 现诊断行 | 触发文本 | 应改为 | 实测结果 |
|---|---|---|---|---|---|
| 1 | `parsePrimaryInitFunc` | `CangJieParsing.kt:3595` | （**不可达**，见下） | 缺参数列表 | 防御分支，文案改为 `Expecting '('` |
| 2 | `parseInitFuncRest` | `CangJieParsing.kt:3642` | `class C { init }` | 缺参数列表 | ✅ `Expecting '('` |
| 3 | `parseMainFunc` | `CangJieParsing.kt:3755` | `main }` | 缺参数列表 | ✅ `Expecting '('` |
| 4 | `parseFunction` | `CangJieParsing.kt:3815` | `class C { func }` | 缺标识符 | ✅ `Expecting identifier` |
| 5 | `parseMacro` | `CangJieParsing.kt:4089` | `@Foo` + `macro }` | 缺标识符 | ✅ `Expecting identifier` |

> **P1 实测更正两处（初稿的"触发文本"列有误）：**
>
> **① `:3595` 是防御分支，不可达。** `parsePrimaryInitFunc` 的唯一调用方要求
> `at(IDENTIFIER) && lookahead(1) == LPAR`（`parseMemberDeclarationRest`），而 `lookAhead(k)` 的语义是
> "跳过空白后的第 k 个 token"（`SemanticWhitespaceAwarePsiBuilderImpl.lookAhead` → `PsiBuilderImpl.lookAhead`），
> 因此 `advance()` 消费标识符之后**必然是 `LPAR`**，`at(RBRACE)` 不成立。
> 初稿写的触发文本 `class C }` 在语义上也不成立（`class C }` 实际由"缺 `{` 或继承列表"报出）。
> ⇒ 保留该分支（防御直接调用），文案改为 `Expecting '('`，但**不要为它编写用例**（无法触发）。
>
> **② `:4089` 的触发文本不能是裸 `macro }`。** `macro package <name>` 是**合法的文件前导形式**，
> `parsePackageDirective`（`:697-703`）会把处于文件首位的 `macro` 当作宏包声明头消费掉，
> 随后报 `Expecting 'package' keyword`，**根本到不了 `parseMacro`**。
> 要进入宏声明分支，`macro` 前面必须有内容（注解或 package 行）。
> ⇒ 用例输入用 `@Foo\nmacro }`。
>
> 这两处都不是"文档写作不严谨"，而是**初稿的输入假设没有跑过**。P1 的测试因此同时起到了
> "验证抑制"与"验证文档断言"两个作用 —— 建议后续阶段沿用这个做法。

> **这是本设计唯一改变既有错误文案的改动**，也正是它值得做的原因：
> 修正后，"缺体"与"残缺输入"在诊断层面就分开了，抑制逻辑才可能收敛到一个点。
> ⚠️ 建议**单独提交、单独评审**，并在提交信息中给出改动前后对照，避免被误认为无关的顺手改动。

##### 为什么这是框架正确而非多此一举

| 判据 | 逐点加 `if`（v2 路线） | 统一入口（v3） |
|---|---|---|
| 新增一个无体 owner（如新的 `operator func` 形态） | 需重新判断"属于哪一组、要不要豁免" | **只要走 `reportMissingBody` 就自动正确** |
| B 组被误豁免的风险 | 依赖评审者的记忆力与注释 | **结构上不可能**（不经过入口） |
| 抑制点数量 | 7 | **1** |

##### P2 / P3 / P5 在本仓不适用（结论与证据）

**P2 / P3 —— 本仓 psi 层不存在对应诊断**

实测：`psi/resources/messages/CangJieParsingBundle.properties` 中与初始化相关的键**只有一个** ——
`parsing.error.field.requires.type.or.initializer`，其唯一调用点为 `CangJieParsing.kt:2297`，
语义是"**类成员字段**必须带类型注解或初始化器"，与官方的"const 缺初始化 / 顶层 var 缺初始化"不是一回事。

⇒ P2/P3 在本仓由**语义层**承担，落点是 checker 内部过滤（见 4.4.5）：
`CfirConstVariableInitializerChecker`（const 变量）、`CfirFileStaticGlobalInitializationChecker`（文件级静态 / 全局初始化）。

⇒ `parsing.error.field.requires.type.or.initializer`（`:2297`）**保持不豁免**：官方 P2/P3 未覆盖该语义，
且 `.cj.d` 中 `class C { let x: Int64 }` 本身就带类型、不触发它；无类型无初始化的字段在 `.cj.d` 中同样非法。

**P5 —— `HAS_BROKEN`：N/A**

`psi` 中不存在 `HAS_BROKEN` 概念（既无属性位也无对应降级路径）⇒ 无落点，也不会触发"破损节点"降级。

##### 其它 `expecting.symbol "{"` 上报点（不属于缺体语义，不豁免）

| 行 | 归属 | 处置 |
|---|---|---|
| `CangJieParsing.kt:635` | `expect(LBRACE, ...)` | 通用块起始，与声明体无关 |
| **`CangJieParsing.kt:3004`** | `parseEnumBody()` | **缺 enum body 的 `{`**（`enum E }`）。B-2 复核补入 |
| `CangJieParsing.kt:3994` | `parseSynchronized` | `synchronized` **表达式**必须有块 |
| `CangJieParsing.kt:4021` | `parseForeign` | `foreign { }` 块体必须有 `{}` |
| `CangJieParsing.kt:4124` | `parseMacro` 宏体 | **独立待核事项**：语法注释写 `functionBody?`（可选）但实现要求必有体。该矛盾与声明模式无关，本特性不放开、也不解决 |

> **B-2 更正：排除清单初稿只有 4 条，漏了 `:3004`（实际 5 条）。**
> 全量复核命令：
> ```bash
> grep -n "parsing.error.function.body.expected\|parsing.error.expecting.symbol" \
>   psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt
> # 期望：function.body.expected 7 处（3595/3642/3755/3815/4089/4145/4157）
> #       expecting.symbol "{" 10 处（635/2838/2879/3004/3777/3876/3994/4021/4124/4137）
> #       合计 17 处 = A 组 7 + B 组 5 + 排除 5
> ```
> **实际影响有限**：`:3004` 用的是 `expecting.symbol`（不是 `function.body.expected`），
> 因此不会被错误地路由进 `reportMissingBody`，不会造成误豁免。
> 但它必须进清单 —— 因为 F1 的整个论点是"**不穷举就无法收敛为一个点**"，
> 而"穷举"的完整性正是**通过计数自检来保证的**（17 处对不上就说明有遗漏）。

##### 改动示意

```kotlin
// parseFunction:3872-3877
} else if (
    !(isInterfaceMethod || (detector?.isAbstractDetected == true)) &&
    (detector?.isForeignDetected != true)
) {
    reportMissingBody("function")            // ← 抑制逻辑只存在于该方法内部
}
```

```kotlin
// parseProperty:2779-2782（与函数的不对称仍然保留）
} else if (!isInterface) {
    if (classdetector != null && !classdetector.isAbstractDetected) {
        reportMissingBody("property")
    }
}
// class/interface 作用域下"无 {} 即 abstract"的行为保持不变（对齐 R4 / Parser.cpp:371-390）
```

> **函数与属性的不对称仍然必须保留**（R3 vs R4）：
> `reportMissingBody` 只统一"**诊断**抑制"，不改变"**是否打 `abstract`**"的判定。
> 后者在 CFIR 层单独处理，且**只作用于函数分支**（见 4.5.2 护栏 1）。

#### 4.2.7 `CjFileElementType` 与 Stub

`.cj.d` 复用 `CjFileElementType`（Stub 类型），因此会正常建 Stub 索引，`.cj.d` 内部的符号导航/补全/Find Usages 可用。

- `CangJieFileStubKind` **不需要**新增 `Declaration` 种类：当前 `WithPackage.File` 已足够表达；是否分类由 `CjFile.isDeclarationFile` 承担。
- **Stub 版本无需升级**：本次不改变任何节点或字段的序列化布局。

> 参考实现对声明文件使用**非 Stub** 的 `IFileElementType`（`CangjieDeclarationParserDefinition.java:165-178`）以避免建索引。本仓选择建索引，因为 `.cj.d` 需要完整的导航与补全能力（见 4.9.3）。**这是一个有意的差异**（`DIFF-3`），理由是本仓的 `.cj.d` 使用场景更偏向"可编辑的 SDK 头文件"，需要索引。
> **评审关注点（v2：已完成双向处置）**：若 `.cj.d` 被放入源码根，Stub 索引会与 `.cjo` 产生同名符号。处置为：
> ① **逻辑侧** —— `.cj.d` 不进入声明提供者聚合（D7，见 4.9.3）；
> ② **索引侧** —— 对源码根下 `*.cj.d` 加排除（4.9.3 "v2 新增缺口"）。
> v1 只做了 ①，且把出处误写成 4.9.2（正确为 4.9.3）。

#### 4.2.8 P1 实施记录（实测）

本节记录 P1 落地时相对设计的**实测差异**。设计稿其余部分与该实现一一对应，未列出的都是照稿实现。

**改动落点（5 个文件）**

| 文件 | 改动 |
|---|---|
| `psi/.../parsing/CangJieParsing.kt` | 构造属性 `sourceKind`（+ companion 三个工厂加参）；`reportMissingBody` 两个重载；A 组落点；B 组 5 处文案；`parseInitFunctionBody` 的声明模式早退 |
| `psi/.../parsing/CangJieParser.kt` | 读 `psiFile.sourceKind` 随构造注入；入口改为对 `sourceKind` 的**穷尽 `when`**；删除 `name.endsWith(".cj.macrocall")` 与不再使用的 `CjMacroCallFile` import |
| `psi/.../parsing/CangJieLightParser.kt` | `parse` / `parseAnnotationOnly` 各加 `sourceKind`（默认 `SOURCE`）；`parseWith` 透传 |
| `cfir/raw-cfir/light-tree2cfir/.../LightTree2Cfir.kt` | `buildCfirFileWithSurfaces(code, sourceFile, …)` 传 `sourceFile.sourceKind` |
| `psi/test/.../psi/DeclarationFileParsingTest.kt`（新增） | 19 个声明模式用例 + 6 个反向用例 |

**实测补充的事实（设计稿没有的）**

1. **宏调用文件的入口是独立的 ParserDefinition，`name.endsWith` 兜底本来就是冗余的。**
   `CangJieMacroCallParserDefinition.FILE_ELEMENT_TYPE.doParseContents` 走
   `parse(builder, psi.containingFile)`，而它自己的 `createFile` 返回 `CjMacroCallFile`
   （`sourceKind` 覆写为 `MACRO_CALL`）⇒ C-1 的"收敛到 `sourceKind`"是**等价替换**，不是能力削减。
   ⇒ 非 `CjFile` 的兜底用 `CjSourceKind.fromFileName(psiFile.name)`（而不是硬编码 `SOURCE`），
   把原有的"按名字识别"能力搬进唯一真源，而不是丢掉它。

2. **`parseMacro` 的分派包在局部 `with` 里** —— 这是"模式不能放进 `ParsingContext`"最直接的一条现场证据：
   ```kotlin
   // CangJieParsing.kt:2568-2571
   return with(parseContext.copy(disableMacroParsing = false, allowParseAnnotationsInValueParameter = false)) {
       parser.parseMacro()
   }
   ```
   `parseContext.copy(...)` **整体替换**上下文对象。若把 `isDeclarationFile` 放进 `ParsingContext`，
   存进这里会被静默清掉、**没有任何编译期或运行期信号**。4.2.3 的论证由此从"推理"变成"实测"。

3. **诊断点计数在改动后的实测值**（自检用）：
   ```bash
   grep -n "parsing.error.function.body.expected\|parsing.error.expecting.symbol\"" \
     psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt
   # function.body.expected：1 处 —— 只在 reportMissingBody 的默认重载里
   # expecting.symbol "{"：6 处 —— 635 / 3004 / 3994 / 4021 / 4124（排除清单 5 处）+ 4137（恢复路径）
   ```
   ⇒ 改动前 17 处 → 改动后 7 处，且**只有 1 处与"缺体"语义绑定**。
   这个数字是回归防线：若将来又出现第 2 处 `function.body.expected`，说明有人绕开了统一入口。

**验收结果（实测）**

| 项 | 结果 |
|---|---|
| `DeclarationFileParsingTest`（声明模式） | **19 / 19 通过** |
| `DeclarationModeReverseParsingTest`（`.cj` 反向） | **6 / 6 通过** |
| 命令 | `:psi:test --tests "*DeclarationFileParsingTest*" --tests "*DeclarationModeReverseParsingTest*"` |

覆盖：无体函数 / 接口函数 / 无返回类型 / 顶层函数 / `main` / 属性 / 访问器 / 构造 / 主构造 / finalizer
在 `.cj.d` 下**零 `PsiErrorElement`**；同样文本在 `.cj` 下**必须报错**；
残缺输入（`func }` / `init }` / `main }` / `@Foo macro }`）在 `.cj.d` 下**仍报错且报修正后的文案**；
残缺输入之后的成员**不被吞掉**（恢复路径必须停下）；无体访问器仍产出 `CjPropertyAccessor` 结构。

> **未覆盖（诚实清单）**：P1 只验证了 **PSI 路径**。LightTree 路径的声明模式（`CangJieLightParser.parse(sourceKind=…)`
> → `LightTree2Cfir`）已接线，但其**行为**验证属于 P2 的测试落点（CFIR 护栏测试的 AST/Stub 双变体），
> 本阶段只保证"参数可传、既有调用点零改动"。

---

### 4.3 `compiler/config` 与 `compiler/arguments`

#### 4.3.1 新增配置项

文件：`compiler/config/src/org/cangnova/cangjie/config/CommonConfigurationKeys.kt`（修改）

```kotlin
/** 是否为 `.cj.d` 声明模式：只收集 `.cj.d`，只产出 `.cjo`，不做 CHIR / 代码生成。 */
@JvmField
val COMPILE_CJD = CompilerConfigurationKey.create<Boolean>("COMPILE_CJD")
```

```kotlin
/** 声明模式开关，对齐官方 `GlobalOptions.compileCjd`。 */
var CompilerConfiguration.compileCjd: Boolean
    get() = getBoolean(CommonConfigurationKeys.COMPILE_CJD)
    set(value) { put(CommonConfigurationKeys.COMPILE_CJD, value) }
```

#### 4.3.2 新增命令行 flag

文件：`compiler/arguments/src/org/cangnova/cangjie/arguments/description/compilerArguments.kt`（修改）

按现有 `verbose` 模板（同文件 `:21-27`）追加：

```kotlin
compilerArgument {
    name = "d"
    // v2：显式指定生成属性名，避免生成 `var d`（可读性差且与配置侧命名不一致）
    compilerName = "compileCjd"
    description = "Compile declaration file(s) (.cj.d)".asReleaseDependent()
    argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
    valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
    lifecycle(CangJieReleaseVersion.V_1_0_5)
}
```

**v2 已确认 `compilerName` 是受支持的字段，且语义正是"生成类中的属性名"**（v1 写的是"以支持为准"，属未闭环表述）：

- 字段存在：`CangJieCompilerArgument.kt:55`（构造参数）、`:137`（builder 属性）、`:192`（`build()` 透传）。
- 生成器消费点：`Generator.kt:127-132`
  ```kotlin
  /** 计算参数在生成类中的 Kotlin 属性名。 */
  fun CangJieCompilerArgument.calculateName(): String = compilerName ?: name
      .removePrefix("X").removePrefix("X")
      .split("-").joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
  ```
- 唯一消费点：`Generator.kt:105-106` 的 `generateProperty`（生成"可冻结检查的可变属性"）。
- ⚠️ 一个容易踩的细节：由于 `?:` 的绑定优先级低于 `.`，上面那串 `removePrefix`/`split`/`joinToString`
  **只作用于 `name` 分支，不作用于 `compilerName`**。因此 `compilerName` 会被**原样**用作属性名
  ⇒ 写 `compilerName = "compileCjd"` 就得到 `var compileCjd: Boolean`，**不会**被 kebab 转换二次加工。
- ⇒ 生成的 `CommonCompilerArguments` 应为 `var compileCjd: Boolean`（而非 `var d: Boolean`），
  这样 4.3.3 的桥接就是一行自然的 `configuration.compileCjd = arguments.compileCjd`。

> **v2 更正 —— 不存在漂移，v1 的"历史债"判断有误**。
>
> v1 称"`gen` 里有 `useFir`，DSL 里没有对应声明"。实测：
> - `useFir` **不在** CLI 参数生成物里，它是 `compiler/config/src/.../CommonConfigurationKeys.kt:178-180`
>   的 `CompilerConfiguration.useFir` 扩展属性（配置层，与命令行解析无关）。全仓 `useFir` 命中仅此一处（含 `bin/` 副本）。
> - 生成物 `compiler/frontend/gen/.../CommonCompilerArguments.kt` 有 **6** 个属性，DSL 声明 **4** 个
>   （`language-version` / `verbose` / `report-perf` / `dump-perf`）。差额 2 个是生成器**主动合成的**
>   `autoAdvanceLanguageVersion` / `autoAdvanceApiVersion`（`compiler/frontend-arguments-generator/src/.../Main.kt:116`
>   的 `additionalSyntheticArguments = listOf("autoAdvanceLanguageVersion", "autoAdvanceApiVersion")`）。
> - ⇒ **DSL 与生成产物一致**。新增 `-d` 是安全的增量：生成后属性数应为 6 + 1 = 7。
>
> **仍然保留的动作**：新增 `-d` 后跑一次生成任务并用 `git diff` 确认**只多出 `d` 一个属性**（这是防止生成器版本错配的便宜保险），
> 但**不再需要**把它当作"先修历史债"的前置阻塞项。生成任务接线见 `compiler/frontend/build.gradle.kts:20-31`（`:compiler:frontend-arguments-generator`）。

#### 4.3.3 参数 → 配置的桥接

本仓**当前不存在** `ArgumentsPipelineArtifact → ConfigurationPipelineArtifact` 的转换阶段（`AbstractFrontendPipeline.kt:12-41`，无子类）。

因此本设计**不新增**通用桥接阶段，而是要求任何声明模式的调用方**显式**写入配置：

```kotlin
configuration.compileCjd = arguments.compileCjd
```

调用方清单：`intellij-ide` toolchain 委托、LSP 入口、测试夹具。这比"补一个半成品通用流水线"更可控（D9）。

---

### 4.4 `compiler/frontend`：源收集与声明模式

#### 4.4.1 统一的源收集过滤器

**三值穷尽，互斥分流**（R1）：

```text
sourceKind == SOURCE       && !compileCjd  → 收集
sourceKind == DECLARATION  &&  compileCjd  → 收集
其它组合（含 MACRO_CALL 的全部组合）        → 拒绝
```

> 注意两点：
> ① `compileCjd == true` 时，`.cj` 必须被**拒绝**，而不是"顺便也收下"（R1 是互斥输入）；
> ② **`MACRO_CALL` 恒被拒绝** —— 它是"能被识别的文件种类"，不是"能作为输入的源"。
> 这一点必须写成**显式规则**（4.4.2 的 `when` 里有一支专门返回 `false`），
> 不能依赖"它的扩展名不是 `cj`，所以恰好落进 else 被拒"——那是巧合，不是规则。

#### 4.4.2 LightTree 路径

文件：`compiler/frontend/src/org/cangnova/cangjie/frontend/sources/GroupedCjSources.kt`（修改，`:114-151`）

**第一步：把"种类判定"抽成一份实现**（`filter` 与 `convertToSourceFiles` 共用；**不得抄成两份**，3.1 原则 5）：

```kotlin
/**
 * 判定文件的源种类。
 *
 * ⚠️ 不能用 `VirtualFile.extension`：它对 `a.cj.d` 返回 `"d"`、对 `a.cj.macrocall` 返回 `"macrocall"`，
 * 必须用完整文件名做字面后缀匹配（R2）。
 *
 * 先看 FileType（精确），再回退到文件名（覆盖 IDE 的 LightVirtualFile、测试夹具等未注册类型的场景）。
 */
private fun VirtualFile.sourceKind(): CjSourceKind = when {
    fileType is CangJieDeclarationFileType -> CjSourceKind.DECLARATION
    fileType is CangJieMacroCallFileType -> CjSourceKind.MACRO_CALL
    fileType is CangJieFileType -> CjSourceKind.SOURCE
    else -> CjSourceKind.fromFileName(nameSequence.toString())
}
```

**第二步：`filter` —— 按种类互斥分流（R1）**：

```kotlin
filter = { virtualFile, isExplicit ->
    when {
        virtualFile.extension == "java" -> false

        // 仓颉家族：三值穷尽，无 else —— 新增种类时编译器会强制在此表态
        virtualFile.isCangJieFamily() -> when (virtualFile.sourceKind()) {
            CjSourceKind.SOURCE -> !isDeclarationMode
            CjSourceKind.DECLARATION -> isDeclarationMode
            CjSourceKind.MACRO_CALL -> false          // 宏调用文件永不作为源（4.4.1）
        }

        // ↓ 既有 else 分支：逐字保留，【不得简化】
        else -> {
            if (virtualFile.isFile) {
                ensurePluginsConfigured()
                val isCangJie = virtualFile.fileType == CangJieFileType.INSTANCE
                if (isExplicit && !isCangJie) {
                    compilerConfiguration.messageCollector.report(
                        org.cangnova.cangjie.messages.CompilerMessageSeverity.ERROR,
                        "Source entry is not a Cangjie file: ${virtualFile.path}",
                    )
                }
                isCangJie
            } else {
                false
            }
        }
    }
}
```

> ⚠️ **A-3 更正一：初稿的伪代码抹平了 `else` 分支的副作用。**
> 初稿把 else 写成 `else -> { /* 保持既有 else 分支：非 .cj 一律拒绝 */ }`，
> 但现状的 else 分支**除了判 `isCangJie` 之外还有两个副作用**：
> `ensurePluginsConfigured()`（**惰性初始化插件**）与 `isExplicit && !isCangJie` 时的
> `"Source entry is not a Cangjie file: <path>"` **ERROR 上报**。
> 伪代码把这两者抹平了 —— **照抄会静默改变行为**（用户显式指定的非仓颉源文件不再报错）。
> ⇒ 上面给出的版本是**逐字保留**的。

**第三步：`convertToSourceFiles` —— 让声明文件走直连分支**：

```kotlin
convertToSourceFiles = { virtualFile ->
    val sources = listOf<CjSourceFile>(CjVirtualFileSourceFile(virtualFileCreator.create(virtualFile)))
    if (virtualFile.extension == CangJieFileType.EXTENSION || virtualFile.sourceKind().isDeclaration) {
        sources                                       // 仓颉家族（.cj / .cj.d）直连
    } else {
        applyCfirProcessSourcesExtension(
            environment = projectEnvironment,
            configuration = compilerConfiguration,
            findVirtualFile = ::findVirtualFile,
            sources = sources,
        ) ?: sources
    }
}
```

> ⚠️ **A-3 更正二：初稿在此处的改法是一个空操作，达不到它自己声明的目的。**
>
> 初稿原文：「`convertToSourceFiles` 分支的 `virtualFile.extension == CangJieFileType.EXTENSION` 判断
> 要改成 `sourceKind == SOURCE`，否则声明文件会误入 `applyCfirProcessSourcesExtension` 插件扩展路径。」
>
> 但把条件换成 `sourceKind == SOURCE` 之后的**实际行为**是：
>
> | 输入 | `sourceKind` | 新条件 | 走向 |
> |---|---|---|---|
> | `a.cj` | SOURCE | `true` | 直连（与现状相同） |
> | `a.cj.d` | DECLARATION | **`false`** | **仍然进 `applyCfirProcessSourcesExtension`** ✗ |
>
> ⇒ 改完之后 `.cj.d` 的走向**与改之前一模一样**，"把声明文件带离插件扩展路径"的目的完全没有达到。
> 原因是逻辑方向错了：要表达的是"**这是仓颉家族文件，直连**"（或用或），
> 而不是"**这不是实现文件，走扩展**"。
>
> **正解**就是上面那句 `ext == "cj" || sourceKind().isDeclaration` ——
> 它同时覆盖 `.cj`（扩展名）与 `.cj.d`（种类），且 `applyCfirProcessSourcesExtension` 只服务真正的"非仓颉"扩展机制。
> 注意 `sourceKind()` 在这里是**第二次调用**，但它是**同一份实现**（第一步抽出的那一个），
> 不构成"同一事实多个来源"；反过来说，**这也是不允许把条件抄成两份的原因**。

要点回顾：

- 必须用 `nameSequence`（完整文件名）而非 `extension`（最后一段）做后缀判定（R2）。
- `MACRO_CALL` 在 `filter` 中被显式拒绝（`false`），不是"默认掉进 else 恰好被拒"——
  前者是**声明过的规则**，后者是**巧合**，两者在新增种类时的表现完全不同。

#### 4.4.3 PSI 路径

文件：`compiler/frontend/src/org/cangnova/cangjie/frontend/environment/coreEnvironmentUtils.kt`（修改，`:57-74`）

同样的两级判定，并且：

```kotlin
// 现状
if (virtualFile.extension != CangJieFileType.EXTENSION) { ensurePluginsConfigured() }
```

需要改为"既不是 `.cj` 也不是 `.cj.d` 时才 `ensurePluginsConfigured()`"，否则合法输入会被迫触发一次插件配置。

`convertToSourceFiles` 分支里 `virtualFileCreator.create(it)` 产出的 `CjPsiSourceFile`，其 `sourceKind` 会自动从 PSI 标记读出来（4.1.2），因此下游无需再判定。

#### 4.4.4 声明模式的 Phase 裁剪

对齐 R5（`CjdCompilerInstance`）。本仓的等价物是：

| 官方被裁阶段 | 本仓对应 | 处置 |
|---|---|---|
| `PerformDesugarAfterSema` | `CfirResolvePhase.BODY_RESOLVE` 之后的 desugar | 声明模式下不启动 |
| `PerformGenericInstantiation` | 无直接对应（CFIR 是惰性解析） | — |
| `PerformOverflowStrategy` | 溢出策略配置 | 不应用 |
| `PerformCHIRCompilation` | `chir:cfir2chir` 入口 | **不调用** |
| `PerformCodeGen` | `compiler:codegen` / `compiler:jvm-codegen` | **不调用** |
| `PerformCjoAndBchirSaving` → 只 `SaveCjo` | `CjoPackageWriter` | 声明模式只走 `.cjo`，不产 bchir |

> 由于本仓 `.cjo` 由外部 `cjc` 产出（D9），上表主要影响的是"IDE/LSP 直接分析 `.cj.d` 时不启动 CHIR / codegen"。**实现方式**：不新增 `PipelinePhase`（`PipelinePhase.outputIfNotEnabled` 会直接抛异常，见 `PipelinePhase.kt:68-75`），而是在调用方（IDE/LSP 入口）用 `configuration.compileCjd` 显式控制。

#### 4.4.5 声明模式下的 Sema 豁免：**框架级标记**，而非逐 checker 打补丁

**判定规则**：官方对声明模式的 Sema 豁免是**白名单** —— 只有 1.2.5 表中的 6 处提前返回，其余**全部照跑**。
因此每个 checker 的处置都可机械判定：

| 判定 | 规则 |
|---|---|
| **豁免** | 该 checker 的语义落在官方 6 类之内（它检查的是"实现的完备性"） |
| **保留** | 不在 6 类之内 —— 官方没豁免的，本仓一律不豁免 |

##### 实现机制：给 checker 加一个分类维度

**问题**：`DeclarationCheckers` 是 `abstract class`，
槽位**全部按"声明种类"划分**（basic / callable / function / classLike / property / constructor / extend …），
注册又是**集合粒度**（`useCheckers(CommonDeclarationCheckers)`，见 `CheckersContainers.kt`）。
而"实现完备性检查 vs 接口 / 类型检查"是**另一个正交维度**，框架没有表达它
⇒ 想按该维度关掉一批 checker，粒度对不上，只能下沉到每个 checker 内部 early-return。

**v3 方案**：把这个维度**加进框架**。

> ⚠️ **A-4 更正一：本仓不存在 `CfirChecker` 接口，标记要加在真实基类上。**
>
> v3 初稿写的是 `interface CfirChecker { val requiresImplementation: Boolean get() = false }`，
> **该类型在本仓不存在，代码无法编译**。实测（`grep -rln "CfirChecker" --include=*.kt cfir analysis compiler`）：
> 只有 2 个文件命中，且都不是类型定义 —— `cfir/checkers/checkers-component-generator/src/.../Generator.kt:191-193`（注释）
> 与 `cfir/resolve/src/.../ResolutionStage.kt:10`（KDoc 里提到 `CfirCheckerSink`）。
>
> 真实的 checker 基类是 **4 个相互独立的 `abstract class`**：
>
> | 基类 | 路径 |
> |---|---|
> | **`CfirDeclarationChecker<D : CfirDeclaration>`** | `cfir/checkers/src/.../checkers/declaration/CfirDeclarationChecker.kt:13` |
> | `CfirExpressionChecker<E : CfirStatement>` | `cfir/checkers/src/.../checkers/expression/CfirExpressionChecker.kt:8` |
> | `CfirTypeChecker<T : CfirTypeRef>` | `cfir/checkers/src/.../checkers/type/CfirTypeChecker.kt:8` |
> | `CfirLanguageVersionSettingsChecker` | `cfir/checkers/src/.../checkers/config/CfirLanguageVersionSettingsChecker.kt:7` |
>
> **好消息：待标记的 11 个 checker 全部是 declaration checker**（生成物的分派签名就是
> `Array<CfirDeclarationChecker<E>>.check(...)`，`E : CfirDeclaration`），
> 因此属性只需要加在 **`CfirDeclarationChecker<D>`** 这一个基类上即可覆盖全部 11 项。
> 若将来出现 expression / type 级别的实现完备性检查，再按需加到对应基类。

```kotlin
// cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirDeclarationChecker.kt
abstract class CfirDeclarationChecker<D : CfirDeclaration> {
    /**
     * 该 checker 是否检查"实现的完备性"——即依赖函数体 / 初始化器 / 访问器实现的存在性。
     *
     * 声明文件（`.cj.d`）中的声明**没有体**，因此这类检查整体跳过，
     * 对齐官方 Sema 的豁免（1.2.5）。
     *
     * 默认 false：新增 checker **默认在所有模式下参与**，其作者无需知道 `.cj.d` 的存在。
     */
    open val requiresImplementation: Boolean get() = false

    // ... 既有成员
}
```

> ⚠️ **A-4 更正二：分派处是生成代码，过滤必须落在生成器里。**
>
> v3 初稿说"分派处统一过滤（唯一的豁免开关）"，但没有指出该分派处**是生成物**，
> 也没在全文任何位置提及生成器 —— 7.4 的核对项会把施工者直接引向一个写着"请勿手动修改"的文件。
>
> 唯一的迭代点（`cfir/checkers/gen/.../declaration/DeclarationCheckersDiagnosticComponent.kt:140-157` 末尾）：
>
> ```kotlin
> private inline fun <reified E : CfirDeclaration> Array<CfirDeclarationChecker<E>>.check(
>     element: E,
>     context: CheckerContext
> ) {
>     for (checker in this) {
>         try {
>             context(context, reporter) { checker.check(element) }
>         } catch (e: Exception) { ... }
>     }
> }
> ```
>
> 该文件第 15-18 行明写「**本文件由生成器自动生成 / 请勿手动修改**」。
> 生成来源：`cfir/checkers/checkers-component-generator`，由 `cfir/checkers/build.gradle.kts` 的
> `generatedDiagnosticContainersAndCheckerComponents()` 挂载。
> 模板位置：**`Generator.kt:391-413` 的 `printDiagnosticComponentCheckMethod()`**（它 `println` 出上面这段）。
>
> ⇒ 改动是**在生成器模板里加一行 `continue`**，然后重跑生成任务；**不得手改 `gen/**`**。
> 同类事实：`DeclarationCheckers`（19 个槽位 + `EMPTY` 伴生 + 每槽位的 `allXxxCheckers` 展开数组）
> **也不在 `src/`**，而在 `cfir/checkers/gen/org/cangnova/cangjie/cfir/analysis/checkers/declaration/DeclarationCheckers.kt`，
> 同样是生成物。（v3 初稿把它写成 `cfir/checkers/src/.../declaration/DeclarationCheckers.kt:13` —— 路径错误。）

**唯一豁免开关（生成器模板内）**：

```kotlin
for (checker in this) {
    // 声明文件里不存在"实现"，依赖实现完备性的 checker 整体跳过。
    if (checker.requiresImplementation && element.isFromDeclarationFile(context)) continue
    try {
        context(context, reporter) { checker.check(element) }
    } catch (e: Exception) { ... }
}
```

> ⚠️ **A-4 更正三：判定依据不能是 `configuration.compileCjd`，必须问"这个文件是什么"。**
>
> v3 初稿写的是 `if (configuration.compileCjd && checker.requiresImplementation) return`。
> `compileCjd` 是**编译（会话）级开关** —— 它回答的是"**本次编译**是不是声明模式"；
> 而 checker 需要回答的是"**这个声明**所在文件是不是声明文件"。
> 二者的差别在 **IDE / LSP 场景**下是致命的（正是 C-2 指出的"官方不存在的模式"）：
>
> | 场景 | `compileCjd`（会话级） | 文件种类（每文件） |
> |---|---|---|
> | 形态 A：`cjc -d` 编译 `.cj.d` | `true`，全局成立 | 一致 ✓ |
> | IDE：`.cj` 源码 + SDK 的 `.cj.d` **同时可见** | **无法表达**（一个布尔值说不出"哪些文件是"） | 逐文件成立 ✓ |
>
> 并且 `CfirSession.Kind` 也**不能**用来代替 —— 它是 `{ Source, Library }`（"CFIR 从哪来"），
> 与声明模式**正交**：`cjc -d` 编译的 `.cj.d` 是 `Kind.Source`，与普通 `.cj` 编译完全一样。
> 拿 `Kind` 当判据会把"读 `.cjo` 的库会话"与"声明模式"混为一谈，**改变既有行为**。
>
> **正解：读元素所属文件的种类**，而本仓已有现成的回指路径（全部已实测）：
>
> ```text
> CheckerContext.containingFileSymbol : CfirFileSymbol?          // CheckerContext.kt:79
>   └─ CfirFileSymbol.sourceFile : CjSourceFile?                 // CfirBasedSymbol.kt:748-753
>        └─ (CjSourceFile as? CjSourceKindCarrier)?.sourceKind   // 4.1.2 / 4.1.3
> ```
>
> ```kotlin
> /** 该元素是否来自 `.cj.d`。取自元素自身的来源，不读任何会话级开关。 */
> internal fun CfirElement.isFromDeclarationFile(context: CheckerContext): Boolean =
>     (context.containingFileSymbol?.sourceFile as? CjSourceKindCarrier)?.sourceKind?.isDeclaration == true
> ```
>
> 这样做的三个好处：
> ① **与解析层同源**（都是 `CjSourceKind`），不存在"解析层判 `.cj.d`、检查层判 `compileCjd`"的分叉；
> ② **天然支持混合会话** —— 不需要为 C-2 额外设计"谁提供权威声明"的开关；
> ③ 不需要给 `CfirSession` / `CheckerContext` 新增任何字段。
>
> 依赖可达性：`cfir/checkers` 已 `api(project(":psi"))`（`build.gradle.kts:10`），`psi → common`，
> 故 `CjSourceKindCarrier` 可直接使用。

**A. 需要标记 `requiresImplementation = true` 的 checker（11 个）**

| checker | 所属槽位 | 官方依据 | 说明 |
|---|---|---|---|
| `CfirConstVariableInitializerChecker` | `callableDeclarationCheckers` | `InitializationChecker.cpp:503` | const 变量缺初始化（**4.2.6 中 P2 的实际落点**） |
| `CfirFileStaticGlobalInitializationChecker` | `fileCheckers` | `InitializationChecker.cpp:503` + P3 语义 | 文件级静态 / 全局初始化（**P3 的实际落点**） |
| `CfirConstructorInitializationChecker` | `constructorCheckers` | `InitializationChecker.cpp:1517` | 构造函数初始化完整性 |
| `CfirClassLikeInitializationChecker` | `classLikeCheckers` | `InitializationChecker.cpp:1517` | 同上（类级侧面） |
| `CfirFunctionInitializationChecker` | `functionCheckers` | `InitializationChecker.cpp:1517` | 同上（函数级侧面） |
| `CfirNotImplementedOverrideChecker` | `classLikeCheckers` | `StructInheritanceChecker.cpp:830-842` | 未实现接口成员 |
| `CfirCommonPackageMainChecker` | `fileCheckers` | `TypeChecker.cpp:2215-2230` | **入口缺失**（注意：不是 main 签名检查） |
| `CfirPropertySemanticsChecker` | `propertyCheckers` | `DeclAttributeChecker.cpp:333-336` | 不要求 `var` 属性同时具备 getter/setter |
| `CfirPropertyAccessorDeclarationChecker` | `propertyAccessorCheckers` | `DeclAttributeChecker.cpp:333-336` | 同上（访问器侧面） |
| `CfirOpenMemberChecker` | `classLikeCheckers` | `DeclAttributeChecker.cpp:289` | "非抽象类中成员被标 abstract"、"abstract/open 成员可见性" |
| `CfirFinalizerDeclarationChecker` | `functionCheckers` | `DeclAttributeChecker.cpp:289` | "open 类中 finalizer" |

> ⚠️ **A-4 更正四：初稿把 A 表列成 12 项，其中 `CfirMemberBodyDeclarationChecker` 一项是错的。**
>
> 初稿称它「**本仓特有**（推导自 P6 / R3）… 官方在 parser 层就允许无体；本仓把'成员体'判断下沉到 checker，
> 故**必须显式豁免**，否则 `.cj.d` 的每个无体成员都会被它报错。**它不受白名单规则保护，属本仓必须自己识别的项**」。
> **三条判断全部需要修正：**
>
> **① 它并非"本仓特有"，官方有对应项。** 该 checker 的 KDoc 自己写着对齐
> `DeclAttributeChecker::CheckAttributesForPropAndFuncDeclInClass`；而该函数**开头第 5 行就是豁免**：
>
> ```cpp
> // external/cangjie_compiler/src/Sema/DeclAttributeChecker.cpp:284-289
> void DeclAttributeChecker::CheckAttributesForPropAndFuncDeclInClass(const ClassDecl& cd, Decl& member) const
> {
>     if (member.astKind != ASTKind::PROP_DECL && member.astKind != ASTKind::FUNC_DECL) { return; }
>     if (opts.compileCjd) { return; }        // ← 官方豁免在此
>     ...
>     if (invalidAbstract) { diag.Diagnose(member, DiagKind::sema_missing_func_body, ...); }
>     ...
> }
> ```
>
> **官方是按"函数"粒度豁免的，不是按"诊断"粒度**：这一个 `return` 同时关掉了
> `sema_missing_func_body`、`sema_invalid_member_visibility_in_class`、`sema_ignore_open`、
> `sema_finalizer_forbidden_in_class` 四条诊断。上表中的
> `CfirOpenMemberChecker` 与 `CfirFinalizerDeclarationChecker` 其实**同属这一个官方豁免**（故官方依据同引 `:289`）——
> 即 **1 个官方函数 ↔ 3 个本仓 checker**。初稿把它们拆成三行、各引不同行号，掩盖了这个对应关系。
>
> **② 它其实"自动沉默"，不需要打标记。** 实测其守卫：
>
> ```kotlin
> // cfir/checkers/src/.../declaration/CfirMemberBodyDeclarationChecker.kt:52-60
> override fun check(declaration: CfirMemberDeclaration) {
>     val member = declaration.bodyRequiredMemberInfo() ?: return
>     val owner = context.findClosestDeclaration<CfirClass>() ?: return
>     if (!member.status.isAbstract) return                      // ← 关键守卫
>     ...
> }
> ```
>
> `.cj.d` 的成员**函数**在护栏 1 生效后 `status.isAbstract == false` ⇒ **直接 return**；
> `.cj.d` 的成员**属性**虽仍是 abstract（R4），但其 owner 是 `interface` / abstract class，`invalidAbstract` 不成立
> ⇒ 同样不报（这也解释了为何 `.cj` 的 `interface I { prop p: Int64 }` 本来就不报）。
>
> **③ 因此"给它打标记"不仅是多余的，而且是有害的** —— 它会**掩盖护栏 1 的缺陷**：
> 若护栏 1 实现错了（`.cj.d` 的无体函数被误判为 implicit abstract），本 checker 会立刻报 `MISSING_FUNC_BODY`，
> 这正是最好的告警；一旦打了 `requiresImplementation`，这个告警会被静默吞掉。
> **它是本设计免费的回归探测器，必须保持启用。**
>
> ⇒ **处置**：A 表由 12 项改为 **11 项**；`CfirMemberBodyDeclarationChecker` **不打标记**，
> 并在 6.2 的 P2 用例里增加一条断言：**`.cj.d` 输入下不得出现 `MISSING_FUNC_BODY`**（该断言同时验证护栏 1）。

**B. 保持默认（`false`）的 checker —— 无需任何改动**

这是 v3 相对 v2 最重要的收益：**B 表不再需要"逐项确认不豁免"，因为默认就是不豁免**。

| 槽位 | 代表 checker | 保留理由 |
|---|---|---|
| `basicDeclarationCheckers` | `CfirAnnotationTargetChecker`、`CfirModifierChecker`、`CfirTypeConstraintsChecker` 等 | 注解 / 修饰符 / 泛型约束 —— 官方全部保留 |
| `mainFunctionCheckers` | `CfirMainFunctionSignatureChecker` | 官方豁免的是**入口缺失**，不是**签名** |
| `classLikeCheckers`（其余） | `CfirSupertypesChecker`、`CfirOverrideChecker`、`CfirValueTypeRecursiveChecker` 等 | 继承 / override / 可见性（R6 明确列举） |
| `extendCheckers`（全部） | `CfirExtendTargetLegalityChecker`、`CfirExtendOrphanRuleChecker` 等 | 官方只豁免"未实现接口成员"，extend 合法性检查全保留 |
| `typeParameterCheckers` / `valueParameterCheckers` / `fieldVariableCheckers` | `CfirTypeParameterBoundsChecker`、`CfirFieldVariableThisOrSuperInitializerChecker` 等 | 泛型约束、参数默认值、**类型不匹配**（注意：类型检查不是完整性检查） |
| `fileCheckers`（其余） | `CfirImportsChecker`、`CfirGeneralSemanticsChecker`、`CfirGenericInstantiationChecker` 等 | import 检查明确保留（R6） |
| `simpleFunctionCheckers` / `callableDeclarationCheckers`（其余） | `CfirFunctionOverloadChecker`、`CfirDefaultParameterChecker`、`CfirAnnotationArgNumberCallableChecker` 等 | 运算符 / 重载 / 默认参数 / 注解参数 |
| `typeAliasCheckers` / `anonymousFunctionCheckers` / `patternVariableCheckers` / `invalidDeclarationCheckers` | 全部 | 官方无豁免 |

> **为什么 B 表只列"代表 checker"而不是穷举**：v3 的机制是"**默认 `false` = 保留**"，
> 因此 B 表在**施工层面不需要**——不打标记的 checker 自动保留，无需逐个确认。
> 本表的用途仅剩"说明哪些类别的检查必须保留"，列代表即可。
>
> ⚠️ **但审计需要穷举清单**：v2 §4.4.5 的 B 表逐项列出了约 40 个 checker 及其保留理由，
> 用于"确认过它们不属于实现完备性检查"。**若需要出具审计证据，应回到 v2 的 B 表逐项核对**（v2 未删除）。
> 这是本稿有意做的精简，不是遗漏；在此显式记录，避免评审时误判。
>
> **自检方法（比查表更可靠）**：给 A 表的 11 个 checker 打上标记后，用
> `grep -rn "requiresImplementation" cfir/checkers/src` 与 `grep -rn "Checkers(" cfir/checkers/src` 对照，
> 确认没有第 13 个"实际上依赖实现、但没打标记"的 checker。
> 这类漏标记**不会立刻暴露**（表现为 `.cj.d` 下多出诊断），所以需要一个专门的用例：
> 用一份"应无诊断"的 `.cj.d` 全量 fixture 跑一遍全部 checker。

**APILevel / SysCap 检查**：**保留且无需任何豁免**。实测
`CfirApiLevelRefHigherChecker` 注册在 `CommonExpressionCheckers.qualifiedAccessCheckers`（`CommonExpressionCheckers.kt:125, 141`），
其数据源 `CfirDeclarationAvailabilityProvider.findAnnotations`（`:88-91`）与 `ownApiLevelInfo`（`:104-119`）
**直接读 `declaration.annotations` 的实时值**，因此 sidecar 合并（4.6.5）一旦写回即自动生效。

##### 为什么这是框架正确而非等价方案

| 判据 | 逐 checker early-return（v2 路线） | 框架级标记（v3） |
|---|---|---|
| 新增一个 checker | 作者**必须知道** `.cj.d` 的存在，否则可能误报；且豁免逻辑散落各处 | 只需在"依赖实现的检查"上标一次；**默认就是正确的** |
| 豁免点数量 | 12 | **1**（生成器模板内） |
| 事后审计 | 靠搜索 `compileCjd` 字面量，容易漏 | 靠搜索 `requiresImplementation`，一眼看全哪些 checker 依赖实现 |
| 与官方对应 | 12 处各自对齐官方豁免点，映射关系隐含 | 标记语义直接就是"官方的实现完备性检查" |

> **验收**：A 表 11 项每项都要有一个断言"`.cj.d` 输入下该 checker 不产出诊断"的用例，
> 且必须配一条"同一文本在 `.cj` 输入下该 checker 产出诊断"的反向用例（6.1 双向验证原则）。

##### P3 实施记录（2026-09-17，已落地并验证）

源收集两级互斥（4.4.1–4.4.3）与 `requiresImplementation` 标记体系（4.4.5）均已实现；测试全绿。
与设计稿的偏差与实证结论：

1. **`isFromDeclarationFile` 的回指路径修正**。设计稿写的是
   `(context.containingFileSymbol?.sourceFile as? CjSourceKindCarrier)?.sourceKind`，
   但 `CjPsiSourceFile`（`CfirFileSymbol.sourceFile` 的实际运行时类型）实现的是 `CjSourceFile`
   并**覆写** `sourceKind`（委托给 PSI 侧的 `CjSourceKindCarrier`），它本身**不是** `CjSourceKindCarrier`
   ⇒ `as?` 恒失败、过滤器对 PSI 路径永不生效（`.cj.d` 诊断全部漏放，首轮实测即暴露）。
   正解是直接读 `context.containingFileSymbol?.sourceFile?.sourceKind` ——
   `CjSourceFile.sourceKind` 本身就是统一入口（自证种类优先、文件名回退），单一真源不变。
2. **`CfirMemberBodyDeclarationChecker` 的属性分支需要显式豁免（A-4 更正四的补充）**。
   更正四的"自动沉默"论证只覆盖**函数**分支（护栏 1 ⇒ `isAbstract == false` ⇒ return）；
   **属性**在 `.cj.d` 中按 R4 仍带 abstract，`class C { prop p: Int64 }`（owner 非 abstract class）
   仍会命中 `invalidAbstract` ⇒ 实测 `.cj.d` 报 `MISSING_FUNC_BODY`，与官方
   `DeclAttributeChecker.cpp:284-289` 的 `opts.compileCjd` 整体提前返回相悖。
   处置：在该 checker 内对**属性分支**加 `isFromDeclarationFile(context)` 守卫（官方语义对齐）；
   **函数分支保持不豁免**——护栏 1 失效时它仍会立刻报 `MISSING_FUNC_BODY`，回归探测器的角色不变。
3. **分析测试 fixture 需要补齐标准 session 组件 + 两段式管线**。
   `AbstractCfirAnalysisTestCase.createTestSession` 原本缺
   `CfirLanguageSettingsComponent` / `CfirExceptionHandler` / `CfirExtensionService` /
   `CfirDefaultImportsProviderHolder`（入口 `registerCommonComponents` 的等价物），逐个按需补齐；
   checker 诊断只在 `runCheckers`（`analyse.kt` 的 resolve → check 两段式）阶段收集，
   测试不能只 `resolveToPhase` 后读 reporter。
4. **`CjPsiSourceFile.getContentsAsStream` 对无 VirtualFile 的 PSI 回退到文件文本**。
   `CfirLiteralNumericOverflowChecker` / `CfirConstEvalArithmeticChecker` 会按偏移读源文本；
   `psiFileFactory.createFileFromText` 产出的 PSI 没有 `virtualFile`，原实现直接 NPE
   （被 CLI 包装器吞成 "Sourceless CfirFile"，需在测试内注册原样重抛的 `CfirExceptionHandler` 才能定位）。
   生产路径（有 VirtualFile）行为不变。

**标记清单自检**：`grep "override val requiresImplementation: Boolean get() = true" cfir/checkers/src`
恰为 **11** 处，与 A 表一一对应（`CfirConstVariableInitializerChecker`、`CfirFileStaticGlobalInitializationChecker`、
`CfirConstructorInitializationChecker`、`CfirClassLikeInitializationChecker`、`CfirFunctionInitializationChecker`、
`CfirNotImplementedOverrideChecker`、`CfirCommonPackageMainChecker`、`CfirPropertySemanticsChecker`、
`CfirPropertyAccessorDeclarationChecker`、`CfirOpenMemberChecker`、`CfirFinalizerDeclarationChecker`）；
`CfirMemberBodyDeclarationChecker` 不打标记（保持回归探测器角色，见第 2 条）。

**测试**：
- `cfir/analysis-tests/.../CfirDeclarationModeCheckersTest.kt`：13/13 通过 ——
  A 表 11 项逐项双向（`.cj` 产出指定诊断 + 同文本 `.cj.d` 零诊断）+ `MISSING_FUNC_BODY` 回归探测 +
  未实现接口成员用例；`CfirCommonPackageMainChecker` 的触发走"测试内置位 `isCommon`"
  （`common` 修饰符不经源码语法产生，对齐 cjo 反序列化路径）。
- `compiler/frontend/.../CjSourceCollectionTest.kt`：+2 用例（`compileCjd = false/true` 互斥互斥对），
  4/4 通过。
- 回归：`DeclarationModeRawCfirStatusTest`（P2 护栏）、`DeclarationFileParsingTest`（P1）、
  `:common:test`、`:cfir:analysis-tests:test` 全量（除 2 个**预存**的注解参数 testdata 失败外全绿，
  与本阶段无关，见 7.3 OP 备注）。

---

### 4.5 `cfir/raw-cfir`：declaration CFIR

#### 4.5.1 现状可直接复用

| 能力 | 现状 |
|---|---|
| `CfirFunction.body` 可空 | 已支持（`CfirFunction.kt:40`） |
| PSI 侧无体 → `body = null` | 已支持（`PsiRawCfirBuilder.kt:3555-3562`） |
| LightTree 侧无体 → `body = null` | 已支持（`LightTreeRawCfirDeclarationBuilder.kt:3082`） |
| `.cjo` 反序列化 `body = null` | 已支持（`CfirDeclDeserializer.kt:1246`） |
| `CfirProperty` 的 `getter`/`setter` 可空 | 已支持（`CfirProperty.kt:38-40`） |

⇒ **本仓不需要为"无 body"新增任何 CFIR 节点或字段。**

#### 4.5.2 需要加固的护栏（核心工作）

**护栏 1 —— 隐式 abstract 推断（v2：必须只作用于函数分支）**

文件与精确结构（实测）：

| 路径 | 函数 | 结构 |
|---|---|---|
| PSI | `PsiRawCfirBuilder.kt:3887-3899` `isImplicitAbstractClassLikeMember` | **单个 `when`，两条分支**：`is CjNamedFunction` / `is CjProperty` |
| LightTree | `LightTreeRawCfirDeclarationBuilder.kt:3287-3293` `isImplicitAbstractClassLikeFunction`<br>`LightTreeRawCfirDeclarationBuilder.kt:3300-3308` `isImplicitAbstractClassLikeProperty` | **两个独立函数**（LightTree 侧已天然分裂） |

```kotlin
// PSI 现状（PsiRawCfirBuilder.kt:3894-3898）
return when (declaration) {
    is CjNamedFunction -> !declaration.hasBody()
    is CjProperty -> declaration.body == null && declaration.getter == null && declaration.setter == null
    else -> false
}

// 改为 —— 护栏【只加在函数分支】
return when (declaration) {
    is CjNamedFunction -> !isDeclarationFile && !declaration.hasBody()   // ← 唯一改动点
    is CjProperty -> declaration.body == null && declaration.getter == null && declaration.setter == null
    //   ↑ 属性分支【保持不动】：R4 要求 .cj.d 中无 {} 的类成员属性仍然获得 abstract
    else -> false
}
```

LightTree 侧同理：**只给 `isImplicitAbstractClassLikeFunction`（`:3287`）加 `&& !isDeclarationFile`，
`isImplicitAbstractClassLikeProperty`（`:3300`）保持不动。**

> **这是 v2 的关键澄清**：v1 只说了"给 `isImplicitAbstractClassLikeMember` 加护栏"，
> 而该函数内部同时覆盖函数与属性。若实现者按 v1 字面在**函数入口**加护栏，会把属性分支一起挡掉，
> **直接违反 R4**（`.cj.d` 中 `class C { prop p: Int64 }` 将不再被标 `abstract`）。
> ⇒ 护栏的语义是「**只抑制函数的隐式 abstract，不抑制属性的**」，与 4.2.6 的 P6/P4 不对称**同源**。

`isDeclarationFile` 的取值路径：
- **PSI 路径**：builder 构造 / `buildCfirFile(file: CjFile)` 时记录 `file.sourceKind`，在 `isImplicitAbstractClassLikeMember` 内读取。
- **LightTree 路径**：`LightTree2Cfir.buildCfirFileWithSurfaces(lightTree, sourceFile, linesMapping)` 已接收 `CjSourceFile`，直接读 `sourceFile.sourceKind`（4.1.2 的默认实现会正确处理）。

**护栏 2 —— interface 默认成员判定：不是"加护栏"，而是"改派生"（v3 定稿更正）**

> ⚠️ **v2 / v3 初稿在此的结论是「两条路径判定结果本来就一致 ⇒ 加护栏只是噪声 ⇒ 确认不改」。该结论是错的。**
> 它的推理比较的是「本仓解析 `.cj` vs 本仓解析 `.cj.d`」，**而不是「本仓 vs 官方」**，
> 因此整段漏掉了 1.3.5 的 `DEFAULT`。在本文档自设的"精确复刻官方语义"目标下，初稿结论不成立。
> 本条已从"护栏"改写为"改派生"。

现状三个判定（**全部从 `hasBody()` 派生**）：

| 路径 | 函数 | 现状谓词 |
|---|---|---|
| PSI | `isDefaultInterfaceMember`（`PsiRawCfirBuilder.kt:3902-3911`） | `is CjNamedFunction -> !foreign && hasBody()` |
| LightTree | `isDefaultInterfaceFunction`（`:3245-3249`） | `isInInterfaceMemberContext() && !isForeign && !modifiers.isAbstract && hasSyntaxBody(node)` |
| LightTree | `isDefaultInterfaceAccessor`（`:3270-3273`） | `isInInterfaceMemberContext() && !modifiers.isAbstract && hasSyntaxBody(node)` |

官方规则（1.3.5）是 `scopeKind == INTERFACE_BODY && !FOREIGN && !ABSTRACT` ——
**`DEFAULT` 是 `ABSTRACT` 的补集**；而本仓把它写成了 `hasBody()` 的补集。
在 `.cj` 下两者等价，在 `.cj.d` 下**结论相反**（见 1.3.5 的四格对照表）。

⇒ **本次不改"加不加护栏"，而是改谓词的来源**：

```kotlin
// 唯一真源：有效 abstract = 显式修饰符 ∨ 隐式推断
val isAbstractEffective = hasModifier(CjTokens.ABSTRACT_KEYWORD) || isImplicitAbstract(...)

isAbstract = isAbstractEffective
isDefault  = isInInterfaceMemberContext() && !isForeign && !isAbstractEffective
```

收益有三层：

1. **与官方同构**：`DEFAULT := !ABSTRACT`，一个定义、一处读取；
2. **`.cj.d` 自动正确**：护栏 1 抑制隐式 abstract 之后，`DEFAULT` 自动置位，**无需任何 `.cj.d` 专属分支**；
3. **消除 J2 反面信号**：同一对属性位（`ABSTRACT` / `DEFAULT`）**只有一份推导**，结构上不可能再静默分叉。

**保留不动**：`isDefaultInterfaceAccessor`（`:3270-3273`）的 accessor 级判定仍用 `hasSyntaxBody`
—— 理由见 1.3.5 末尾的"残留差异"，它与 `.cj.d` 无关。

**验收（三条必须成对提交）**：

| # | 输入 | 断言 |
|---|---|---|
| 1 | `.cj.d`：`interface I { func f(): Int64 }` | CFIR 中 `DEFAULT` 为**真**、`ABSTRACT` 为**假** |
| 2 | `.cj`：同一文本 | `ABSTRACT` 为**真**、`DEFAULT` 为**假**（方向相反的守卫） |
| 3 | `.cj.d`：`interface I { prop p: Int64 }` | `ABSTRACT` 为**真**、`DEFAULT` 为**假**（防止把属性也一起放开） |

> 第 3 条尤其重要：它是"`DEFAULT` 由 `isAbstractEffective` 派生"这一实现的**结构守卫** ——
> 若有人把护栏 1 误加到属性分支（见上），第 3 条会立刻失败。

**护栏 3 —— `BodyBuildingMode` 不得被复用**

`BodyBuildingMode.LAZY_BODIES` 语义是"延迟构建 body"（PSI 侧产出 `CfirLazyBlock` 占位，LightTree 侧 `body = null`）。它**不是**声明模式：

- `LAZY_BODIES` 下 LightTree 的 `body = null` 与声明模式的 `body = null` **恰好同形**，但前者是"待恢复"，后者是"语义上不存在"，下游（如 `CfirTypeVariablesAfterPCLATransformer.kt:97` 对 `CfirLazyBlock` 的跳过）行为不同。
- 生产入口 `analyse.kt:57` / `:78` 不传该参数，当前 `LAZY_BODIES` 只被测试使用。
- ⚠️ LightTree 侧已有防御注释（`LightTreeRawCfirDeclarationBuilder.kt:3284-3285`："LightTree 路径必须基于源码语法判断，不能受 lazy body 构建模式影响"），
  且 `hasSyntaxBody`（`:3323-3324`）只读 LightTree 结构、不读已构造的 CFIR body。**这条既有防御与声明模式护栏正交，不要顺带改它。**

⇒ **明确禁止**：不得用 `bodyBuildingMode = LAZY_BODIES` 来实现声明模式。声明模式必须走独立的 `isDeclarationFile` 通路。

**护栏 4 —— 主构造参数合成的访问器**

PSI 侧 `PsiRawCfirBuilder.kt:1056-1076`（主构造参数合成 accessor，`body = null`）、LightTree 侧 `:681, :717`。
这些是**语言规定的隐式合成**，与声明模式无关，**不改**。

##### P2 实施记录（2026-09-16，已落地并验证）

护栏 1 / 护栏 2 已实现，测试全绿。与设计稿的差异与实证结论：

**实现落点**（两条路径分别独立实现，共享同一事实源 `CjSourceKind`）：

| 路径 | 载体 | 改动 |
|---|---|---|
| PSI | `PsiRawCfirBuilder` | `isImplicitAbstractClassLikeMember` 的 `CjNamedFunction` 分支加 `!declaration.sourceKind.isDeclaration`；`isDefaultInterfaceMember` 改为**取反共享**隐式 abstract 结论（不再独立判 `hasBody`）。文件种类经**新增的文件级扩展属性** `CjDeclaration.sourceKind`（`containingFile as? CjFile`）回指，**不需要构造参数** —— PSI 天然持有文件回指 |
| LightTree | `LightTreeRawCfirDeclarationBuilder` | 新增构造属性 `sourceKind`（默认 `SOURCE` ⇒ 既有构造点零改动）；`isImplicitAbstractClassLikeFunction` 加 `!sourceKind.isDeclaration`；`isDefaultInterfaceFunction/Property` 改为接收 `isImplicitAbstract` 参数（同源传入）；调用点只算一次 |
| 接线 | `LightTree2Cfir.buildCfirFileWithSurfaces` | 构造 builder 时传 `sourceFile.sourceKind` —— 与解析侧（P1 已接）同一事实源，两条路径不产生第二个真源 |

**与设计稿的两处偏差（均为实测修正）**：

1. **`isDefaultInterfaceProperty` 的"有效 abstract"判定扩展到了属性**。设计稿只写了函数侧（`SetDefaultFunc`）；实现时确认官方 `PROP_DECL` 分支同样是"见 `ABSTRACT` 即 return"（`Parser.cpp:371-390`），因此属性的 `DEFAULT := interface && !abstract && !implicitAbstract`，与函数侧同构。这同时消除了 PSI / LightTree 在"接口属性带 `{}` 但 accessor 无体"上的既有分歧（旧 PSI 判 `hasBody()`，旧 LightTree 判 accessor body，两者可不同）。
2. **accessor 级别的 `DEFAULT` 保持独立判定**（`hasSyntaxBody` / `hasBody()`），不继承属性整体的结论 —— accessor 的 default 只依赖它自己是否有体。

**测试**：`cfir/raw-cfir/light-tree2cfir/test/.../DeclarationModeRawCfirStatusTest.kt`，PSI / LightTree **双路径 × 5 组断言**（无体函数 `.cj.d` 不标 abstract；同文本 `.cj` 标 abstract；无体属性 `.cj.d` **仍**标 abstract——R4 互斥断言；interface 无体函数 `.cj.d` 获得 `DEFAULT`——R15；同文本 `.cj` 是 abstract 而非 default）。10/10 通过。

**测试期间发现并登记的一个既有事实**：接口在 CFIR 中是独立的 `CfirInterface`（`CfirClassLikeDeclaration` 的子类），**不是 `CfirClass`** —— 按 `filterIsInstance<CfirClass>()` 找接口成员会得到空集。今后写 class-like 相关断言一律用 `CfirClassLikeDeclaration`。

**回归基线**（全量 light-tree2cfir + psi2cfir）：除上述新用例外，既有失败 4 类**全部预存**，与本改动无关（已逐一归因）：
① `testInterfaceDeclaration` 的 `abstract?` —— **对照实验证明**：临时还原谓词到旧行为后该测试仍失败（golden `ade0381e1` 落后于 HEAD 谓词语义）；
② `testForWithPatternGuard` —— WIP 的 `patternGuard` 渲染领先于 golden；
③ `testPackageAndImport` —— WIP 的 import 渲染出现 `std.std.collection` 包名重复；
④ 全部 `testAllFilesPresentIn*` —— WIP 新增的 `featuresDirective.cj`（untracked）未进 coverage matrix。

#### 4.5.3 需要接收 declaration mode 的入口清单

| 入口 | 文件 | 需要的改动 |
|---|---|---|
| `PsiRawCfirBuilder` | `psi2cfir` | 构造函数新增 `isDeclarationFile: Boolean = false`（或从 `session` 的当前文件推断） |
| `LightTreeRawCfirDeclarationBuilder` | `light-tree2cfir` | 构造已接收 `source: CharSequence`；新增 `sourceKind: CjSourceKind`（或从 `LightTree2Cfir` 下传） |
| `LightTree2Cfir` | `light-tree2cfir` | `buildCfirFileWithSurfaces` 从 `sourceFile.sourceKind` 取值并下传 |
| `CfirSession.buildPreMacroRawCfirFromCjFiles` | `cfir/entrypoint/src/.../pipeline/analyse.kt:55` | 无需新增参数：`CjFile.isDeclarationFile` 已在 PSI 文件上 |
| `CfirSession.buildPreMacroRawCfirViaLightTree` | `cfir/entrypoint/src/.../pipeline/analyse.kt:73` | 无需新增参数：`CjSourceFile.sourceKind` 已可读 |

> 设计取舍：**优先从已有的文件对象上读取种类，而不是层层透传布尔参数**。这样不会破坏既有的公共入口签名（`buildPreMacroRawCfirFromCjFiles` 等已被测试 fixture 依赖）。

#### 4.5.4 `.cj.d` 中的 body 相关 pass 行为

| 阶段 | `.cj.d` 行为 |
|---|---|
| `BODY_RESOLVE` | 无 body 的声明不进入 body resolve（本来就是可选路径） |
| body 相关的检查器的"函数体内部表达式检查" | 无 body 即无表达式，自然不触发 |
| CHIR / codegen | 不启动（4.4.4） |

⇒ 无需额外护栏，**前提是 4.5.2 的护栏 1 已生效**（否则隐式 abstract 会改变 `STATUS` 阶段的属性，进而影响 body resolve 的可达性判断）。

---

### 4.6 `cfir/cfir-serialization`：`.cjo` + `.cj.d` 合并

这是**最容易遗漏、也最关键**的部分（R8-R14）。

#### 4.6.1 整体结构

```text
                    .cjo 路径
                       │
        ┌──────────────▼──────────────────────┐
        │ CjdSidecarLocator                   │  4.6.2
        │  path.cjo → path.cj.d（同目录同名） │
        └──────────────┬──────────────────────┘
                       │ 存在且可读？
        ┌──────────────▼──────────────────────┐
        │ CjdSidecarParser                    │  4.6.3
        │  解析 .cj.d → 签名索引              │
        │  （不跑 Sema、不做宏展开）          │
        └──────────────┬──────────────────────┘
                       │
        ┌──────────────▼──────────────────────┐
        │ CfirDeclDeserializer.convertDecl    │  4.6.4
        │  反序列化 .cjo 声明                 │
        │  → 用 DeclarationMatchKey 匹配      │
        │  → 追加注解                          │
        │  → publishAnnotationInfo()          │
        └──────────────┬──────────────────────┘
                       │
        ┌──────────────▼──────────────────────┐
        │ CfirDeclarationAvailabilityProvider │  消费（不改）
        │ CfirApiLevelRefHigherChecker        │
        └─────────────────────────────────────┘
```

#### 4.6.2 路径推导

文件：`cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjd/CjdSidecarLocator.kt`（新增）

```kotlin
object CjdSidecarLocator {
    /**
     * 推导 `.cjo` 对应的 `.cj.d` 路径。
     *
     * 对齐官方 `ImportManager::SaveDepPkgCjdPath`：同目录、同名、只替换后缀。
     * 官方用 `rfind(".cjo")` 截断，本实现用 `removeSuffix` —— 两者对
     * "路径中含有 .cjo 片段" 的输入行为不同（见下方说明），本实现更保守。
     */
    fun deriveCjdPath(cjoPath: Path): Path {
        val fileName = cjoPath.fileName.toString()
        require(fileName.endsWith(CjoConstants.FILE_EXTENSION)) { "not a cjo path: $cjoPath" }
        val stem = fileName.substring(0, fileName.length - CjoConstants.FILE_EXTENSION.length)
        return cjoPath.resolveSibling(stem + CjSourceKind.DECLARATION_SUFFIX)
    }

    /** 找到可读的 sidecar；找不到返回 null（对齐 R9：静默跳过）。 */
    fun findReadable(cjoPath: Path): Path? =
        deriveCjdPath(cjoPath).takeIf { it.isRegularFile && it.isReadable }
}
```

> **DIFF-4**：官方 `cjoPath.substr(0, cjoPath.rfind(".cjo"))` 对 `/a.cjo.dir/x.cjo` 这类**目录名含 `.cjo`** 的路径会截断到错误位置。本实现只作用于文件名，语义更保守且更正确。需在测试中覆盖含 `.cjo` 片段的目录名。

#### 4.6.3 `.cj.d` 的解析与签名索引

文件：`cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjd/CjdSidecarIndex.kt`（新增）

**设计要点**：

1. **不跑 Sema，不做宏展开** —— 对齐 R10。
   > 与官方的差异：官方在 sidecar 路径上**会做宏展开**（`CompileStrategy.cpp:321-323`）。本仓的宏展开是 CFIR 层的重型操作（`MacroConstructionService.expand`），在 `.cjo` 反序列化路径上引入宏展开会把"库加载"变成"编译"，代价与风险都不成比例。
   > **DIFF-5**：本仓 sidecar 路径**不做宏展开**。影响范围：`.cj.d` 中由宏生成的声明将无法参与匹配。
   > **缓解**：sidecar 只用于搬运注解，而 `@APILevel` / `@SysCap` / `@Hide` 这类注解写在**宏调用之外**的显式声明上，宏生成声明的情形在实践中罕见。此差异必须在文档和评审中显式记录，并作为**已知限制**加入测试基线。
   > **后续可选**：若确有需要，可在 P4 之后追加一个"宏展开后的 `.cj.d` 重解析"增量（复用 `MacroConstructionService`）。
   >
   > ⚠️ **C-4 澄清：上面的"缓解"论证目前是**假设**，尚未对任何真实 SDK 样本验证过，因此它是 P4 的准入条件。**
   >
   > 官方在 sidecar 路径上**确实**跑宏展开（`CompileStrategy.cpp:311-323`，全程 `compileCjd = true`），
   > 且该开关被透传到 7 处宏实参重解析 / 重解析点
   > （`MacroEvaluation.cpp:296 / 310 / 365 / 387 / 562 / 834 / 866`、`MacroExpansion.cpp:364`，已逐条核对）。
   > "本仓选择不做"这个决定本身可以接受，但**它的前提（"注解写在宏调用之外"）必须先被数据证实**。
   >
   > **P4 开工前必须完成的一步（半小时量级）**：
   > 取一份真实 SDK 的 `.cj.d`（最好是覆盖面较广的那个），统计三件事：
   >
   > | # | 统计项 | 决定什么 |
   > |---|---|---|
   > | 1 | 含宏调用的**顶层 / 成员声明**占比 | 若接近 0，`DIFF-5` 风险可忽略 |
   > | 2 | 带 `@APILevel` / `@SysCap` / `@Hide` 的声明中，注解是否落在**宏调用之外** | 直接验证"缓解"论证成立与否 |
   > | 3 | 宏**生成**的声明中是否出现上述注解 | 若有，`DIFF-5` 直接导致注解丢失，必须改方案 |
   >
   > **准入判定**：第 2 项成立且第 3 项为空 ⇒ `DIFF-5` 可接受，按本设计实施；
   > 否则**先改方案再写代码**（追加"宏展开后重解析"，或在 `.cj.d` 解析中保留宏调用节点不做展开而按语法签名匹配）。
   > 这与"先按假设实施、出问题再说"是两种完全不同的风险姿态 —— 后者会在 P4 中后期才暴露，返工面极大。

2. **只提取注解与匹配键，不构建 CFIR**。构建完整 declaration CFIR 的成本与复杂度都不必要——sidecar 的产出物只是"签名 → 注解列表"。

3. **支持两种输入源（与 4.4 的源收集一致），且两者都必须走声明模式**：
   - 磁盘上的 `.cj.d` → LightTree 轻量解析。
   - 内存中的 `.cj.d`（IDE 未保存的编辑器内容）→ 走 PSI（`CjFile.sourceKind == DECLARATION`，4.2.2，天然已具备）。

   > **v2 修正 —— 这条在 v1/v2 初稿里是"建议优先复用 LightTree"，但它有一个实测出来的阻塞点。**
   >
   > 实测 `CangJieLightParser`（`psi/src/org/cangnova/cangjie/parsing/CangJieLightParser.kt`）：
   > ```kotlin
   > fun parse(
   >     builder: PsiBuilder,
   >     errorListener: LightTreeParsingErrorListener? = null,
   >     languageModuleName: String = "",
   > ): FlyweightCapableTreeStructure<LighterASTNode> =
   >     parseWith(builder, errorListener, languageModuleName) { parseFile() }   // :46
   >
   > private inline fun parseWith(..., parseAction: CangJieParsing.() -> Unit) {
   >     val cjParsing = CangJieParsing.createForTopLevelNonLazy(...)
   >     cjParsing.parseAction()          // :73 —— 直接调 parseFile()，无上下文透传
   >     ...
   > }
   > ```
   >
   > **它没有任何 `ParsingContext` 入口**，而 `parseFile()` 又在函数体内硬编码 `with(ParsingContext.DEFAULT)`
   > ⇒ **LightTree 路径当前拿不到声明模式**。
   >
   > 后果不是"少抑制几个诊断"这么轻：`CangJieLightParser` 自带 `LightTreeParsingErrorListener`
   > 与 `reportErrors`（`:91-109`，扫描 `TokenType.ERROR_ELEMENT`）⇒ 非声明模式下解析 `.cj.d` 时，
   > **每个无体成员都会产出 `ERROR_ELEMENT`**，进而使 LightTree 的 `FUNC` / `PROPERTY` 节点结构不完整
   > ⇒ sidecar 提取出的签名键（`DeclarationMatchKey`）也会错 ⇒ 匹配静默失败。
   > 这类故障表现为"注解全部没合并"，但没有任何报错，极难定位。
   >
   > ⇒ **落点（4.2.5 已给出，此处只重述结论）：不新增入口，给既有入口加 `sourceKind` 参数。**
   >
   > ```kotlin
   > // 4.2.5 的改动：两个既有入口各加一个透传参数（默认 SOURCE ⇒ 既有调用点零改动）
   > fun parse(..., sourceKind: CjSourceKind = CjSourceKind.SOURCE)
   > fun parseAnnotationOnly(..., sourceKind: CjSourceKind = CjSourceKind.SOURCE)
   > // parseWith 内部把它交给 createForTopLevelNonLazy(builder, module, sourceKind)
   > ```
   >
   > ⚠️ **一处自我更正**：本段初稿写的是"给 `CangJieLightParser` 新增 `parseDeclaration()`，
   > 与 `parse` / `parseAnnotationOnly` 构成**三入口族**"。该方案已在 4.2.3 / 4.2.5 被否决：
   > ① 声明文件的**文法与 `.cj` 完全相同**，它不需要自己的文法入口，只需要自己的 `sourceKind`；
   > ② 三入口族会把"必须选对入口才正确"的隐含契约固化下来 —— 那正是 v3 要消除的东西；
   > ③ `parseAnnotationOnly` 的真实身份是**宏展开片段的内部重解析入口**（`LightTreeRawCfirDeclarationBuilder.kt:1477`），
   >    与文件种类无关，把它和"文件级入口"并列成族，本身就是分类错误（C-1 同一问题的镜像）。
   > ⇒ **入口仍为 2 个，模式的差异全部由构造属性承载。**

4. **输出**：

```kotlin
/** 一条可匹配的声明签名及其注解。 */
data class CjdDeclarationEntry(
    val key: DeclarationMatchKey,
    val annotations: List<CjdAnnotation>,
    val members: List<CjdDeclarationEntry>,
) {
    /** 函数/构造函数的参数注解，按参数序号索引。 */
    val parameterAnnotations: List<List<CjdAnnotation>> = emptyList()
}

/** 一个 `.cj.d` 文件的索引结果。 */
class CjdSidecarIndex(
    val cjdPath: Path,
    /** 顶层声明，按签名键索引；同键保留全部条目以支持重载诊断。 */
    val topLevel: Map<DeclarationMatchKey, List<CjdDeclarationEntry>>,
)
```

#### 4.6.4 `DeclarationMatchKey` 规格（对齐 1.4.5）

文件：`cfir/cfir-serialization/src/.../cjd/DeclarationMatchKey.kt`（新增）

```kotlin
/**
 * 声明匹配键。
 *
 * 构成严格依照官方 `IsSameDeclByIdentifier` / `IsSameFuncByIdentifier` 的判等维度，
 * 但用确定性哈希替代官方 `unordered_map + find_if` 的不确定顺序。
 */
sealed interface DeclarationMatchKey {
    val identifier: String
    val typeParameterNames: List<String>

    data class Function(
        override val identifier: String,
        override val typeParameterNames: List<String>,
        val parameterNames: List<String>,
        val parameterTypes: List<TypeKey?>,
        /** 泛型约束：[(类型, [上界...])] */
        val constraints: List<Pair<TypeKey, List<TypeKey>>>,
    ) : DeclarationMatchKey

    data class Variable(
        override val identifier: String,
        override val typeParameterNames: List<String>,
        val type: TypeKey?,
    ) : DeclarationMatchKey

    data class TypeDecl(
        override val identifier: String,
        override val typeParameterNames: List<String>,
        val constraints: List<Pair<TypeKey, List<TypeKey>>>,
    ) : DeclarationMatchKey

    data class Extend(
        val extendedType: TypeKey,
        val inheritedTypes: List<TypeKey>,
        override val typeParameterNames: List<String>,
    ) : DeclarationMatchKey {
        override val identifier: String get() = ""   // extend 无标识符
    }

    data class MacroExpand(
        override val identifier: String,
        val fullName: String,
        override val typeParameterNames: List<String> = emptyList(),
    ) : DeclarationMatchKey
}
```

`TypeKey` 覆盖官方 `IsSameType` 支持的形状（1.4.5）：`RefType` / `QualifiedType` / `FuncType` / `TupleType` / `OptionType` / `VArrayType` / `ConstantType` / `PrimitiveType`，以及**未识别形状的 `Opaque`**（官方 `default` 分支返回 true，本仓对应"不可比较即视为相等"）。

**归一化规则**（必须实现）：

| 规则 | 说明 |
|---|---|
| `PRIMARY_CTOR_DECL` → identifier `"init"`，种类 = `Function` | 对齐 `MergeAnnoFromCjd.cpp:363-364` |
| `PRIMARY_CTOR_DECL` 的键与 `.cjo` 侧 `FUNC_DECL "init"` 相等 | 对齐 `MergeAnnoFromCjd.cpp:411-418` |
| `Type/Type` 分支先归一 `Rune`→`UInt8` 名称，随后仍要求 kind 相同，两种类型**不等价** | 对齐 `MergeAnnoFromCjd.cpp:143-154` |
| `VAR_WITH_PATTERN_DECL` → 递归展开 pattern 取底层 `VarDecl` | 对齐 `MergeAnnoFromCjd.cpp:360-361` |
| 类型别名**不展开** | 对齐 `MergeAnnoFromCjd.cpp:169` 的已知限制（D6） |
| 返回类型**不参与键** | 对齐：官方不比返回类型 |
| 修饰符**不参与键** | 对齐：官方不比修饰符 |
| 命名参数标记**不参与键** | 对齐 |

**`DIFF-1` 落地**：泛型约束的 `upperBounds` 比较以 `lgc[i].upperBounds.size()` 为上界（不用外层约束数）。

#### 4.6.5 合并点

文件：`cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirDeclDeserializer.kt`（修改）

现有关键序列（**实测行号 `:131-175`**，v1 写的 `:131-167` 偏短）：

```kotlin
fun deserializeDecl(declIndex: Int): CfirDeclaration {
    context.declCache[declIndex]?.let { return it }                   // :132
    // ... 加锁 + 递归保护 ...
    val result = convertDecl(decl) ?: return null                     // :147
    result.serializedInteropFacts = serializedInteropFacts(decl)      // :152
    result.annotationInfo = serializedAnnotationInfo(decl)            // :153
    // ================= sidecar 合并点：插在此处 =================
    appendSerializedAnnotationMarker(result)                          // :154
    if (result is CfirMemberDeclaration) {                            // :155
        val target = result.annotationTargetFor()                     // :156
        result.annotations.forEach { it.replaceAnnotationTarget(target) }   // :157-159
        if (result.annotations.isNotEmpty() || result.annotationInfo != null) {
            result.publishAnnotationInfo()                            // :164
        }
        result.publishInteropInfo(context.moduleData.session)         // :166
    }
    context.declCache.putIfAbsent(declIndex, result)                  // :168
    context.declCache[declIndex] ?: result                            // :169
}
```

**改动位置（精确到行间）**：在 `:153` 与 `:154` 之间插入：

```kotlin
// 1) 读取 .cjo 侧注解（既有）
result.serializedInteropFacts = serializedInteropFacts(decl)          // :152
result.annotationInfo = serializedAnnotationInfo(decl)                // :153

// 2) sidecar 合并（新增）—— 必须在此行间，理由见下
cjdSidecarIndex?.mergeInto(result)

// 3) 之后全部既有逻辑保持不变
appendSerializedAnnotationMarker(result)                              // :154
if (result is CfirMemberDeclaration) { ... }                          // :155-167
```

> **v2 更正 —— 合并点必须在 `:153` 与 `:154` 之间的真正理由**（v1 的表述不准确）。
>
> v1 说理由是"`publishAnnotationInfo()` 发布注解快照供 provider 读取"。实测：
> `CfirDeclarationAvailabilityProvider.findAnnotations`（`CfirDeclarationAvailabilityProvider.kt:88-91`）与
> `ownApiLevelInfo`（`:104-119`）**直接读 `declaration.annotations` 的实时列表，不读任何快照**。
> 所以「provider 看不到」这个担心的机制是错的（结论恰好仍然正确，但理由必须换）。
>
> 真正约束合并点的是下面三条，**任一条弄错都会产生静默错误**：
>
> | # | 约束 | 位置 | 后果 |
> |---|---|---|---|
> | 1 | `result.annotations.forEach { it.replaceAnnotationTarget(target) }` 必须能遍历到 sidecar 注解 | `:157-159` | 合并晚于它 ⇒ sidecar 注解的 **annotationTarget 未归一化**，注解目标检查（`CfirAnnotationTargetChecker`）会用错目标 |
> | 2 | `publishAnnotationInfo()` 必须基于含 sidecar 的注解集合构建声明侧快照 | `:160-165` | 合并晚于它 ⇒ TestRegistration / Deprecated / Annotation 元数据快照**漏掉 sidecar 注解**，与源码路径分叉 |
> | 3 | `appendSerializedAnnotationMarker`（`:724-762`）内部有**去重判断**：若当前 `annotations` 中已存在 `BuiltInAnnotationKind.ANNOTATION` 的调用则直接返回 | `:726-729` | 合并**早于**它 ⇒ 若 sidecar 恰好自带 `@Annotation`，marker 会被正确跳过；合并晚于它 ⇒ 会**重复追加**一个 marker（注解重复） |
>
> ⇒ 三条约束一致指向同一插入点：**`:153` 之后、`:154` 之前**。这是一个**行间插入**，不接受"插在 `convertDecl` 之后"这类模糊描述。
>
> **补充：合并生效范围已确认覆盖顶层声明**。`:155` 的门禁是 `result is CfirMemberDeclaration`，
> 而实测继承链为：`CfirDeclaration` →（sealed）`CfirMemberDeclaration`（`CfirMemberDeclaration.kt:19`）
> →（sealed）`CfirCallableDeclaration`（`:21`）/ `CfirClassLikeDeclaration`（`:22`）→ 具体声明。
> 顶层函数、类/接口、属性、变量**都是** `CfirMemberDeclaration` 的后代，因此 `:160-165` 的 publish 对它们都会执行。

`mergeInto` 的职责（对齐 R12 / R13）：

```kotlin
fun CjdSidecarIndex.mergeInto(declaration: CfirDeclaration) {
    // R13：只匹配导出声明
    if (!declaration.isExported) return

    val key = DeclarationMatchKey.of(declaration) ?: return
    val entry = topLevel[key]?.firstOrNull() ?: return

    // R12：只搬注解，方向 .cj.d → .cjo
    declaration.replaceAnnotations(declaration.annotations + entry.annotations.toCfirAnnotations())

    // 成员
    declaration.members?.forEach { member ->
        val memberKey = DeclarationMatchKey.of(member) ?: return@forEach
        entry.members.firstOrNull { it.key == memberKey }?.let { memberEntry ->
            member.replaceAnnotations(member.annotations + memberEntry.annotations.toCfirAnnotations())

            // 参数
            if (member is CfirFunction) {
                member.valueParameters.forEachIndexed { index, parameter ->
                    memberEntry.parameterAnnotations.getOrNull(index)?.let { annotations ->
                        parameter.replaceAnnotations(parameter.annotations + annotations.toCfirAnnotations())
                    }
                }
            }
        }
    }
}
```

`CfirDeclaration.replaceAnnotations` 已存在（`cfir/cfir-tree/gen/.../declarations/CfirDeclaration.kt:37`）。

**必须同时处理的两个"另一种形态"**：
- `PrimaryCtor`：在 `.cjo` 侧可能表现为 `CfirPrimaryConstructor`，在 `.cj.d` 侧表现为函数 + 参数注解。匹配键归一化（4.6.4）保证二者可比。
- `CfirProperty`：其注解挂在 `CfirProperty` 本身；`.cj.d` 侧同理。getter/setter 的注解**不合并**（对齐 1.4.5 的"不参与合并的位置"）。

#### 4.6.6 索引的挂载与失效

**挂载点**：`CfirDeserializationContext`（`cfir/cfir-serialization/src/.../deserialize/CfirDeserializationContext.kt:16`）增加一个可空的 `cjdSidecarIndex`，由 `CfirDeserializedSymbolProvider.loadPackageDeserializers` 在加载 `.cjo` 时同步构建。

```kotlin
// cfir/cfir-serialization/src/.../provider/CfirDeserializedSymbolProvider.kt
fun loadPackageDeserializers(packageName: String, cjoPath: Path): PackageDeserializers {
    val context = CfirDeserializationContext(...)
    context.cjdSidecarIndex = CjdSidecarLocator.findReadable(cjoPath)?.let(::buildSidecarIndex)
    ...
}
```

**失效机制**（对齐 D5 的代价）：

| 触发 | 处置 |
|---|---|
| `.cj.d` 文件内容变化 | 使该包的 `PackageDeserializers` 整体失效并重建 |
| `.cjo` 文件变化 | 同上（既有行为） |
| `.cj.d` 被创建/删除 | 同上 |
| 包被从 classpath 移除 | 随既有 provider 清理 |

`AbstractCfirDeserializedSymbolProvider` 已有按包缓存（`cfir/cfir-serialization/src/.../provider/AbstractCfirDeserializedSymbolProvider.kt:172` `getOrCreateDeserializers`），失效入口并入该缓存的管理逻辑。

**清理语义（对齐 R14）**：

官方在 APILevel 检查结束后**全局清除**依赖包上的 `CUSTOM` 注解（`ClearAnnoInfoOfDepPkg`）。本仓不采用全局遍历，理由是：

- 本仓 `.cjo` 声明是**惰性物化**的，全局遍历触达不到未物化的声明；
- 本仓的注解是**追加**在 provider 的缓存对象上，随 provider 缓存一起回收，天然有生命周期边界。

⇒ **等价实现**：sidecar 注解的生命周期 = `PackageDeserializers` 的生命周期。当 provider 缓存被清除（`.cjo`/`.cj.d` 变化、索引重建、会话结束）时，注解随之消失，**依赖包不会被永久污染**。

> **v2 —— 该验证项已完成，结论：本仓不存在跨 provider 复用的 `.cjo` 声明对象，「provider 生命周期即清理边界」成立。**
>
> 实测证据链（四段，逐段可复核）：
>
> | # | 事实 | 位置 |
> |---|---|---|
> | 1 | `declCache` 是 `ConcurrentHashMap<Int, CfirDeclaration>`，**挂在 `CfirDeserializationContext` 实例上** | `CfirDeserializationContext.kt:30` |
> | 2 | `PackageDeserializers` 持有 `private val context: CfirDeserializationContext`，**一包一 context** | `AbstractCfirDeserializedSymbolProvider.kt:393-397` |
> | 3 | `contextCache` 是 `ConcurrentHashMap<String, PackageDeserializers>`，**挂在 provider 实例上** | `AbstractCfirDeserializedSymbolProvider.kt:56` |
> | 4 | `loadPackageDeserializers` **每次调用都新建 context** | `CfirDeserializedSymbolProvider.kt:35-40` |
>
> ⇒ 同一个 `.cjo` 被 N 个 provider 加载时，会得到 N 个**互相独立**的 `CfirDeserializationContext`，
> 各自拥有独立的 `declCache` 与独立的声明对象。**sidecar 注解写回其中一个 provider 的声明实例，不会影响其他 provider。**
>
> ⇒ 因此**不需要** `PackageDeserializers` 销毁时的显式"从声明上摘除注解"逻辑；
> R14（合并注解不得永久污染依赖包）由 provider 缓存的生命周期天然满足。
>
> ⇒ 本条从 P4 的"验收项"降级为**回归用例**：在 6.4 生命周期矩阵中保留"同一 `.cjo` 被两个 provider 加载"用例，
> 断言的是「两个 provider 的声明对象 `!==` 且注解集合互不干扰」，用于**防止将来引入跨 provider 的共享缓存**（例如有人为了省内存把 `declCache` 提升到全局），而不是修复当前缺陷。

#### 4.6.7 `.cjo` 写出的注解（可选，形态 A 相关）

`CjoPackageWriter` 当前**不写注解**（`CjoPackageWriter.kt:136-163` 只写 kind / isTopLevel / fullPkgName / identifier / exportId / mangledName）。

由于本仓 `.cjo` 由外部 `cjc -d` 产出（D9），**本次不改 `CjoPackageWriter`**。若未来需要本仓自产 `.cjo`：

- FlatBuffers schema 已支持：`ModuleFormat.fbs:283` 的 `Decl.annotations:[Anno]`；
- 反序列化侧已支持：`deserializeAnnotations`；
- 只需在 `CjoPackageDeclaration.write` 补 `Decl.addAnnotations`。

---

### 4.7 `cfir/checkers`：检查器行为

**结论：`.cj.d` 支持完成后，APILevel / SysCap checker 不做任何改动。**

理由：`CfirApiLevelRefHigherChecker` 的唯一数据源是 `context.session.declarationAvailabilityProvider`（`CfirApiLevelRefHigherChecker.kt:87-129`），后者从 `declaration.annotations` + `annotationClassId` 读取（`CfirDeclarationAvailabilityProvider.kt:85-129`）。sidecar 注解一旦合并到声明上（4.6.5），检查器自动受益。

需要**新增**的只是 4.4.5 的声明模式豁免（实现性检查的裁剪/early-return）。

---

### 4.8 `analysis` 与 `lsp`

#### 4.8.1 `analysis/decompiled`（`.cjo` → stub / light declaration）

`.cj.d` 的 sidecar 注解需要在两条 `.cjo → 声明` 路径上保持一致（2.1 表中已指出本仓有两条并行路径）：

| 路径 | 模块 | 是否需要改 |
|---|---|---|
| `.cjo → CFIR 库声明` | `cfir/cfir-serialization` | **改**（4.6.5） |
| `.cjo → stub / light declaration → PSI` | `analysis/decompiled` | **改**（见下） |

IDE 侧的注解展示（文档、inspection、light declaration 渲染）走第二条路径，因此需要在 `.cjo` 的 stub/light declaration 构建阶段也注入 sidecar 注解：

- 入口：`analysis/decompiled/decompiler-to-stubs/src/.../CjoDeclarationLoader.kt`、`CjoFileStubBuilder.kt`；
- `CallableCjoStubBuilder` 已有 `hasBody = compiledCallableHasBody(declaration.status)`（`:184, :217`）的既有概念，可作为"注解读取"的平行扩展点；
- 复用同一个 `CjdSidecarLocator` + `DeclarationMatchKey`，避免两套匹配实现（原则 5）。

> **实施建议（v2：依赖可行性已验证，不再是"建议"）**：把 sidecar 索引与匹配谓词放在
> **`cfir/cfir-serialization`**（已有 `.cjo` 读写基础设施），由 `analysis/decompiled` 依赖引用，而不是复制实现。
>
> 实测确认 `analysis/decompiled/decompiler-to-stubs/build.gradle.kts` **已经**包含
> `implementation(project(":cfir:cfir-serialization"))`（另有 `:cfir:cfir-tree`、`:cfir:cfir-common`、`:psi`、`:common`、`:flatbuffers-gen`）
> ⇒ **依赖已存在，无需新增任何模块依赖**，`CjdSidecarLocator` / `CjdSidecarIndex` / `DeclarationMatchKey`
> 可直接被 `analysis/decompiled` 复用。这一条消除了 4.8.1 的唯一不确定性。

> ⚠️ **C-3 澄清：两条路径的"注解一致性"必须显式定义，否则会静默分叉。**
>
> 上面说"复用同一个 `CjdSidecarLocator` + `DeclarationMatchKey`"——这只保证了**匹配规则一致**，
> **并不保证结果一致**。因为 D5 选择了"把注解写回 `declaration.annotations`"（而不是读取侧 union），
> 其直接后果是：**同一个 `.cjo` 被物化几次，注解就被追加几次**。
>
> 因此必须明确两条路径的关系，二选一并写进测试：
>
> | 情形 | 后果 | 需要的保障 |
> |---|---|---|
> | 两条路径产出**同一个**声明对象 | 注解只应写入一次，重复合并会**叠加** | 4.6.6 的"provider 内 `declCache` 独立"证据链需**扩展到覆盖第二条路径** |
> | 两条路径产出**两份**对象 | 各自写自己的副本，互不影响 | 补一条断言：**两份对象的注解集合逐项相等**，防止两条路径分叉 |
>
> **准入条件（P4 开工前必须先答）**：`CjoFileStubBuilder` / `CjoDeclarationLoader` 最终是否复用
> `cfir-serialization` 的反序列化缓存？
> **若答案是"两份对象"**，则 4.6.5 的合并点必须**幂等化**（同一 `DeclarationMatchKey` 重复合并不叠加），
> 并且这一条要成为合并点实现的显式约束（现在是三条约束，应升级为四条）。
> **若答案是"同一个对象"**，需要把 4.6.6 的证据链补上第二条路径的覆盖 ——
> 因为"跨 provider 不共享"证明不了"跨路径不共享"。

#### 4.8.2 `analysis/light-declarations`

`CaLightDeclarationModels.kt:40` 明确"注解列表按需恢复，避免在 light declaration 建立阶段触发完整符号解析"。sidecar 注解属于"按需恢复"的注解，接入时需保证不破坏该惰性特性：**sidecar 索引在 light declaration 构建时不加载，只在注解被请求时加载**。

#### 4.8.3 `lsp`

- `.cj.d` 作为**文档**：走 PSI 路径，高亮/诊断/补全由既有能力提供（4.2）。
- `.cj.d` **不进入编译单元**，除非 `configuration.compileCjd == true`。
- 配置注入：LSP 入口需显式设置 `configuration.compileCjd`（4.3.3），并按其决定源收集模式（4.4）。

---

### 4.9 `intellij-ide`

#### 4.9.1 文件类型注册

文件：`intellij-ide/modules/ide/base/src/main/resources/META-INF/cangjie-filetypes.xml`（修改）

```xml
<extensions defaultExtensionNs="com.intellij">
    <fileType name="CangJie"
              language="CangJie"
              implementationClass="org.cangnova.cangjie.lang.CangJieFileType"
              extensions="cj"
              fieldName="INSTANCE"/>

    <!-- 新增：`.cj.d` 必须使用 patterns，extensions 按最后一段 '.' 取后缀，无法表达双后缀 -->
    <fileType name="CangJieDeclaration"
              language="CangJie"
              implementationClass="org.cangnova.cangjie.lang.declarations.CangJieDeclarationFileType"
              patterns="*.cj.d"
              fieldName="INSTANCE"/>

    <fileType name="cjo"
              implementationClass="org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType"
              extensions="cjo"
              fieldName="INSTANCE"/>
</extensions>
```

**不变更**：`cangjie-language-shell.xml` 中 `<lang.parserDefinition language="CangJie" .../>`（D1：复用同一 ParserDefinition）。

> **v2 更正 —— 那条"历史旁证"不成立，且 v1 据此提出的动作是多余的。**
>
> v1 称"过期构建产物 `intellij-ide/build/resources/main/META-INF/cangjie-filetypes.xml:49-51` 留有草稿，
> 本次实现应当清掉那段注释"。实测：该文件**位于 `build/`，是构建产物**，其内容与当前源码**已经不一致**：
>
> | 项 | `build/` 产物里的草稿 | 当前源码（`cangjie-filetypes.xml`） |
> |---|---|---|
> | `.cjo` 实现类 | `CangJieBinaryObjectFileType` | `CangJieBuiltInFileType` |
> | `cjd` 注册 | `extensions="cjd"`（**单后缀，错误的表达**） | 不存在 |
> | 声明类型类名 | `CangJieDeclarationsFileType` | 该类名早已不存（现文件 `CangJieDeclarationsFileType.kt` 内的对象是 `CangJieBuiltInFileType`） |
>
> ⇒ 它是**更早版本的陈旧产物**，只能作为"方向曾被考虑过"的极弱旁证，**不能作为设计依据**（尤其不能当作"官方/历史正确做法是 `extensions="cjd"`"的证据 —— 那是错的，
> 见 D1：必须用 `patterns="*.cj.d"`，因为 `foo.cj.d` 的 `VirtualFile.extension` 是 `d`）。
>
> ⇒ **源码 `cangjie-filetypes.xml` 中本来就没有这段注释**（已实测，该文件仅 14 行，只有 `CangJie` 与 `cjo` 两条注册），
> 因此 v1 要求的"清掉那段注释"**没有可执行对象，本条删除**。`build/` 目录是产物，不应作为改动目标。

#### 4.9.2 解析与文件创建：**v3 下 `createFile` 无需改动**（B-4）

文件：`psi/src/org/cangnova/cangjie/parsing/CangJieParserDefinition.kt`（**不改**，实测 `:106-113`）

现状（原文照录，**保持不变**）：

```kotlin
override fun createFile(viewProvider: FileViewProvider): PsiFile {
    return when (viewProvider.fileType) {
        is CangJieFileType -> CjFile(viewProvider, false)
        else -> PsiPlainTextFileImpl(viewProvider)
    }
}
```

> **B-4 更正：v2 / v3 初稿在此给出的"改为"代码属于 v2 世界，在 v3 下既编译不过、也不需要。**
>
> 初稿写的是：
> ```kotlin
> // ❌ 已删除
> is CangJieDeclarationFileType -> CjFile(viewProvider, isCompiled = false, isDeclarationFile = true)
> ```
> 三重问题：
>
> ① **编译不过**：4.2.2 已把 `CjFile.isDeclarationFile` 改为**只读派生属性**
>    （`val isDeclarationFile: Boolean get() = sourceKind.isDeclaration`），不再是构造参数。
> ② **不需要**：v3 的 `sourceKind` 从 `viewProvider.fileType` 推导。`.cj.d` 被 `is CangJieFileType ->` 接住后创建的
>    `CjFile`，读 `viewProvider.fileType` 即得 `DECLARATION`，结果与专设分支**逐位相同**。
> ③ **"声明分支必须前置"这条顺序要求随之消失** —— 它是 v2 布尔构造参数方案的产物（构造参数一旦传错就静默出错）。
>    v3 下**不存在传参**，因此不存在传错的可能。
>
> ⇒ 照初稿改会**新增一个语义冗余的分支**，并把一条已经不成立的前提写进验收清单（7.4-E 已据此删改）。

**v3 定稿：本文件不动。** `.cj.d` 的处理链条本身是自洽的：

```text
cangjie-filetypes.xml 注册 CangJieDeclarationFileType          （4.9.1）
        ↓
viewProvider.fileType == CangJieDeclarationFileType
        ↓
createFile: is CangJieFileType -> 命中（子类型）⇒ CjFile(viewProvider, false)   ← 既有代码，无需改
        ↓
CjFile.sourceKind = DECLARATION                                  （4.2.2，从 viewProvider.fileType 推导）
        ↓
CangJieParser.parse 读 psiFile.sourceKind ⇒ 注入 CangJieParsing 构造属性         （4.2.4）
```

> **唯一必须记住的前提**：`CangJieDeclarationFileType` **必须继承** `CangJieFileType`（4.2.1 的设计），
> 这正是"`createFile` 不用改"的依据。若将来有人把它改成独立的 `FileType`，`.cj.d` 就会 fallthrough 到
> `PsiPlainTextFileImpl` —— v1 描述的那种故障模式才会真正出现。**这是 4.2.1 的一条隐性约束，在此显式登记**：
>
> | FileType | 是否继承 `CangJieFileType` | `createFile` 结果 |
> |---|---|---|
> | `CangJieFileType`（`.cj`） | 自身 | `CjFile` ✓ |
> | `CangJieDeclarationFileType`（`.cj.d`，本次新增） | **是** | 被 `is CangJieFileType ->` 接住 ⇒ `CjFile` ✓ |
> | `CangJieBuiltInFileType`（`.cjo`） | 否（独立 object） | fallthrough ⇒ `PsiPlainTextFileImpl`（既有行为，本次不改） |
>
> ⚠️ **顺带更正一处 v1/v2 的故障描述**：v1 写"若不改 `createFile`，`.cj.d` 会 fallthrough 到 `PsiPlainTextFileImpl`"。
> 在**继承**设计下这句话不成立（`is CangJieFileType` 对子类型为真）。
> 但由于 v3 已经**根本不需要改** `createFile`，该故障描述连同它的"改法"一起作废。
> 需要保留的只有一条**可区分的验收断言**（6.6）：**"`.cj.d` 打开后无 `Function body expected` 波浪线"**
> —— 它能同时排除"静默降级为普通 `CjFile`"与"退化成纯文本"两种故障，而 v1 的"不是纯文本"只能排除后者。

#### 4.9.3 源码与索引收集

文件：`intellij-ide/modules/ide/base/src/main/kotlin/org/cangnova/cangjie/ide/base/analysisApiPlatform/CaIdeScopeCangJieFileCollector.kt`（`:107-111`）

**决策（D7）：`isCangJieScopeCandidate()` 不改。**

```kotlin
private fun VirtualFile.isCangJieScopeCandidate(): Boolean {
    return fileType == CangJieBuiltInFileType ||
        extension.equals("cjo", ignoreCase = true) ||
        isCangJieFileType()          // 保持不含 .cj.d
}
```

理由与后果见 D7。**必须补充**：在函数上添加注释说明这是有意为之，避免后续被人"顺手补上"：

```kotlin
/**
 * 注意：**故意不把 `.cj.d` 列为候选**。
 *
 * `.cj.d` 是 `.cjo` 的 API 元数据 sidecar，不是独立的库声明（官方在
 * `ParseAndMacroExpandCjd` 中只用它搬运注解，不注册为包）。
 * 若把 `.cj.d` 与 `.cjo` 同时作为声明提供者，会产生同包两套声明、重复符号、
 * overload 数量错误与导航目标不稳定。
 *
 * `.cj.d` 自身的 PSI/高亮/导航由 `CangJieDeclarationFileType` 与
 * `CjFile.sourceKind == DECLARATION`（4.2.2）承载，与此处的声明聚合无关。
 */
```

同时需要检查 **`deveco/modules/ide/base/.../CaIdeScopeCangJieFileCollector.kt`** —— 本仓存在同名副本，两处必须保持一致（或提取为共享模块）。

**另一处必须检查**：`psi/src/org/cangnova/cangjie/utils/virtualFileUtil.kt:34-40`

```kotlin
fun VirtualFile.isCangJieFileType(): Boolean {
    val nameSequence = nameSequence
    if (nameSequence.endsWith(CangJieFileType.DOT_DEFAULT_EXTENSION)) return true   // ".cj"
    return FileTypeRegistry.getInstance().isFileOfType(this, CangJieFileType.INSTANCE)
}
```

**结论：行为正确，无需改动。v2 补全判定依据（v1 只给了半条理由）**：

1. 第一条：`foo.cj.d` 的 `nameSequence` 以 `.cj.d` 结尾，**不以 `.cj` 结尾** ⇒ `endsWith(".cj")` 为 false。（`DOT_DEFAULT_EXTENSION = ".cj"`，`CangJieFileType.kt:39`）
2. 第二条：`FileTypeRegistry.isFileOfType(file, type)` 的判定是**"该文件的注册 FileType 是否 == 传入的 type"**，
   **不涉及 Java/Kotlin 的继承关系**。`foo.cj.d` 的注册 FileType 是 `CangJieDeclarationFileType`（由 `patterns="*.cj.d"` 认领），
   `≠ CangJieFileType.INSTANCE` ⇒ false。
   > ⚠️ 这一条必须写清楚，因为很容易被误推成"CangJieDeclarationFileType 继承了 CangJieFileType，所以 isFileOfType 会返回 true"。
   > 那个推断是错的：`FileTypeRegistry` 比较的是**注册身份**，不是类型层级。
   > 另一个反向证据：`.cjo` 的 `CangJieBuiltInFileType` 完全不继承 `CangJieFileType`，而它照样能被 `isFileOfType` 正确识别为"不是 .cj"。
3. ⇒ 净结果：`x.cj.d` 的 `isCangJieFileType()` 为 false，`isCangJieScopeCandidate()`（`CaIdeScopeCangJieFileCollector.kt:107-111`）也为 false ⇒ 与 D7 一致，**无需改动**。
4. 仍然需要补充一个显式的 `isCangJieDeclarationFileType()`（实现同 2，比较 `CangJieDeclarationFileType.INSTANCE`），供需要**正向**区分声明文件的调用方使用。

**v2 新增缺口 —— `.cj.d` 的 Stub 索引边界必须显式处理**

v1 在 DIFF-3 里承认"`.cj.d` 会建 Stub 索引"，但没有给出**边界**。v2 补齐：

- `CjFileElementType` 是 `.cj.d` 复用的文件元素类型（4.2.7），因此**任何被 PSI 加载的 `.cj.d` 都会建 Stub 并被索引**。
- `CjWorkspaceModelSync.kt:560-573` 的 source roots 来自 `cjModule.sourceSets`（**目录粒度**），
  所以"`.cj.d` 不会作为独立 source root"是自动成立的（无需改动）——**但这不等于"不会被索引"**：
  只要 `.cj.d` 物理位于某个已注册的 source root 目录内，它就会被索引。
- ⇒ **本设计的立场**：`.cj.d` 的**规范位置是库目录**（与 `.cjo` 同目录同名，R8），
  **不应放进模块源码目录**。`.cj.d` 属于 SDK / 三方库的 API 元数据，放进源码目录本身就是误用。
- ⇒ **落实为可验证的约束**（而不是靠约定）：
  1. 在 `CjWorkspaceModelSync` 同步 content root 时，对源码根下匹配 `*.cj.d` 的文件加排除
     （`WorkspaceModelCompat` 已有 exclude 能力，见 `WorkspaceModelCompat.kt`）；
  2. 6.6 验收项新增一条：**"源码根下的 `.cj.d` 不出现在项目 Stub 索引的符号中"**。
- ⇒ 这样 DIFF-3 的"评审关注点"（`.cj.d` 与 `.cjo` 产生同名符号）被**从两个方向同时关闭**：
  逻辑方向上不进声明提供者聚合（D7），索引方向上不进源码组。

#### 4.9.4 依赖模型

文件：`intellij-ide/modules/domain/project-model/src/main/kotlin/org/cangnova/cangjie/project/model/CjDependency.kt`（`:305-339`）

按用户原文的建议，**不新增独立依赖类型**，而是在 `Binary` 上加派生属性：

```kotlin
data class Binary(
    override val name: String,
    val cjoPath: java.nio.file.Path,
    val libPath: java.nio.file.Path? = null,
    override val target: String,
    override val scope: CjDependencyScope = CjDependencyScope.COMPILE,
    override val sourceModule: CjDependencyDeclarant,
) : CjDependency() {
    /**
     * 派生的声明文件路径（`.cjo` 的 API 元数据 sidecar）。
     *
     * 有且仅有同目录同名的 `.cj.d` 存在时才非空。
     * `.cj.d` 不作为独立依赖参与链接，也不产生独立的 library root。
     */
    val derivedCjdPath: java.nio.file.Path?
        get() = CjdSidecarLocator.findReadable(cjoPath)

    // ...
}
```

**v2 修正 —— 这里的"内联"不是风格选择，而是依赖边界所迫；但必须用常量而非字面量**。

实测 `intellij-ide/modules/domain/project-model/build.gradle.kts` 的依赖为：

```kotlin
implementation(project(":modules:foundation"))
implementation(project(":modules:domain:toolchain"))
implementation(project(":modules:ide:ux"))
implementation(libs.cangjieCommonForIde)      // ← 主仓 common 的发布工件
implementation(libs.cangjiePsiForIde)         // ← 主仓 psi 的发布工件
```

⇒ 两件事同时成立：
1. `project-model` **不能**引用 `CjdSidecarLocator` —— 它**没有**、也不应该依赖 `cfir-serialization`。
   （`cfir-serialization` 通过 `libs.cangjieCfirForIde` 暴露，而该工件只被 `ide/base` 等模块以 `compileOnly` 引入，见 4.10。）
   ⇒ 因此 `derivedCjdPath` 的路径推导**必须**在 `project-model` 内自行实现，这不是"偷懒"，是唯一可行解。
2. `project-model` **可以**引用主仓 `common` 模块的类 ⇒ **后缀字面量仍然有单一真源**。

⇒ **落点规格**（这样既满足依赖边界，又不违反 3.1 原则 5 的"单一真源"）：

```kotlin
val derivedCjdPath: java.nio.file.Path?
    get() {
        val fileName = cjoPath.fileName?.toString() ?: return null
        if (!fileName.endsWith(CjoConstants.FILE_EXTENSION)) return null
        val stem = fileName.removeSuffix(CjoConstants.FILE_EXTENSION)
        // 后缀常量来自 common，与 CjSourceKind.DECLARATION_SUFFIX 同源；不得写 ".cj.d" 字面量
        return cjoPath.resolveSibling(stem + CjSourceKind.DECLARATION_SUFFIX)
            .takeIf { it.isRegularFile() && it.isReadable() }
    }
```

> **与 3.1 原则 5 的关系（v2 澄清）**：原则 5 的原文是"文件**种类判定**只允许存在一份实现，
> 禁止在各模块重复写 `endsWith(".cj.d")`"。上述实现**没有**重复"种类判定"（它不做 `SOURCE`/`DECLARATION` 的语义判断，
> 只做路径推导），且**复用了 `CjSourceKind.DECLARATION_SUFFIX` 常量** ⇒ 原则 5 未被违反。
> 若实现者图省事写成 `stem + ".cj.d"` 字面量，才构成违反。**这一条要写进 7.4 复核清单。**
>
> 同理，`CjoConstants.FILE_EXTENSION` 存在（`.cjo`，`CjoConstants.kt:14`）——但注意它也在 `cfir-serialization` 中，
> `project-model` 同样引不到 ⇒ `.cjo` 后缀在 `project-model` 内也只能用字面量，**这是既有状态，不是本次引入**。

> **v3 说明：为什么最终选择内联，而不是"下沉 `common` 抽公共类"**。
> 曾评估过更强方案 —— 把纯路径部分下沉到零依赖的 `common`，让两处共用（它更符合"单一真源"直觉）。
> **结论是不做**，理由三条：
> 1. **实际使用点只有 2 个**：`cfir-serialization` 内的 sidecar 定位、`project-model` 的派生属性。
>    而后者因依赖边界**必须**自己实现 ⇒ 抽类能消除的只是 3 行重复。
> 2. **`common` 是全局最底层模块**。为"`.cjo` → `.cj.d` 的库元数据布局"这一知识新增公共类型，
>    会把它塞进所有模块的编译面，而该语义只对两个调用方有意义。
> 3. **"单一真源"的目标已用更小的手段达成**：真正需要唯一的不是"函数"而是**后缀字面量**，
>    复用 `CjSourceKind.DECLARATION_SUFFIX` 即已满足（3.1 原则 5 的原文也只约束"判定实现"与"字面量"）。
>
> ⇒ **判据**：当"抽象"的收益小于"扩大公共 API 表面"的代价时，**内联 + 复用常量**比"抽类"更符合框架正确性。
> 框架正确性要求的是**语义归属正确**，不是"函数必须唯一"——这一点与第 8 章"不建议破坏"的清单同源。

#### 4.9.5 Workspace Model 同步

文件：`intellij-ide/modules/domain/project-model/src/main/kotlin/org/cangnova/cangjie/project/workspace/CjWorkspaceModelSync.kt`

- `:1375-1380` 目前把 `.cjo` 作为 `LibraryRoot(COMPILED)` 加入 `classesRoots`。
- **`.cj.d` 不得加入 `classesRoots`**（D7 / R11）。加入后果：IDE 会把 `.cj.d` 当独立库声明，正是不希望发生的重复符号来源。
- `.cj.d` 通过 `Binary.derivedCjdPath` 传递给分析层，由 4.6 的 sidecar 机制消费。

#### 4.9.6 构建入口

本仓 IDE 的常规构建走 `cjpm`（`CjpmCommandExecutor.kt:48-83`），**不直接调 `cjc`**。

- **`.cj.d` 不应混入普通项目构建流程**。`cjpm build` 不产生 `.cj.d`，也不需要感知它。
- 若需要提供"编译声明文件"动作，则按用户原文，在 toolchain 中**显式**调用：

```text
<SDK>/tools/bin/cjc -d --output <dir> xx.cj.d
```

复用既有的 `ToolchainCommandLine`（`intellij-ide/modules/domain/toolchain/src/main/kotlin/org/cangnova/cangjie/toolchain/command/commandLine.kt:45-254`）构造命令行，参照 `CompilerMacroExpansionProvider.kt:249-296` 直接调编译器的方式。

- **产物回读**：`CjpmBuildOutputParser.collectArtifacts`（`:129-175`）目前把 `.cjo` 映射为 `ArtifactFileType.OBJECT`。若 `.cj.d` 编译产物需要被识别，扩展点在这里。**本次可不做**，列为后续项。

#### 4.9.7 `deveco` 模块

`deveco/modules/ide/base/.../CaIdeScopeCangJieFileCollector.kt`、`deveco/modules/domain/toolchain/.../CangjieSdkLayout.kt`（`:55-68` 的 KDoc 已提到 `.cj.d`，但 `collectCjoFiles()` 只收 `.cjo`，`:535-550`）。

- `.cj.d` 支持落地后，`CangjieSdkLayout` 应新增 `collectCjdFiles()`，并让 SDK 布局暴露 `modules/<target>/**/*.cj.d`。
- **实施顺序**：建议 `intellij-ide` 先落地，`deveco` 跟随（硬约束见 4.9.8）。

#### 4.9.8 与硬约束的关系

根据项目约定，本次改动范围 **只限 `.cj.d` 支持所必需的模块**；`external/cangjie_compiler` 与 `external/cangjie_deveco_plugins` 是**只读镜像**，只作为语义取证来源，不得修改。

---

### 4.10 跨仓边界：主仓发布 → `intellij-ide` 消费（v2 新增，v1 完全遗漏）

**这是本特性最容易在实施到 P5 时"卡住"的地方，必须在计划阶段就纳入。**

#### 4.10.1 事实：`intellij-ide` 不通过 project 依赖引用主仓

`intellij-ide` 是**独立 Gradle 构建**（不在主 `settings.gradle.kts` 内）。它的模块通过 Maven 坐标消费主仓的**发布工件**：

`intellij-ide/gradle/libs.versions.toml`
```toml
cangjie                      = "1.1.1"                                                    # :43
cangjieCommonForIde          = { module = "org.cangnova.cangjie:cangjie-frontend-common-for-ide", version.ref = "cangjie" }   # :91
cangjiePsiForIde             = { module = "org.cangnova.cangjie:cangjie-frontend-psi-for-ide",    version.ref = "cangjie" }   # :92
```

`intellij-ide/modules/ide/base/build.gradle.kts` 以 `compileOnly` 引入 11 个该类工件
（common / psi / cfir / analysis-api / analysis-api-cfir / analysis-api-standalone / code-insight ×5）。

主仓侧的对应发布模块（`settings.gradle.kts`）：
- `:prepare:ide-plugin-dependencies:cangjie-frontend-common-for-ide`（`:35`）
- `:prepare:ide-plugin-dependencies:cangjie-frontend-psi-for-ide`（`:36`）
- 另有 `:prepare:ide-plugin-dependencies-module:*`（`:50-51`）与其余 9 个同族模块

#### 4.10.2 事实：本地仓库优先，但版本号是固定的

`intellij-ide/settings.gradle.kts` 的依赖仓库按优先级排列，并带明确注释：

```kotlin
repositories {
    // 优先解析本地发布产物，支持与主仓库联调：
    // 1. ../build/repo 是 cangjie 根工程默认 publish 落盘的位置
    // 2. mavenLocal() 兼容显式执行 publishToMavenLocal 的场景
    maven { url = uri("../build/repo") }
    mavenLocal()
    mavenCentral()
    ...
}
```

⇒ 联调路径是通的（本地 `build/repo` 优先于远端），**但版本号 `cangjie = "1.1.1"` 不会随源码变化**。

#### 4.10.3 对本特性的影响（三处硬依赖）

| 阶段 | 依赖发布的内容 | 如果不先发布会怎样 |
|---|---|---|
| **P5** | `cangjie-filetypes.xml` 里 `implementationClass="...CangJieDeclarationFileType"` —— 该类定义在 `psi` | 插件启动即 **`ClassNotFoundException`**，整个 `<fileType>` 扩展注册失败 |
| **P5** | `CjFile.getFileType()`（`psi`）/ `CjSourceKind`（`common`） | psi 工件里没有对应成员 ⇒ **编译失败**（`CjFile` 是主仓类，IDE 侧无法"就地补"） |
| **P5** | `CjDependency.Binary.derivedCjdPath` 引用 `CjSourceKind.DECLARATION_SUFFIX`（`common`） | 同上，编译失败 |
| **P6** | `analysis/decompiled` 侧的 sidecar 注入（主仓内，无跨仓问题） | — |

⇒ **P5 的真实前置条件是「P0 + P1 + P4 完成」+「主仓重新发布 `common` / `psi` 工件到 `../build/repo`」**，
而不是 v1/v2 写的"依赖 P1, P4"。

#### 4.10.4 联调流程（写进实施计划）

```bash
# 1) 主仓：发布到 <cangjie>/build/repo（intellij-ide 会优先在这里解析）
gradlew-queue.bat publish                 # 或仅发布所需的两个工件：
gradlew-queue.bat :prepare:ide-plugin-dependencies:cangjie-frontend-common-for-ide:publish \
                  :prepare:ide-plugin-dependencies:cangjie-frontend-psi-for-ide:publish
# 2) intellij-ide：重新解析依赖（版本号未变，必须强制刷新，否则可能命中旧缓存）
cd intellij-ide && ./gradlew --refresh-dependencies :modules:ide:base:compileKotlin
```

> ⚠️ **`--refresh-dependencies` 是本流程的必要步骤，不是可选优化**。
> 由于 `cangjie = "1.1.1"` 是**固定版本号**，而 `../build/repo` 里的同名同版本文件内容被覆盖了，
> Gradle 的依赖缓存**可能**仍然复用旧解析结果（Gradle 对固定版本的模块有缓存，且不校验本地 repo 文件时间戳）。
> 表现为"代码明明改了、发布也成功了，IDE 侧却仍报找不到类"——这是最容易误判为"代码没写对"的假故障。

#### 4.10.5 与 `deveco` 的关系

`deveco/` 同样是独立构建（见 7.3-OP3），因此**它也落在同一条边界之外**：
如果 `deveco` 要消费 `.cj.d` 相关的 `psi` / `common` 类，同样需要先发布。
本特性的既定范围是"`intellij-ide` 先落地、`deveco` 跟随"（4.9.7），故 `deveco` 侧的发布需求不在本次，
但**同一套联调流程对它同样适用**。

---

## 5. 实施计划

按"可独立验证、可独立回滚"切分。每阶段完成后立即提交（项目约定：每个功能立即 commit）。

### 5.1 阶段划分

| 阶段 | 内容 | 依赖 | 验收标准 |
|---|---|---|---|
| **P0** | 公共契约：`CjSourceKind`（3 值）、`CjSourceKindCarrier`、`CjSourceFile.sourceKind`、**`CjFile.sourceKind` + `getFileType()` 归正**（4.2.2）、`CompilerConfiguration.compileCjd`、`compilerArguments` 的 `-d` | — | `CjSourceKind.fromFileName` 边界用例（`.cj.d`、`a.cj.d`、`.cjd`、`a.CJ.D`、`a.cj.d.txt`、`a.cj.macrocall`）；生成产物 `CommonCompilerArguments.kt` **由 6 个属性变为 7 个，且 `git diff` 只多出 `compileCjd`** |
| **P1** | `psi` 解析模式：`CangJieDeclarationFileType`（**P0 已交付**）、**`CangJieParsing.sourceKind` 构造属性**（4.2.3/4.2.5，`ParsingContext` 不加字段）、`CangJieParser.parse` 按 `sourceKind` 穷尽分派（4.2.4/C-1）、**`reportMissingBody` 统一入口**（A 组，6 个调用点 + 1 处早退）、**B 组 5 处诊断文案修正**（4.2.6）、`CangJieLightParser` 透传 | P0 | ✅ **已完成**（实测见 4.2.8）：`DeclarationFileParsingTest` 19/19 + `DeclarationModeReverseParsingTest` 6/6 通过。`.cj.d` 中无体函数 / 构造 / 访问器 / `main` / finalizer 均无 `PsiErrorElement`；残缺输入（`func }` / `init }` / `main }` / `@Foo macro }`）仍报错且报修正后的文案；同一文本在 `.cj` 下必须报错（双向验证）；残缺输入后的成员不被吞掉。**未覆盖**：LightTree 路径的声明模式行为（属 P2 测试落点） |
| **P2** | **✅ 已完成（2026-09-16，实施记录见 4.5.2 末尾）**。护栏 1（仅函数分支）+ 护栏 2（DEFAULT 与 ABSTRACT 同源）；PSI 经文件回指取 `sourceKind`（无需构造参数）、LightTree 加构造属性并由 `LightTree2Cfir` 接线 | P1 | 10/10 通过（`DeclarationModeRawCfirStatusTest`，PSI/LightTree 双路径 × 5 组断言，含 R3/R4 互斥对与 R15）；全量回归失败均为预存问题（已逐项归因，见实施记录） |
| **P3** | 源收集两级互斥（`GroupedCjSources`、`coreEnvironmentUtils`）；**`CfirDeclarationChecker.requiresImplementation` 标记体系 + 生成器模板过滤 + A 表 11 个 checker 打标记**（4.4.5） | P0 | 收集：`compileCjd=false` 只收 `.cj`、`=true` 只收 `.cj.d`，两者都不收对方；checker：A 表 11 项各有"`.cj.d` 下不产出诊断"用例 + 一条"`.cj` 下产出诊断"反向用例；**`.cj.d` 下不出现 `MISSING_FUNC_BODY`**（该断言同时验证护栏 1） |
| **P4** | **sidecar 合并**：`CjdSidecarLocator`、`CjdSidecarIndex`、`DeclarationMatchKey`、`CfirDeclDeserializer` 合并点、provider 失效 | P0 **+ 两个准入条件**（C-3 / C-4，见下） | 见 6.4 sidecar 测试矩阵；跨 provider 对象不共享作为**回归防线**（4.6.6）；**两条路径的注解集合逐项相等**（C-3） |
| **P5** | IDE 侧：`cangjie-filetypes.xml` 注册、~~`CangJieParserDefinition.createFile`~~（**B-4：v3 下无需改动**，见 4.9.2）、源码根 `*.cj.d` 排除出 Stub 索引、收集器注释与 `deveco` 副本对齐、`CjDependency.Binary.derivedCjdPath` | P0, P1, P4 **+ 主仓发布 common/psi 工件**（见 4.10，联调须 `--refresh-dependencies`） | 手工验收见 6.6 |
| **P6** | `analysis/decompiled` 注解注入、`lsp` 接入、`deveco` SDK 布局 | P4, P5 | light declaration / 文档中能看到 `.cj.d` 提供的 `@APILevel` |

**P4 的两个准入条件（必须先答，再写代码）**

P4 是本设计中唯一"结论依赖尚未验证的事实"的阶段（其余阶段的事实都已实测）。因此它不能直接开工：

| # | 准入问题 | 怎么答 | 答案影响 |
|---|---|---|---|
| **C-3** | `analysis/decompiled` 与 `cfir-serialization` 两条 `.cjo → 声明` 路径，是否产出**同一个**声明对象？ | 读 `CjoFileStubBuilder` / `CjoDeclarationLoader`，看它是否复用 `cfir-serialization` 的反序列化缓存 | 同一对象 ⇒ 4.6.6 证据链需扩展覆盖第二条路径，且合并需**幂等**；两份对象 ⇒ 补"注解集合逐项相等"用例 |
| **C-4** | `DIFF-5`（sidecar 不做宏展开）的缓解前提"注解写在宏调用之外"，在真实 SDK 中是否成立？ | 取一份真实 SDK 的 `.cj.d`，统计 4.6.3 C-4 表中的三项 | 成立 ⇒ 按本设计实施；不成立 ⇒ **先改方案**（追加宏展开后重解析），否则会在 P4 中后期返工 |

> 完整论证见 4.8.1（C-3）与 4.6.3（C-4）。两件事都是"半小时量级的核实"，但**决定的是 P4 的方案形状**，不是实现细节。

### P4 实施进度（2026-09-17，部分完成，真实 sidecar 已取得）

- **已实现路径定位**：`CjdSidecarLocator.deriveCjdPath` / `findReadable`，只替换文件名末尾的 `.cjo`，复用 `CjSourceKind.DECLARATION_SUFFIX`。缺失、非普通文件或不可读时返回 null；定位结果不缓存。
- **已验证**：`:cfir:cfir-serialization:test --tests "*CjdSidecarLocatorTest"`，9 tests / 0 failures / 0 errors / 0 skipped。覆盖同目录同名、目录和文件名中额外的 `.cjo` 片段、相对路径、非法后缀、根路径、缺失文件、目录冒充文件、可读文件以及缺失后创建。不可读文件的权限分支尚未单独实测（Windows ACL 环境）。
- **定位器阶段模块回归**：首次因 C 盘满在配置阶段失败；空间恢复后重跑 `:cfir:cfir-serialization:test`，8 suites / 30 tests / 0 failures / 0 errors / 0 skipped。此结果只覆盖定位器阶段，不能代表后续 parser/index 实现已验证。
- **C-3 静态核实完成**：`CjoDeclarationLoader.loadDeclarations` 每次创建独立 `CfirDeserializationContext`；provider 也创建自己的 context。两条路径复用 deserializer 实现，但不共享 `declCache` 或声明对象。后续必须补“两份对象、注解有序语义逐项相等”的测试，并验证合并幂等。stub 当前未完整携带注解参数值，不能把物化出口的一致性当成最终 PSI 往返一致性。
- **C-4 仍未通过准入**：对本机 `C:/Users/lin17/.cangjie/sdks/` 下 `cangjie-1.0.5`、`cangjie-1.0.0`、`cangjie-0.53.18`、`cangjie-0.53.13` 递归枚举，分别有 275 / 281 / 405 / 403 个文件、46 / 46 / 55 / 55 个 `.cjo`，但 `.cj.d` 均为 **0**；大小写变体同样为 0，扫描错误为 0。三项宏统计均缺样本，**不是 0% 宏调用，也不是验证通过**。需要实际携带 `.cj.d` 和目标注解的 SDK；有宏时还需宏实现或展开结果。
- **缓存设计补充约束**：当前 provider、`CjoManager` 和 `CjoSearchPath` 没有完整包级失效协议。仅清 `contextCache` 会留下其它 symbol/name/buffer/negative 缓存。后续施工必须完整定义包级失效或 provider/session 重建边界，不能直接采用设计示意中“既有行为”的假设。
- **C-4 新样本核实（同日后续，覆盖上述缺样本状态）**：用户提供的 DevEco 仓颉插件 `26.0.0.821` 内 `harmonyos-cangjie-sdk-windows/cangjie/build-tools/modules/linux_ohos_aarch64_cjnative/std/` 含 38 个 `.cj.d`，全部有同目录同名 `.cjo`（该目录共 46 个 `.cjo`）。逐文件排除注释/字符串后，发现 6,497 个 `@!APILevel` 块；其它代码区注解为 Frozen 667、OverflowWrapping 186、ConstSafe 4、Intrinsic 1、OverflowThrowing 1，没有未知 `@` 名称或残留宏调用。43 个 `public macro` 是宏声明，另有 3 个 `macro package`，不是宏调用。APILevel 均在显式声明上；`std.unittest` 的 `Perf` 前有连续重复块，6,497 块对应 6,496 个目标位置。声明数属于只读结构统计，不冒充项目 parser AST 实测。
- **C-4 判定**：对本批真实 APILevel sidecar，允许继续按不展开宏的方案实施。当前文本没有待展开调用，不要求追溯显式声明在 SDK 构建前的历史生成来源。SysCap/Hide 此样本均无实例，不能宣称其真实 SDK 覆盖；任意宏生成声明仍属于 DIFF-5 已知限制。
- **真实语法修正**：样本为 `@!APILevel[since: "22"]`，不是普通调用形式；还存在 `throwexception: true`、`permission: "a" & "b"`。解析必须保留方括号、字符串 since、复合表达式、源码顺序及重复，不允许只提取方案 6.5 的简化示例。
- **解析与索引实现**：新增 `CjdSidecarParser`（磁盘 LightTree / 内存 PSI）、无树引用的 `CjdSidecarIndex`、注解原文/参数/UTF-16 范围及结构匹配键；顺序与重复保留，`matches` 与结构 `equals/hashCode` 分离。真实 SDK 实测 38 文件全部解析成功并提取 6,497 个 APILevel、零诊断（2026-09-17）。本轮修复：FIELD 成员遗漏、PSI 文档注释子语法误报、无体宏声明（`parseMacro` 缺体走 `reportMissingBody`，仅声明模式豁免）及 import 路径 `internal` 等软关键字误报（限定名段消费前重映射为标识符）。回归：`DeclarationFileParsingTest` 21、`DeclarationModeReverseParsingTest` 7、locator 9、parser 9、match key 4，全部通过。
- **二进制合并与接线已落地（同日后续）**：`CjdBinaryDeclarationMatcher` / `CjdBinaryTypeAdapter` 基于原始表建立 context 私有匹配计划，按 `allDecls` 索引在声明自身 publication 前追加注解；成员与参数不依赖父声明构造完成。`CjdAnnotationConverter` 保留源码顺序、重复、来源范围与不可转换表达式诊断。`CjoManager.loadPackageSnapshot` 统一包头、buffer 和实际选中路径；provider 与 `CjoDeclarationLoader` 均传入该来源，不从包名或触发用虚拟文件猜测 sidecar 路径。
- **生命周期契约**：本代按需读取并缓存；CJO、sidecar 或搜索根变化后，重建 `CjoSearchPath`、`CjoManager`、provider 及 session/module 整图，不提供仅清 context 的局部刷新。`testProviderCachesRemainIsolatedAcrossSidecarReplacement` 验证旧 provider 保持旧注解，新 provider 使用独立声明与注解节点。
- **合并验证（2026-09-17）**：`CjdBinaryAnnotationIntegrationTest` 5 tests 全部通过，覆盖 APILevel 消费、Frozen/overflow/deprecation publication、参数及嵌套成员、幂等缓存、歧义/缺失与二进制签名保持；`CjdDeclarationLoaderIntegrationTest` 1 test 通过，实际调用 provider 和反编译加载器，验证两份声明的有序注解名/参数值/原文逐项相等、不同名 VirtualFile 不影响 sidecar 选择、无物理路径的内存入口不加载 sidecar。
- **模块回归（2026-09-17）**：队列执行 `:cfir:cfir-serialization:test`（75 tests / 0 failures / 0 errors / 1 skipped）、`:analysis:decompiled:decompiler-to-stubs:test`（5 / 0 / 0 / 0）、`:analysis:decompiled:decompiler-to-file-stubs:test`（1 / 0 / 0 / 0）。序列化模块跳过项为未设置 `CANGJIE_CJD_SDK_DIR` 的真实 SDK parser 用例；前述 38 文件解析证据仍属于此前实测，不能当作本轮真实二进制合并验证。
- **真实 SDK 合并回归（2026-09-17，同日后续）**：设置 `CANGJIE_CJD_SDK_DIR` 后，队列执行序列化模块测试，14 suites / 80 tests / 0 failures / 0 errors / 0 skipped。38 个 sidecar 匹配 5,031 个 APILevel；生产 provider 加载 `std.math.RoundingMode`、6 个枚举构造项及 `toString`，availability 均为 since="22"。覆盖测试新增全包无 AMBIGUOUS 断言；此统计不代表所有源码注解都应匹配（仍受导出门控和类型元数据限制）。
- **Rune 类型身份修复**：`TypeKey.primitive` 不再将 Rune 折叠成 UInt8。官方 Type/Type 比较即使归一名称仍检查 kind，Type/Ty 则直接比较原始名称。最小重载回归经红→绿验证，SDK `std.core` 三处歧义消失，总匹配数从 5,014 增至 5,031。同步修正旧 parser 用例，使“修饰符/默认值/返回类型不影响键”使用相同参数类型，而不再暗含 Rune/UInt8 等价。
- **std.console 缺项归因**：`read(arr)`、`write(buffer)`、`writeln(buffer)` 的 sidecar 参数为 `Array<Byte>`，SDK `std.core` 定义 `Byte = UInt8`；实际 CJO Array 元素 kind 为 `TypeKind.Type`（28），不是 UInt8。当前 adapter 将此元数据返回 Unknown，主类型反序列化器亦不支持该 kind。真实 SDK 测试直接断言原始参数字段、Unknown 与拒绝匹配，禁止将未知类型当通配符；另有自包含测试固化已展开 UInt8 二进制与 Byte 源码键不匹配。不能仅凭别名文本把这三项解释成已展开 UInt8，也不能为提高覆盖数放宽匹配。
- **尚未完成**：~~完整声明种类边界复核、引用点 availability 诊断端到端验收~~（**两项已于 2026-09-18 完成**，见下），以及 P5/P6 的 IDE/LSP/DevEco 工作；当前不能把合并与接线测试通过等同于 P4 全部验收，也不能把 CFIR 注解相等视为最终 stub/PSI 参数往返相等。DevEco 新增 SDK 布局枚举测试目前在任务依赖阶段被无效 `.host/devEco-studio` 路径阻断，尚未运行到测试体。**边界复核后新增的未覆盖面**：`TYPE_ALIAS`/`MACRO` 注解数为 0、`EXTEND` 仅 1 个带注解样本、无 `FINALIZER`/`MAIN` 条目 ⇒ 这三类在本语料下**无证据**，不能宣称已验证。

#### P4 收尾之一：声明种类边界复核（2026-09-18）

**语料（可复现）**：DevEco 仓颉插件 `26.0.0.821` 内
`harmonyos-cangjie-sdk-windows/cangjie/build-tools/modules/linux_ohos_aarch64_cjnative/std/`
—— 38 个 `.cj.d` + 46 个 `.cjo`。测试以环境变量 `CANGJIE_CJD_SDK_DIR` 指向该目录启用。

**① 文本结构统计（已完成，不依赖编译）**

按括号深度切出"顶层声明区域"，剔除注释与字符串后统计 `@!APILevel[...` 的落点：

| 落点 | 数量 |
|---|---|
| APILevel 总数 | **6,497** |
| 顶层声明**自身**带 APILevel | 1,099 |
| 成员注解，其顶层声明**自身也有**注解 | 4,064 |
| 成员注解，其顶层声明**自身无**注解 | **1,334** |

统计脚本：会话工作区 `cjd_boundary_stats.py`（纯文本，无需编译器）。总数 6,497 与 1.4.2 记录的
6,497 **逐字吻合**，两个独立口径互证。

**结论 1（差额主因是官方同款门控，不是实现缺陷）**：
1,466 的未合并差额中，**1,334（90.9%）** 由官方 `MergeAnnoFromCjd.cpp:486`
（`toplevelDecl->annotations.empty()` 即 `continue` ⇒ 其成员不进 `memberMapping`）决定。
本仓 4.4.1 与 `CjdBinaryMatchPlan` 的 `sidecar.declarations.filter { it.annotations.isNotEmpty() && kind != MAIN }`
是该门控的逐字镜像。
⇒ **门控内上限 = 1,099 + 4,064 = 5,163**，实测合并 5,031 ⇒ 门控**之内**还剩 **132** 条待匹配层归因。

**结论 2（这条必须登记为显式已知限制）**：
**6,497 中的 1,334（20.5%）注解在任何实现下都不可能被合并**，涉及 **370 个顶层容器**
（其自身无 `@APILevel`）。这些容器的成员在 IDE 中**不会**因 APILevel 被提示，
且该行为与官方 `cjc` 一致。此前文档只有 4.6.5 一行"R13 门控"提到它，没有量化，
容易被误读成"覆盖率不足的实现缺陷"。⇒ 记为**已知限制**（与 `DIFF-*` 同级的显式差异登记），
并注明"若要在本仓放宽该门控，属偏离官方语义的独立决策，须单独评审"。
按文件分布（差额最大的前几个）：`std.overflow` 505、`std.core` 325、`std.unittest.prop_test` 137、
`std.convert` 62、`std.math` 60、`std.binary` 56。

**② 匹配层归因（已完成，实测）**

`cfir/cfir-serialization/test/.../cjd/CjdSdkDeclarationBoundaryAuditTest.kt`（新增）。
审计的不是覆盖率数字（随 SDK 版本漂移），而是**无静默丢失**这一硬约束：
38 个 sidecar 里每条带注解的条目都必须有归宿 —— 被合并 / 有指向它自己的匹配诊断 / 祖先已解释原因；
"够不到且无人解释"即静默丢失。运行命令：

```bash
CANGJIE_CJD_SDK_DIR=<std 目录> gradlew-queue.bat :cfir:cfir-serialization:test --tests '*CjdSdkDeclarationBoundaryAuditTest*'
```

实测（2026-09-18，1 test / 0 failures）：

| 归宿 | allAnnotations | APILevel |
|---|---|---|
| 已合并 | 5,656 | **5,031** |
| `ATTRIBUTED_SELF_MISSING`（匹配失败，有指向它自己的诊断） | 155 | **140** |
| `UNREACHABLE_BY_ANCESTOR_TOP_LEVEL_GATE`（官方门控，结构性） | 1,545 | **1,326** |
| `UNATTRIBUTED`（静默丢失） | **0** | **0** |
| 合计 | 7,356 | **6,497** |

⇒ 账目**逐条闭合**：`5,031 + 140 + 1,326 = 6,497`，且 `UNATTRIBUTED == 0`、`AMBIGUOUS == 0`。
**"无静默丢失"这条断言现在是可执行的回归防线。**

性质拆分（`1,466`）：
| 成因 | 数量 | 性质 |
|---|---|---|
| 官方同款**顶层门控** | 1,326 | 与官方逐字一致；见 ① |
| **导出门控**（R13，二进制侧 `NON_EXPORTED` 32 条） | ≤ 32 | 与官方 `IsExportedDecl` 同款 |
| **参数类型元数据不可解析** | ≈ 108 | 已知限制：`Array<Byte>` 的 CJO 元素 kind 为 `TypeKind.Type`，adapter 返回 Unknown；见 3378 行 std.console 归因 |

`ATTRIBUTED_SELF` 的 140 条按文件分布（可作为后续收窄的靶点）：
`std.reflect` 33、`std.net` 32、`std.io` 14、`std.unittest` 11、`std.database.sql` 8、`std.core` 6、
`std.fs` 6、`std.binary` 4、`std.crypto.cipher` 4、`std.unittest.common` 4、`std.unittest.mock` 4、
`std.console` 3、`std.crypto.digest` 3、`std.env` 3、`std.math.numeric` 2、`std.unittest.prop_test` 2、`std.random` 1。
`NON_EXPORTED` 的名字全部是包内实现细节（`std.core` 的 `arrayInitByCollection`/`array*OutOfBoundsExceptionMessage`/
`Future*`、`std.unittest` 的 `TestProgressCollector`/`*ProgressData` 等），符合 R13 的预期。

**声明种类实测分布**（38 个 sidecar 的条目数 / 注解数）：
`FUNCTION 4462 / 5300`、`PROPERTY 912 / 929`、`VARIABLE 538 / 538`、`EXTEND 372 / 1`、`CLASS 355 / 354`、
`INTERFACE 102 / 102`、`STRUCT 91 / 95`、`ENUM 37 / 37`、`MACRO 43 / 0`、`TYPE_ALIAS 7 / 0`。
要点：
① **`TYPE_ALIAS` 与 `MACRO` 在本语料中注解数为 0** ⇒ D6 的"类型别名不展开导致匹配失败"这一已知限制
**未被该语料覆盖**，不能据此宣称已验证；
② `EXTEND` 372 个但只有 1 个带注解，且已合并 ⇒ extend 键（extendedType + inheritedTypes）有效但样本极薄；
③ 不存在 `FINALIZER` / `MAIN` 条目 ⇒ P4 的 finalizer、main 分支在本语料下**无证据**。

**③ 引用点 availability 端到端验收（已完成，实测）**

`cfir/analysis-tests/tests/.../CjdSidecarAvailabilityDiagnosticTest.kt`（新增）。
真实 SDK 的 `.cjo` + 同目录 `.cj.d` 经**生产 provider**（`CfirDeserializedSymbolProvider`）加载并合并后，
在 `projectApiLevel = 20` 的源码会话里引用 `std.math.RoundingMode.Floor`（sidecar `since="22"`），
经 resolve → `runCheckers` 两段式，**断言产出 `APILEVEL_REF_HIGHER`**，并先断言
`UNRESOLVED_REFERENCE` 不出现（保证引用确实解析到了 `.cjo` 声明，否则不构成端到端）。

⇒ 4.7"sidecar 注解一旦合并，APILevel checker 自动受益、不做任何改动"由此从**推理**变为**实测**。

> **施工记录（两处非显然的装配要求，供后续同类测试复用）**：
> ① 库会话必须**自注册** `CfirSymbolProvider` / `CfirProvider` / `CfirExtendProvider` / `StructuredProviders`
> —— 反序列化 provider 解析跨包引用时查询的是**它自己的**库会话，不是源会话；
> ② 组件清单必须用生产入口 `CfirSession.registerCliCompilerAndCommonComponents(...)`
> （`cfir/entrypoint/.../session/ComponentsContainers.kt:86`），
> 手写会漏掉 `CfirLazyDeclarationResolver` 等 20 余个必需组件、且逐个报错发现。

### 5.2 破坏性改动的提交切分

本设计含 5 处破坏性改动，其中**仅有 1 处产生外部可见行为变化**。建议按下表切分提交，便于评审与独立回滚：

| 提交 | 内容 | 外部可见变化 | 可独立回滚 |
|---|---|---|---|
| **C-1** | `CjFile` 改为持有 `sourceKind` + `getFileType()` 归正（P0 内） | 无（`.cj` 行为逐字不变） | 是 |
| **C-2** | 解析模式改为解析器构造属性（P1 内） | 无 | 是 |
| **C-3** | **诊断文案修正**（P1 内，**必须单独提交**） | **有**：**9 条**错误信息文本变化（B 组 5 + A 组 4，见 4.2.6 更正③） | 是 |
| **C-4** | `CfirDeclarationChecker.requiresImplementation` 标记体系 + **生成器模板改动**（P3 内）。**生成物会整体重生成，diff 面较大** | 无 | 是（但需单独观察生成物 diff） |
| **C-5** | 清理 **9** 个零使用点的 `ParsingContext` 预设（**独立提交，不属本特性功能**） | 无 | 是 |

> **C-3 必须单独提交**：它是唯一改变用户可见行为的改动，若混进功能提交会被误认为"顺手改了错误信息"。
> 提交信息应给出改动前后对照与理由（"缺体"与"残缺输入"的语义分离，见 4.2.6）。

### 5.3 阶段依赖与顺序说明

- **P4 是本设计的核心**，也是风险最高的部分。它独立于 P0-P3 之外，**可以并行推进**；P1/P2/P3 不依赖 P4。
- **P2 必须紧接 P1**：若 `.cj.d` 能解析出无体成员、但 CFIR 仍把它们当隐式抽象，
  会得到"能打开但语义全错"的更糟状态。两者必须同批交付。
- **P3 的 checker 标记不宜延后**：它不阻断解析功能，但越晚做，需要打标记的 checker 越多（数量随业务增长）。
- **C-5（清理死预设）可以最先做**：不依赖任何其它改动，且能立刻降低后续实现者被误导的概率。

---

## 6. 测试策略

### 6.0 测试落点与基类约定（v2 新增）

v1/v2 初稿只给了"测什么"，没给"放哪里、用什么基类"。本仓的测试是有约定的，落地位置错了会导致
"用例写了但跑不起来"或"重复造基类"。以下为实测的落点表：

| 阶段 | 落点目录 | 既有同类先例 / 基类 | 测试依赖 |
|---|---|---|---|
| **P1 解析** | `psi/test/org/cangnova/cangjie/psi/`（**注意包名是 `psi`，不是 `parsing`**） | `DoubleColonParsingTest.kt`、`ModifierParsingTest.kt`、`ForeignAndAnnotationParsingTest.kt`、`EnumConstructorAnnotationParsingTest.kt` | `testImplementation(testFixtures(project(":tests:test-infrastructure")))` |
| **P2 CFIR 护栏（PSI 路径）** | `cfir/raw-cfir/psi2cfir/testFixtures/org/cangnova/cangjie/cfir/builder/` | `AbstractRawCfirBuilderTestCase.kt`、`AbstractRawCfirBuilderLazyBodiesTestCase.kt`、`AbstractRawCfirBuilderSourceElementMappingTestCase.kt` | `testFixturesApi(testFixtures(project(":tests:test-infrastructure")))` |
| **P2 CFIR 护栏（LightTree 路径）** | `cfir/raw-cfir/light-tree2cfir/testFixtures/...` | 该模块 **`testFixturesApi(testFixtures(project(":cfir:raw-cfir:psi2cfir")))`** ⇒ 两侧基类本来就成对共享 | 同上 |
| **P3 源收集** | `compiler/frontend/test/...` | 源收集/环境相关测试 | 同模块既有 test 配置 |
| **P4 sidecar** | `cfir/cfir-serialization/test/org/cangnova/cangjie/cfir/serialization/cjd/`（与既有 `.../cjo/` 平级） | `CjoPackageWriterTest.kt`、`CjoSearchPathTest.kt`；**端到端参照 `CjoSdkDeserializationIntegrationTest.kt`**（已有"真实 `.cjo` fixture"集成测试先例） | `testImplementation(libs.junit.jupiter)` |
| **P6 声明注入** | `analysis/decompiled/decompiler-to-stubs/test/...` | Analysis API 测试 | `testImplementation(testFixtures(project(":analysis:analysis-test-framework")))` |

**两条必须遵守的约定**：

1. **P2 的护栏测试要走「AST / Stub」双变体**。
   本模块既有 `AbstractRawCfirBuilderLazyBodiesByAstTest` 与 `...ByStubTest` 一对
   ⇒ 声明模式护栏（4.5.2 护栏 1）同样是**读取 PSI 是否经过 Stub** 会分叉的判定，必须按同一模式成对编写。
   命名建议：`AbstractRawCfirBuilderDeclarationModeByAstTest` / `...ByStubTest`。
2. **新增抽象测试基类要经由 `TestGeneratorForPsi2Cfir.kt`**。
   本仓的 `ProjectTestsExtension.testGenerator` 会把 `generateTestGeneratorTests` 挂到 `compileTestKotlin` 依赖上
   ⇒ 只写抽象基类、由生成器产出变体测试，**不要手写两份重复的变体类**。

**执行命令**（遵循 `AGENTS.md`：并发场景一律走队列包装器）：

```bash
gradlew-queue.bat :psi:test --tests "*CjdDeclaration*"
gradlew-queue.bat :cfir:raw-cfir:psi2cfir:test --tests "*DeclarationMode*"
gradlew-queue.bat :cfir:raw-cfir:light-tree2cfir:test --tests "*DeclarationMode*"
gradlew-queue.bat :cfir:cfir-serialization:test --tests "*CjdSidecar*"
```

### 6.1 双向验证原则

本特性最大的测试风险是"把 `.cj` 也放宽了"。因此**每一个抑制点都必须有反向用例**：同一段文本，在 `.cj` 下必须报错，在 `.cj.d` 下必须不报错。

### 6.2 解析层测试矩阵（P1 / P2）

| 用例 | `.cj` 期望 | `.cj.d` 期望 |
|---|---|---|
| `class C { func f(): Int64 }` | 解析为 abstract 成员 | 解析为无体、**非** abstract 声明 |
| `class C { func f() }`（无返回类型） | 报 abstract 需返回类型 | 无诊断 |
| `class C { init() }` | 报缺函数体 | 无诊断，且**不**打 `HAS_BROKEN` |
| `class C { prop p: Int64 }` | 无诊断（抽象属性） | 无诊断（**仍标 abstract**） |
| `interface I { prop q: Int64 { get() } }` | 报 getter 缺体 | 无诊断 |
| `let g: Int64`（顶层） | psi 层**不报**；由语义层 `CfirFileStaticGlobalInitializationChecker` 承担 | 两边都不报（该 checker 在声明模式跳过，4.4.5） |
| `const c: Int64` | psi 层**不报**；由语义层 `CfirConstVariableInitializerChecker` 承担 | 同上 |
| `main()`（无体） | 报缺函数体 | 无诊断 |
| `~init()`（finalizer 无体） | 报缺函数体 | 无诊断 |
| `func f(): Int64 { 1 }`（顶层有体） | 无诊断 | 无诊断（有体本就合法） |
| 同一文件同时含 `.cj` / `.cj.d` 语义 | — | 需独立文件，模式按**文件**而非片段生效 |

> **P2 补充用例**：对 `.cj.d` 的无体类成员函数，断言其在 CFIR 中的 `attributes` **不含** implicit abstract，且不因该属性触发 override/实例化类诊断。
>
> **P2 反向用例（防"护栏加错分支"）**：对 `.cj.d` 中的 `class C { prop p: Int64 }`（无 `{}`、无 getter/setter），
> 断言其在 CFIR 中**仍然**带 implicit abstract。这条与上一条构成**互斥断言对**，
> 用于锁定 R3/R4 的不对称性；若实现者把护栏加在了 `when` 的整体入口（而不是 `CjNamedFunction` 分支），本用例会失败。
> LightTree 路径需有独立的一份相同断言（两个函数是分开实现的）。

> **P2-DEFAULT 用例组（v3 定稿新增 —— 对应 A-1 / 1.3.5）**：
> 这一组必须**四格齐全**，缺任何一格都无法锁定"`DEFAULT := !isAbstractEffective`"这一实现。
> 两个属性位要在同一条断言里同时校验，否则"两个谓词各自算"的旧实现仍可能蒙混过关。
>
> | # | 输入 | `.cj` 期望 | `.cj.d` 期望 |
> |---|---|---|---|
> | D1 | `interface I { func f(): Int64 }` | `ABSTRACT=true`、**`DEFAULT=false`** | `ABSTRACT=false`、**`DEFAULT=true`** |
> | D2 | `interface I { func f(): Int64 { 1 } }` | `ABSTRACT=false`、`DEFAULT=true` | 同 `.cj`（有体本就合法） |
> | D3 | `class C { func f(): Int64 }` | `ABSTRACT=true`、`DEFAULT=false` | `ABSTRACT=false`、**`DEFAULT=false`**（非 interface 作用域） |
> | D4 | `interface I { prop p: Int64 }` | `ABSTRACT=true`、`DEFAULT=false` | **`ABSTRACT=true`**、`DEFAULT=false`（R4 不被 DEFAULT 逻辑破坏） |
>
> D1 的右格是本轮复核的核心新增项；D3 的右格用于锁死"`DEFAULT` 只在 `INTERFACE_BODY` 置位"；
> D4 的右格是"护栏只作用于函数分支"的**第二重守卫**（与上一条反向用例同源）。
> LightTree 与 PSI 两条路径各跑一份。

> **P1 守卫用例（v3 新增 —— 防"误豁免残缺输入"）**：
> 以下输入在 `.cj` 与 `.cj.d` 中**都必须报错**，且报的是**修正后的**诊断文案（缺标识符 / 缺参数列表）：
>
> | 输入 | 期望诊断 |
> |---|---|
> | `class C { func }` | 缺标识符（**不得**再报"函数体缺失"） |
> | `main }` | 缺参数列表 |
> | `init }` | 缺参数列表 |
> | `macro }` | 缺标识符 |
> | `class C }` | 缺 class body |
>
> 这 5 条是 4.2.6 B 组诊断修正的**唯一验收手段** —— 若修正后仍报"函数体缺失"，说明"缺体"与"残缺输入"没有被分开，
> 抑制逻辑迟早会误伤。
>
> **P1 边界用例（v3 新增 —— 防"抑制范围外溢"）**：`reportMissingBody` 只抑制"缺体"，不得影响其它诊断：
>
> | 输入（`.cj.d`） | 期望 |
> |---|---|
> | `prop p: Int64 { get() { 1 } }` | 正常解析（getter 有体） |
> | `class C { let x }`（既无类型又无初始化器） | **仍报** `parsing.error.field.requires.type.or.initializer`（4.2.6 结论：该项不豁免） |
> | `class C { let x: Int64 }` | 无诊断 |
> | `synchronized` 表达式缺块 | **仍报**（4.2.6 排除清单） |

### 6.3 后缀判定测试（P0）

```
✓ "a.cj.d"     → DECLARATION
✗ ".cj.d"      → SOURCE      （官方 HasCJDExtension 的 fileIt != fileEnd 检查）
✗ "a.cjd"      → SOURCE
✗ "a.CJ.D"     → SOURCE      （大小写敏感）
✗ "a.cj.d.txt" → SOURCE
✗ "a.cj"       → SOURCE
✗ "dir.cj.d/x.cj" → SOURCE
```

### 6.4 sidecar 测试矩阵（P4）

**路径推导**

| 用例 | 期望 |
|---|---|
| `lib/ohos.arkui.cjo` | `lib/ohos.arkui.cj.d` |
| `/a/b/x.cjo` | `/a/b/x.cj.d` |
| `/a.cjo.dir/x.cjo` | `/a.cjo.dir/x.cj.d`（**DIFF-4** 的反例） |
| 目录中无 `.cj.d` | `null`（静默） |
| `.cj.d` 存在但不可读 | `null`（静默，对齐 R9） |

**匹配与合并**

| 用例 | 期望 |
|---|---|
| 顶层类 + 类注解 | 注解合并到 `.cjo` 的类声明 |
| 顶层函数 + 函数注解 | 合并 |
| 成员函数 + 成员注解 | 合并 |
| 函数参数注解 | 合并到对应序号的参数 |
| 主构造函数（`.cj.d` 侧 `init`）与 `.cjo` 侧 `FUNC_DECL "init"` | 合并 |
| 重载函数（同名不同参数类型） | **按参数类型精确匹配**，不串位（D6 / DIFF-2） |
| 泛型函数（形参名相同，约束数 > 上界数） | 不越界，匹配正确（DIFF-1） |
| `extend T : I` 的注解 | 按 extendedType + inheritedTypes 匹配 |
| 宏调用声明的注解 | 按 fullName + identifier 匹配 |
| `var` / `prop` 的注解 | 按类型匹配 |
| 类型别名出现在参数类型中 | **不匹配**（对齐官方限制，D6） |
| `.cj.d` 中有注解但 `.cjo` 无对应声明 | 静默跳过，不报错（对齐 `Debugln` 行为） |
| `.cjo` 中声明的注解（原生的） | **不被覆盖**，与 sidecar 注解**合并** |
| 目标为非导出声明 | 不匹配（R13） |
| `MAIN_DECL` 上的注解 | 不参与（R13） |
| 类型参数上的注解 | **不合并**（对齐官方阶段覆盖范围，记录为已知限制） |
| 属性 getter/setter 上的注解 | **不合并**（同上） |

**生命周期**

| 用例 | 期望 |
|---|---|
| `.cj.d` 内容变化后重新分析 | 注解随之更新 |
| `.cj.d` 删除后重新分析 | 注解消失，`.cjo` 回到原生状态 |
| `.cjo` 变化 | 索引与注解一并重建 |
| 会话结束 | 注解随 provider 缓存回收（R14 的等价实现） |
| **同一 `.cjo` 被两个 provider 加载**（v2：已从"验收项"降级为**回归防线**） | 断言两 provider 的同一 `declIndex` 声明对象 `!==`（即各自持有一份），且给 A 注入的 sidecar 注解不出现在 B 的声明上。**当前实现已满足**（4.6.6 证据链），此用例用于**防止将来把 `declCache` 提升为跨 provider 共享** |
| **合并点顺序回归**（v2 新增） | 断言 sidecar 注解的 `annotationTarget` 已被归一化（覆盖 `:157-159` 约束 1）、`publishAnnotationInfo()` 的快照包含 sidecar 注解（约束 2）、且 sidecar 自带 `@Annotation` 时**不产生重复 marker**（约束 3） |
| **`.cj.d` 自带注解 + `.cjo` 原生注解并存** | 两侧注解**都在**（`replaceAnnotations(原 + sidecar)` 是追加，不是覆盖），且不出现 `annotationTarget` 不一致 |

### 6.5 端到端语义测试

用一个最小化的 `.cjo` + `.cj.d` 对：

```cangjie
// demo.cj.d
package demo

@APILevel(since: 12)
public func f(): Unit

@APILevel(since: 12, syscap: "SystemCapability.Demo.X")
public class C {
    public func g(): Unit
}
```

断言：
1. 合并后 `.cjo` 的 `f` / `C` / `C.g` 上带有 `ohos.labels.APILevel` 注解；
2. `projectApiLevel = 10` 时，引用 `f` 触发 `APILEVEL_REF_HIGHER`；
3. `syscapEnabled = true` 且 syscap 未被授予时，引用 `C` 触发 `APILEVEL_SYSCAP_ERROR`；
4. 把 `.cj.d` 移除后，上述诊断全部消失（证明注解确实来自 sidecar）。

> 这一组用例同时验证 R12、R14 与 1.4.7 的消费链。

### 6.6 IDE 侧验收（P5）

| 检查 | 期望 |
|---|---|
| 打开 `x.cj.d` | Cangjie 语法高亮生效，文件类型显示为 `CangJieDeclaration` |
| **`x.cj.d` 的 `CjFile`** | `sourceKind == DECLARATION` 且 `isDeclarationFile == true`，`getFileType() === CangJieDeclarationFileType`（OP1 断言） |
| **`x.cj` 的 `CjFile`**（反向断言） | `sourceKind == SOURCE`、`isDeclarationFile == false`，`getFileType() === CangJieFileType` |
| **`x.cj.d` 走的是既有 `createFile` 分支**（B-4） | `createFile` **未被修改**，但断言结果仍为上面的 `CjFile`（证明"不改也对"） |
| `.cj.d` 中的无体函数 | **无** `Function body expected` 波浪线（4.9.2 更正后唯一能同时排除"静默降级"与"退化成纯文本"的断言） |
| `.cj.d` 中的无体 `main` / 主构造 / getter / setter | 同样**无**波浪线（覆盖 4.2.6 A 组 7 个豁免点） |
| **`class C { func }`（缺函数名）**（反向） | **仍报错**（4.2.6 B 组未被误豁免）；同理 `main }`、`init }`、`macro }`、`class C }` 也必须仍报错 |
| `.cj.d` 中的 `synchronized` 表达式缺块（反向） | **仍报错**（4.2.6 排除清单未被误豁免） |
| `.cj.d` 中的 prop | 高亮/折叠/括号匹配正常 |
| `.cj.d` 内部符号导航 | 可用 |
| `.cj.d` 是否出现在声明提供者聚合 | **否**（D7） |
| `.cj.d` 是否加入 `classesRoots` | **否** |
| **源码根下的 `.cj.d` 是否进入项目 Stub 索引符号** | **否**（OP2 / 4.9.3） |
| **`.cj.d` 与 `.cjo` 并存时的权威声明** | 来自 `.cjo`（D7 + 4.9.3 排除），**无重复符号告警** |
| `x.cj.d` 与 `x.cjo` 并存时 | 无重复符号告警 |
| 普通项目构建 | 流程不变，不感知 `.cj.d` |

> **C-2 新增：被排除出索引之后，`.cj.d` 的"打开态能力"必须单独验收。**
>
> 4.9.3 把源码根下的 `.cj.d` 排除出 Stub 索引，其代价**不会**体现在上面任何一条"不出现在…"的断言里 ——
> "不在索引里"与"打开后还能正常用"是**两件不同的事**。官方也不存在"同一模块里 `.cj` 与 `.cj.d` 并存"的模式
> （R1 是两模式互斥输入），因此这条没有官方先例可依，必须自己验收：
>
> | # | 能力 | 期望 | 若不成立说明 |
> |---|---|---|---|
> | 1 | 打开 `.cj.d` 并查看结构（折叠、括号匹配、高亮） | 正常 | 与索引无关，预期通过 |
> | 2 | **编辑** `.cj.d`（改一个类型名、加一个成员） | 无异常、无假报错 | 解析正确 |
> | 3 | 文件**内**补全（成员名、类型名） | 可用 | 文件内 PSI 可解析 |
> | 4 | **跨文件**补全 / 跳转（`.cj.d` → 其它 `.cj.d` 或 `.cj`） | 可用 | ← **最可能因"不在索引中"而失效**，重点验收 |
> | 5 | 在 `.cj.d` 中**查找引用** | 可用（至少文件内） | 索引依赖项 |
> | 6 | `.cj` 中跳转到 SDK 声明 | 落到 `.cjo`（权威），而非 `.cj.d` | D7 的正确行为 |
>
> 若第 4/5 项失败，需要重新权衡"排除索引"与"索引内冲突"的取舍（4.9.3），
> 而不是简单地"为了不冲突就牺牲能力" —— 这是 P5 的一个**真实决策点**，不是验收细节。

---

## 7. 风险与开放问题

### 7.1 已识别的有意差异（必须评审确认）

| 编号 | 差异 | 理由 | 影响 |
|---|---|---|---|
| **DIFF-1** | 泛型约束 `upperBounds` 比较用 `upperBounds.size()` 作为上界，官方用外层约束数 | 官方写法在 `upperBounds.size() < genericConstraints.size()` 时越界 | 本仓更严格正确；不改变可匹配集合 |
| **DIFF-2** | 用确定性签名键匹配，官方用 `unordered_map + find_if` | 官方匹配顺序不确定；类型别名场景下会漏配 | 本仓确定性更强；可能出现"官方漏配、本仓配上"的情况 |
| **DIFF-3** | `.cj.d` 建 Stub 索引，官方参考实现对声明文件用非 Stub 类型 | 本仓 `.cj.d` 需要完整导航/补全能力 | **v2：风险已从两个方向关闭** —— 逻辑方向不进声明提供者聚合（D7，`CjWorkspaceModelSync` 只对 `CjPackage.Binary` 加 `.cjo`/lib，`CjWorkspaceModelSync.kt:1374-1381`）；索引方向对源码根下 `*.cj.d` 加排除（4.9.3 新增缺口 + 6.6 验收项）。剩余残余风险：`.cj.d` 被**手工**放进源码根且排除规则失效时仍可能建索引，由 6.6 的对应断言守住 |
| **DIFF-4** | `.cj.d` 路径推导只作用于文件名，官方用 `rfind(".cjo")` 截断整条路径 | 官方写法在目录名含 `.cjo` 时截断错误 | 本仓更保守正确 |
| **DIFF-5** | sidecar 路径**不做宏展开**，官方会做 | 本仓宏展开是 CFIR 层重型操作，在库加载路径引入会把"加载"变成"编译" | **`.cj.d` 中由宏生成的声明无法参与匹配**。必须作为已知限制记录；若确有需要，可追加"宏展开后重解析"增量 |

### 7.2 已知限制（与官方一致，不修正）

| 限制 | 取证 |
|---|---|
| 类型别名出现在被比较的类型中会导致匹配失败 | `MergeAnnoFromCjd.cpp:169` |
| 类型参数上的注解不参与合并 | `MergeAnnoFromCjd.cpp` 三阶段均未覆盖 |
| 属性 getter/setter 上的注解不参与合并 | 同上 |
| 返回类型不参与匹配（依赖"仓颉不允许仅返回类型不同的重载"这一语言约束） | `IsSameDeclByIdentifier` 未比较返回类型 |
| 修饰符不参与匹配 | 同上 |

### 7.3 开放问题（v2：全部已决策并关闭）

> v1 此节 5 条全部停留在"建议/后续项"。v2 逐条做完取证并给出**可施工的结论**。
> 结论分布：**1 条改为"本次就改"**（OP1 由"不改"翻转为"改"，理由见下）、2 条确认为"不改"、2 条给出可执行的后续路径。

#### OP1 —— `CjFile.getFileType()` 硬编码问题（v1 结论被推翻）

- **现状**：`CjCommonFile.getFileType(): FileType = CangJieFileType.INSTANCE`（`psi/.../psi/CjFile.kt:66`）。
- **v2 决策：本次改为 `viewProvider.fileType`。**
- **爆炸半径已枚举（实测，可复核；B-3 复核后修正了两处计数口径）**：
  - `psi` 内 `getFileType()` 的覆写共 **2 处**：
    | 位置 | 归属 | 受本次改动影响？ |
    |---|---|---|
    | `psi/src/.../psi/CjFile.kt:66` | `CjCommonFile.getFileType()` | **是**（本次唯一改动点） |
    | `psi/src/.../macro/file/CangJieMacroCallFileType.kt:34` | `CjMacroCallFile.getFileType()`（**子类自覆写**） | 否（覆写优先） |
    > ⚠️ 初稿称"覆写只有这一处，另一处是 `FileType` 自身的接口实现、与 `CjFile` 无关"——**不准确**。
    > `CangJieMacroCallFileType.kt:34` 正是 `CjMacroCallFile`（`CjFile` 的子类）的 `getFileType()` 覆写，
    > 与 `CjFile.kt:66` 属**完全同类**。结论不变（它自己覆写了，不受基类影响），
    > 而且**它恰好是支持本决策的现成正向论据**：本仓已有"子类返回自身真实 FileType"的实现，OP1 不是新做法。
  - `CangJieFileType.INSTANCE` 的引用共 **7 处**：`utils/virtualFileUtil.kt:39`、`psi/CangJieDeclarationUseScopePolicy.kt:47`、
    `psi/CjCodeFragment.kt:187/199/307`、`psi/CjFile.kt:66`、`psi/CjPsiFactory.kt:760`。
    **其中没有一处读取 `PsiFile.getFileType()`** —— 它们要么自己去查 `FileTypeRegistry`（`virtualFileUtil.kt:39`），
    要么在**构造** `LightVirtualFile` 时把 `CangJieFileType.INSTANCE` 作为参数传入（`CjCodeFragment` / `CjPsiFactory`）。
    后者的行为在改动后**完全不变**（那些文件本来就不是 `.cj.d`）。
    > 计数口径：初稿 OP1 说"6 处"却列了 7 个位置；8.7 条 1 说"其余 5 处"却列了 6 个。**本版统一为：共 7 处，其中 1 处（`CjFile.kt:66`）是改动点，其余 6 处不动。**
- **安全性论证**：`CjFile` 只在 `CangJieParserDefinition.createFile` 的 `is CangJieFileType ->` 分支被创建
  （4.9.2），因此任何 `CjFile` 的 `viewProvider.fileType` **只可能是** `CangJieFileType` 或 `CangJieDeclarationFileType`。
  改动后，`.cj` 仍返回 `CangJieFileType.INSTANCE`（行为不变），`.cj.d` 返回 `CangJieDeclarationFileType.INSTANCE`（**这正是期望值**）。
  ⇒ **不存在第三种取值，因此不存在未知爆炸半径。**
- **不改的代价（升级为缺陷）**：会造成 `psiFile.getFileType() ≠ viewProvider.fileType` 的**自相矛盾状态**。
  D1 已选择"独立 FileType 复用 Language"，而 `getFileType()` 硬编码会把 FileType 维度重新抹平成"都是 `.cj`"，
  使 D1 的一半收益失效，且任何依赖 `psiFile.fileType` 的通用 PSI 逻辑（图标、编辑器 tab、`FileTypeRegistry` 反查、`isCangJieFileType` 语义）都会把 `.cj.d` 当 `.cj`。
- ⇒ **动作**：改为 `override fun getFileType(): FileType = viewProvider.fileType`，纳入 P5。
  6.6 验收项新增："`.cj.d` 的 `CjFile.getFileType()` 返回 `CangJieDeclarationFileType`；`.cj` 的仍返回 `CangJieFileType`"。

#### OP2 —— `.cj.d` 是否进入 `CjWorkspaceModelSync` 的 source roots

- **v2 决策：不进入（保持 v1 结论），并补上 v1 缺失的"索引侧"约束。**
- 实测：`CjWorkspaceModelSync.kt:560-573` 的 `expectedSourceRoots` 来自 `cjModule.sourceSets`（**目录粒度**），
  所以"`.cj.d` 不会被建成独立 source root"是**结构性自动成立**的，无需改动。
- 但 source root 是目录 ⇒ **`.cj.d` 若物理躺在源码目录里，仍然会被 Stub 索引**（因为它是 CangJie language 的 PSI 文件）。
  v1 把这条风险完全漏掉了 ⇒ 处置见 4.9.3 的"v2 新增缺口"（加 `*.cj.d` 排除 + 验收项）。
- ⇒ **动作**：不改 source roots；新增源码根下 `*.cj.d` 的 exclude；6.6 新增对应验收项。

#### OP3 —— `deveco` 与 `intellij-ide` 的收集器重复代码

- **实测**：两份文件**存在且内容相同**（逐行比对 `:100-112` 一致）：
  - `intellij-ide/modules/ide/base/src/main/kotlin/.../CaIdeScopeCangJieFileCollector.kt`
  - `deveco/modules/ide/base/src/main/kotlin/.../CaIdeScopeCangJieFileCollector.kt`
    两者的 import 集合也一致（`org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType`、`org.cangnova.cangjie.utils.isCangJieFileType`）。
- **v2 决策：本次不收敛为共享模块，改用「同步注释 + 一致性校验」双保险。**
- **不收敛的理由（不是"后续再说"，而是有明确障碍）**：`intellij-ide/` 与 `deveco/` 是**两个独立 Gradle 构建**
  （见 `AGENTS.md` §4 末尾：子项目独立构建，不在主 `settings.gradle.kts` 内）。
  提取共享源码需要新建一个被两个构建同时依赖的模块，并改动两套构建脚本与发布链路 —— 这属于构建基础设施改造，**超出本特性范围**，
  且它的收益（消除 12 行重复）远小于风险。
- **动作（可执行、可验收）**：
  1. 两处都加上完全相同的 KDoc（4.9.3 已给出待插入注释原文），并**显式写明"本文件存在同名副本，改动必须同步"**；
  2. 在 7.4 复核清单中加入一条**两文件正文一致性校验**（比较排除头部版权注释后的正文哈希），
     可作为 CI 检查或提交前手工核对；
  3. 登记为独立技术债（"两个构建的目标文件重复"），不阻塞本特性。

#### OP4 —— `AbstractFrontendPipeline` 是否顺带补齐

- **v2 决策：不补（保持 v1 结论）。**
- 实测复核：`compiler/frontend/src/.../pipeline/AbstractFrontendPipeline.kt:12-41` 仅有一个抽象类定义
  （`execute` + `runPhasedPipeline` + 抽象 `createCompoundPhase`），**无任何子类**，也无调用方。
- ⇒ `.cj.d` 支持不依赖它（D9）。补齐泛用流水线是独立课题，混入本特性会显著扩大改动面与评审成本。
- ⇒ 本特性的 `compiler/frontend` 改动**只有源收集过滤**（4.4.2 / 4.4.3），不碰 pipeline 目录。

#### OP5 —— `.cjo` 的 `derivedCjdPath` 是否在 IDE UI 上可见

- **v2 决策：不进入本次范围，但给出可执行路径（使"后续项"不再是空头承诺）。**
- 4.9.4 已在 `CjDependency.Binary` 上加 `derivedCjdPath`，**数据来源已经就绪**。
- 若要展示，落点是依赖树/依赖描述渲染层，**只需读该属性并拼一行文案**，不涉及任何本设计的其它部分。
  ⇒ 因此它可以**完全独立地**在任何时候追加，无前置依赖，也不影响功能正确性。
- ⇒ 动作：本次不实现，登记为"可选增强（无前置依赖）"。

#### OP6（v2 新增）—— 类型别名导致 sidecar 匹配失败是否应在本仓修复

- **v2 决策：不修复，与官方保持一致**（D6 已定），但 v1 未把它列进开放问题，v2 显式登记为**已知限制**而非"遗漏"。
- 依据：`MergeAnnoFromCjd.cpp:169` 明确记载"若标识符是类型别名之后的名称，比较不会成功"。
  修复它需要类型解析/别名展开，超出 sidecar 的职责边界，且会让本仓与官方语义分叉。
- ⇒ 动作：无代码改动；6.4 保留"类型别名出现在参数类型中 ⇒ 不匹配"用例作为**基线固化**（防止将来有人"顺手修好"从而偏离官方）。

### 7.4 交付前必须复核的清单

#### A. 通用

- [ ] 每一条 R1–R15（1.5 语义红线）都有对应的可执行测试用例
- [ ] **1.3.5 的 `DEFAULT` 结论已落地**：`D1/D2/D3/D4` 四格用例齐全（见 6.2 的 P2-DEFAULT 组）
- [ ] 每一个 `DIFF-*` 都在评审记录中确认（含"F6 抽公共类方案**否决**"这条"不实施"的差异记录）
- [ ] `external/` 下无任何改动
- [ ] `build/` 下产物**未被用作设计依据**（4.9.1 的 `cjd` 草稿教训）
- [ ] 所有涉及 `cfir/checkers/gen/**` 的改动都落在**生成器**上，而非生成物（A-4）

#### B. 解析层（P1）

- [ ] **`reportMissingBody` 是唯一的解析期抑制点**：全仓不存在第二处 `sourceKind.isDeclaration` 形式的诊断抑制判断
- [ ] A 组 **7 处**全部改为调用 `reportMissingBody`（逐个对照 4.2.6 A 表）
- [ ] **B 组 5 处文案已修正**；守卫用例（`class C { func }`、`main }`、`init }`、`macro }`、`class C }`）
      在 `.cj` 与 `.cj.d` 下**都报错**、且报的是**修正后的文案**（4.2.6 B 表）
- [ ] 4.2.6 排除清单的 **5** 个点（`:635`、**`:3004`**、`:3994`、`:4021`、`:4124`）**未被误豁免**
- [ ] **抑制点计数自检**（B-2）：`grep -n "parsing.error.function.body.expected\|parsing.error.expecting.symbol" CangJieParsing.kt`
      应命中 **17 处**，其中 A 组 7 / B 组 5 / 排除 5 —— 数字对不上说明清单过期
- [ ] `parsing.error.field.requires.type.or.initializer`（`:2297`）保持**不**豁免
- [ ] `ParsingContext` 中**没有** `isDeclarationFile` 字段、**也没有** `DECLARATION_FILE` 预设（A-2）；
      模式一律来自 `CangJieParsing.sourceKind`
- [ ] `CangJieParser.parse` 按 `sourceKind` 分派，`name.endsWith(".cj.macrocall")` 已删除（C-1，消除双真源）
- [ ] `CangJieLightParser` 已透传 `sourceKind`

#### C. CFIR 层（P2 / P3）

- [ ] 护栏**只加在函数分支**：`isImplicitAbstractClassLikeMember` 的 `CjProperty` 分支与
      `isImplicitAbstractClassLikeProperty` **未被改动**（R4 不被破坏）
- [ ] **`.cj.d` 输入下不出现 `MISSING_FUNC_BODY`**（由 `CfirMemberBodyDeclarationChecker` 报出）——
      它是护栏 1 是否正确生效的**行为级证据**（4.4.5 更正四）
- [ ] **`DEFAULT` 与 `ABSTRACT` 同源**（A-1）：`isDefault` 由 `isAbstractEffective` 取反派生，
      代码中**不再存在** `hasBody()` / `hasSyntaxBody()` 形态的 `isDefault` 谓词
- [ ] `DEFAULT` 派生在**两条路径都改**：PSI（`PsiRawCfirBuilder.kt:3860/3872`）、
      LightTree（`:805-806`、`:959-960`）
- [ ] `requiresImplementation` 标注在 **`CfirDeclarationChecker<D>`** 上（本仓**不存在** `CfirChecker` 接口，A-4），
      覆盖 4.4.5 A 表 **11** 项（**不含** `CfirMemberBodyDeclarationChecker` —— 它靠自身的 `isAbstract` 守卫自动沉默，
      且必须保持启用作为护栏 1 的回归探测器）
- [ ] 分派过滤**改在生成器模板**（`cfir/checkers/checkers-component-generator`），
      **不得**手改 `cfir/checkers/gen/**`（A-4）
- [ ] **新增 checker 的默认行为正确**：临时加一个空 checker，验证它"不需要知道 `.cj.d` 存在"
- [ ] 源收集两级互斥：`compileCjd=false` 只收 `.cj`、`=true` 只收 `.cj.d`，两者都不收对方
- [ ] **源收集的两处都要放行**（A-3）：`filter` 与 `convertToSourceFiles`。
      只改 `convertToSourceFiles` 会被 `filter` 拒掉，**改动看起来生效实则无效**

#### D. sidecar（P4）

- [ ] 合并点位于 `CfirDeclDeserializer.kt:153` 与 `:154` **之间**（target 归一化 / 快照完整 / marker 不重复 三条同时满足）
- [ ] 跨 provider 对象不共享（6.4 生命周期矩阵的回归防线）
- [ ] **两条 `.cjo → 声明` 路径的注解一致性**（C-3）：`cfir-serialization` 与 `analysis/decompiled`
      各自物化出的对象，注解集合**逐项相等**（或证明二者是同一对象）
- [ ] `project-model` 的 `derivedCjdPath` 使用 `CjSourceKind.DECLARATION_SUFFIX` 常量，**未写 `".cj.d"` 字面量**（4.9.4）

#### E. IDE 与跨仓（P5）

- [ ] `.cj.d` 的 `sourceKind == DECLARATION` 且 `getFileType() === CangJieDeclarationFileType`；`.cj` 反之
- [ ] 打开 `.cj.d` **无 `Function body expected` 波浪线**
- [ ] 源码根下的 `*.cj.d` 已被排除出 Stub 索引
- [ ] **被排除出索引后，`.cj.d` 的编辑能力不退化**（C-2）：打开 / 编辑 / 补全 / 跨文件跳转 / 查找引用各验收一次 ——
      "不在索引里" 与 "打开后能力正常" 是**两件事**，6.6 只断言了前者
- [ ] `deveco` 与 `intellij-ide` 的收集器**正文一致**（OP3 的一致性校验）
- [ ] **主仓已发布 `cangjie-frontend-common-for-ide` / `-psi-for-ide` 到 `build/repo`，
      且 `intellij-ide` 侧以 `--refresh-dependencies` 重新解析**（4.10；漏掉会表现为"找不到类"的假故障）
- [ ] `.cj.d` 不出现在声明提供者聚合、`classesRoots`、Stub 索引符号中

> ⚠️ **已从本清单删除（v2 残留，A-3/B-4）**：原 E 组前 3 条要求
> 「`cangjie-filetypes.xml` / `createFile` 中声明分支位于 `is CangJieFileType` **之前**」。
> 在 v3 的 `sourceKind` 派生方案下：
> ① `createFile` **根本不需要改动**（`is CangJieFileType ->` 会接住子类，`CjFile` 从 `viewProvider.fileType` 自得 `DECLARATION`，见 4.9.2 改写版）；
> ② "声明分支必须前置"这条顺序要求**随之消失**，照做反而会诱导实现者加一个多余的 `when` 分支。
> 原第 2 条（"不是纯文本"）在 v2 就被 4.9.2 更正过，已由"无波浪线"这一可区分断言取代。

#### F. 版本与提交纪律

- [ ] **C-3（B 组 5 处文案修正）单独提交**，提交信息含改动前后对照（5.2）
- [ ] **C-5（清理 **9** 个死 `ParsingContext` 预设）独立提交**，不混入功能提交（B-1：不是 11 个）
- [ ] **8.7「施工级不要动清单」逐条确认**

---

## 8. 框架正确性评估与破坏性改动索引

> **本章的作用**：集中给出"为什么这样设计"的依据，供评审逐条核对；并列出本设计的破坏性改动、破坏半径与提交切分。
>
> **第 3–4 章给出的是【已采纳本章结论之后】的设计**，不是待评估的方案。
> 本章的价值在于：① 提供可复用的框架正确性判据；② 说明每处破坏性改动的必要性、破坏半径与风险控制；
> ③ 明确列出**不建议**破坏的部分，防止后续打着"框架正确"的旗号过度重构。
>
> **阅读建议**：只施工 ⇒ 读 8.2（总览）+ 8.4（排序与风险控制）；
> 评审设计本身 ⇒ 8.1（判据）+ 8.3（逐项详述）是核心。
>
> 作为对照：如果不做这些改动，方案会退化成"纯增量"——**每新增一个同类实体都要人工枚举一遍判断点**。
> 这正是框架不正确的定义。各处的量化对比见 8.6。

### 8.1 判据：怎么判断"框架是否正确"

用这 5 条自检，任何一条不满足，就说明该处是"纯增量在掩盖框架缺陷"：

| # | 判据 | 含义 |
|---|---|---|
| J1 | **新增同类实体时，是否需要重新枚举判断点？** | 若"再加一个无体 owner（如 `~init`、`operator func`）"时需要重新想"它要豁免吗"，则框架未表达该语义 |
| J2 | **同一事实是否有多个来源？** | 真源分裂 ⇒ 迟早不一致 |
| J3 | **局部作用域是否会意外清除全局模式？** | 全局属性借用局部机制承载 ⇒ 随时可能被覆盖 |
| J4 | **新增一个同类实现者（新 checker / 新解析器）的作者，是否需要知道 `.cj.d` 的存在？** | 若需要，说明该语义没有被框架吸收，而是在每个实现者身上重复 |
| J5 | **两条同类路径（PSI / LightTree、intellij-ide / deveco）是否共享同一机制？** | 不共享 ⇒ 每次变更都要"同步两处"，必然漂移 |

### 8.2 评估结果总览

| # | 议题（若不做框架改动会退化成什么） | 违反判据 | **本设计的处置** | 破坏半径 | 落点 |
|---|---|---|---|---|---|
| **F1** | 缺体诊断散落在 **17** 个上报点（A 7 + B 5 + 排除 5），其中 5 个还**误用了同一条诊断消息** ⇒ 抑制只能逐点人工判断 | J1 J4 | ✅ **已采纳**：统一 `reportMissingBody()` 入口；修正 B 组 5 处诊断语义 | `CangJieParsing.kt` 7 个调用点 + 5 条文案 + 1 个私有方法 | 4.2.6 |
| **F2** | 模式作为"输入参数"传进来 ⇒ 必须新增第 3 个文件级入口，且调用点必须选对 | J3 J5 | ✅ **已采纳**：模式上移为 `CangJieParsing` 的不可变构造属性 | 2 个工厂方法 + 2 个入口 + 调用点 | 4.2.3 / 4.2.5 |
| **F3** | 把"是否声明文件"塞进 `ParsingContext` 字段 ⇒ 被任何局部 `with` 静默清除 | J3 | ✅ **已采纳**：模式完全不进 `ParsingContext`（`ParsingContext` 只承载**语法入口参数**，与文件种类正交，4.2.5） | 同 F2 | 4.2.3 / 4.2.5 |
| **F4** | 同一事实 4 处表达（枚举 / `CjFile` 布尔 / `ParsingContext` 布尔 / `compileCjd`） | J2 | ✅ **已采纳**：`CjSourceKind` 唯一真源；`CjFile` 直接持有枚举，`isDeclarationFile` 退化为派生属性 | **仅 3 处**（构造 / 子类点，见 8.3-F4） | 4.2.2 |
| **F5** | 12 个 checker 各自 early-return ⇒ 新增 checker 的作者必须记得"要给 `.cj.d` 加豁免" | J1 J4 | ✅ **已采纳**：`CfirDeclarationChecker.requiresImplementation` 标记 + 生成器统一过滤（判定依据为文件种类） | 1 个基类属性 + **11** 个 checker 各 1 行 + 生成器模板 1 处 | 4.4.5 |
| **F6** | `project-model` 内联复制 `.cjo → .cj.d` 路径推导 | J5 | ❌ **评估后否决**：抽公共类的收益（消除 3 行）小于"把库元数据布局知识塞进最底层 `common`"的代价 ⇒ 保留内联 + 复用常量 | — | 4.9.4 |
| **F7** | PSI 与 LightTree 各自维护"模式从哪来" | J5 | ✅ **已采纳**（F2 的自然结果）：两条路径都从文件取 `sourceKind`，判定同源 | 2 个 object | 4.2.5 末 |

> **F6 为什么被否决**，值得单独说明：它看起来最"符合单一真源"，但**框架正确性要求的是"语义归属正确"，不是"函数必须唯一"**。
> 真正需要唯一的不是那个函数，而是**后缀字面量** —— 复用 `CjSourceKind.DECLARATION_SUFFIX` 即已满足（3.1 原则 5 也只约束"判定实现"与"字面量"）。
> 这个反例说明：**判据不能机械套用**，否则会从"纯增量"滑向"过度抽象"。

### 8.3 逐项详述

#### F1：`reportMissingBody()` —— 让"体缺失"成为一个概念（**最重要的框架修正**）

**问题**：若不采纳本项（即退回"逐点加 `if`"），就必须把 12 个上报点人工分成 A/B/C 三组、
逐个判断"这里要不要豁免"。为什么需要这么费劲？因为**"缺体"在本仓解析器里不是一个概念，
只是 12 处重复的错误上报**。

**框架缺陷的实证**：B 组 5 点（`func }`、`main }`、`init }`、`macro }`、`class C }`）报的是
`parsing.error.function.body.expected` —— 但它们的真实问题**不是缺体，而是缺名字/缺参数列表**。
`parseFunction` 的 `:3814-3817` 在跳过 `func` 关键字后立刻判断 `at(RBRACE)`，此时连函数名都没有，
却复用了"函数体缺失"的文案。这是**诊断语义误用**，也是为什么必须人工分组的原因。

**破坏性改动**：

```kotlin
// 1) 新增统一上报入口（唯一抑制点）
private fun reportMissingBody(owner: String, position: Int) {
    if (sourceKind.isDeclaration) return          // ← 唯一的声明模式判断
    error(CangJieParsingBundle.message("parsing.error.function.body.expected"), position)
}
```
```kotlin
// 2) A 组 7 处改为调用它
reportMissingBody("function", lastToken.End())
```
```kotlin
// 3) B 组 5 处的诊断语义修正为各自真实缺失
//    parseFunction:  "func }" → 改为缺标识符
error(CangJieParsingBundle.message("parsing.error.expecting.identifier"))
//    parseMainFunc / parseInitFuncRest / parsePrimaryInitFunc / parseMacro 同理
```

- **为什么这是"框架正确"而非"多此一举"**：抑制逻辑从"7 处 × 人工判断分组"变成"1 处 + 语义自洽"。
  将来新增体 owner（例如 `operator func`、`~init` 的新形态），**只要走 `reportMissingBody` 就自动正确**（J1 满足）；
  B 组的残缺输入**永远不会**经过该入口，因此**在结构上不可能被误豁免**（不再依赖评审者的记忆力）。
- **破坏半径**：`CangJieParsing.kt` 内 7 个调用点 + 5 条诊断文案 + 新增 1 个私有方法 + 诊断资源新增 1 个键。
- **代价（必须承认）**：会**改变 5 处既有错误信息文本**，若有测试断言了旧文案需同步更新。
  这是本方案唯一的"外部可见行为变化"，需在评审时确认（见 8.4 的风险控制）。
- **对第 4 章的替换**：采纳后，4.2.6 的"A/B/C 三组"从**施工必读**降级为**背景说明**；
  施工只需"改 7 处调用 + 改 5 处文案"。

#### F2 / F3：模式不该住在 `ParsingContext` 里

**关键实证（这改变了整个判断）**：

```
全仓 ParsingContext.XXX 使用统计：
   6  DEFAULT          （CangJieParser 的 6 个 code fragment / 文件入口）
   1  ANNOTATION_ONLY  （parseOnlyAnnotationFile）
   —— 其余 11 个预设（REPORT / SILENT / IF_WHILE_CONDITION / MATCH_EXPRESSION_MODE /
      FUNCTION_LITERAL_BLOCK / FUNCTION_LITERAL_COLLAPSED / MACRO_BACK_TOKEN /
      NO_STRING_INTERPOLATION / LEGACY …）在全仓没有任何使用点。
```

⇒ 两个结论：

1. **`ParsingContext` 的"局部细粒度开关"定位是名义上的**。它有 15 个字段、11 个预设
   （实测；初稿写作 20 / 14，见 B-1 更正），实际职责只有一件：**给一个文件级入口选一种解析模式**。
   把 `.cj.d` 再塞进去，是用一个已经过度设计、且实际只当"模式枚举"用的结构去承载第三种模式。
2. **F3 的隐患是真实的但当前未触发**：`ParsingContext` 的每个预设都是**全新实例**，
   因此 `IF_WHILE_CONDITION` 这类预设的 `isDeclarationFile` 会回到默认 `false`。
   今天没有调用点，所以不发作；但一旦有人（包括后续的 `.cj.d` 实现者自己）在文件解析路径上
   `with(ParsingContext.IF_WHILE_CONDITION)`，**声明模式会静默失效**，且症状是"某些位置的诊断突然出现"，极难定位。
   ⇒ 把**文件级恒定属性**放在一个**随时可能被局部替换**的载体里，本身就是框架错配。

**破坏性改动**：

```kotlin
// CangJieParsing 持有文件种类（不可变）
class CangJieParsing(
    builder: SemanticWhitespaceAwarePsiBuilder,
    isTopLevel: Boolean,
    isLazy: Boolean,
    languageModuleName: String,
    val sourceKind: CjSourceKind,        // ← 新增：文件级恒定
) { ... }

// 工厂方法带上它
fun createForTopLevel(
    builder: SemanticWhitespaceAwarePsiBuilder,
    languageModuleName: String = "",
    sourceKind: CjSourceKind = CjSourceKind.SOURCE,
): CangJieParsing = CangJieParsing(builder, true, true, languageModuleName, sourceKind)
```

- **收益**：模式**只有一个来源**且**不可能被局部覆盖**（J3 满足）；`parseFile` / `parseOnlyAnnotationFile`
  不再各自硬编码模式，`ParsingContext` 回归它的真实职责（语法局部开关）。
- **顺带清理（破坏性但正确）**：**9** 个死预设应当删除（B-1 修正的计数）。保留死代码会让下一个实现者误以为
  "这套机制支持细粒度模式"，从而继续往上堆字段。**这是一次性收敛，不是本特性的功能需求，可独立提交。**
- **破坏半径**：`CangJieParsing` 的 2 个工厂方法 + 3 个文件级入口 + `CangJieParser` / `CangJieLightParser` 的调用点。
- **对第 4 章的替换**：采纳后 4.2.3（`ParsingContext.isDeclarationFile` 字段）**不再需要**；
  4.2.5 的"新增第三个入口"简化为"两个入口各自传 `sourceKind`"，且 LightTree 侧的阻塞（V2-3）**自动消失**
  —— 因为模式不再依赖 `with(...)` 的嵌套，`CangJieLightParser.parse` 只要透传一个参数即可。

#### F4：`CjSourceKind` 必须是唯一真源

**现状**：同一事实有 4 处表达（`CjSourceKind` 枚举 / `CjFile.isDeclarationFile` 布尔 /
`ParsingContext.isDeclarationFile` 布尔 / `CompilerConfiguration.compileCjd` 布尔）。

**破坏半径实测 —— 这是本方案最"划算"的一处**：

```
CjFile 定义：psi/src/org/cangnova/cangjie/psi/CjFile.kt:392
open class CjFile(viewProvider, isCompiled = false, val isCodeFragment = false)
构造 / 子类点全仓仅 3 处：
  1. CangJieMacroCallFileType.kt:20      （CjMacroCallFile : CjFile(...)）
  2. CjCodeFragment.kt:53                （CjCodeFragment : CjFile(...)）
  3. CangJieParserDefinition.kt:110      （CjFile(viewProvider, false)）
```

**破坏性改动**：`CjFile` 直接持有 `val sourceKind: CjSourceKind`（由 `viewProvider.fileType` 推导），
`isDeclarationFile` 退化为 `sourceKind.isDeclaration` 派生属性（保留该名字以兼容既有调用点）。

- **收益**：真源唯一（J2）；用枚举替代布尔，为"未来第三种源"留出空间（J1）；
  且与 F2 合并后，`sourceKind` 从文件一路流到解析器，**中间没有任何 Bool 中转**。
- **破坏半径**：3 处构造点（`createFile` 在 v3 下**无需改动**，见 4.9.2）。
- **对第 4 章的替换**：采纳后 `CjFile` 不再有 `isDeclarationFile` 构造参数，只保留派生属性。
- ⚠️ **一处自我更正**：本项曾主张"既然只有 3 个构造点，就不必引入 `CjSourceKindCarrier` 这个过渡契约"。
  **该判断是错的**：`CjPsiSourceFile` 在 `common`、`CjFile` 在 `psi`，依赖方向 `psi → common`
  反向不成立 ⇒ `common` 无法判断"这个 PSI 文件是不是声明文件"，**只能由 `psi` 实现一个契约**。
  判据是"如果没有它，是否还存在别的正确实现方式"——没有。`CjSourceKindCarrier` 是**依赖倒置的必需品**，
  不是"为避免破坏而引入的兼容壳"（3.3-D8 / 4.1.3 已改正）。

#### F5：checker 体系缺少"实现完备性"分类维度

**现状**：第 4.4.5 A 表列了 12 个需要 early-return 的 checker。我当时的结论是
"分组注册粒度太粗，所以逐 checker early-return"——**这是回避问题**。
真正的缺陷是：**checker 体系只有一个分类维度（声明种类），而"实现完备性检查 vs 接口/类型检查"是另一个正交维度，框架没有表达它。**

**实证**：`DeclarationCheckers` 是 `abstract class`，
槽位全是"按声明种类"分的（basic / callable / function / classLike / property / constructor / extend …），
注册是**集合粒度**（`useCheckers(CommonDeclarationCheckers)`，`CheckersContainers.kt`）。
⇒ 想按"实现完备性"关一批 checker，粒度对不上，只能下沉到每个 checker 内部。

**破坏性改动**（**注意：本项在 2026-09-16 的复核后做了三处施工层更正，见 4.4.5 的 A-4 更正一/二/三**）：

```kotlin
// 加在真实的基类上（本仓不存在 CfirChecker 接口）
// cfir/checkers/src/.../checkers/declaration/CfirDeclarationChecker.kt
abstract class CfirDeclarationChecker<D : CfirDeclaration> {
    /**
     * 该 checker 是否检查"实现的完备性"（依赖函数体 / 初始化器 / 访问器实现的存在性）。
     * 声明文件（`.cj.d`）下这类检查整体跳过 —— 对齐官方 Sema 豁免。
     * 默认 false：新增 checker 默认参与全部模式。
     */
    open val requiresImplementation: Boolean get() = false
}
```

过滤点在**生成器模板**（`checkers-component-generator/.../Generator.kt:391-413` 的 `printDiagnosticComponentCheckMethod()`），
判定依据是**元素所属文件的种类**，不是会话级开关：

```kotlin
if (checker.requiresImplementation && element.isFromDeclarationFile(context)) continue
```

- **收益（J1 + J4）**：豁免点 12 → 1（标记收敛为 11 项）；**新增 checker 的作者不必知道 `.cj.d` 的存在**，
  只需在写"依赖实现的检查"时声明一次标记，默认行为就是正确的。
- **破坏半径**：1 个基类属性 + 11 个 checker 各 1 行 + 生成器模板 1 处（**不含**手改 `gen/**`）。
- **对第 4 章的替换**：采纳后 4.4.5 的 A 表从"12 处逐点 early-return 清单"变为
  "11 个 checker 打标记"——施工量相近，但**语义层级不同**：前者把知识散在 12 个实现里，后者把它固化为框架能力。
- **复核带来的三处修正**（都不改变本项的取向，但都是施工阻断级）：
  ① 标记加在 `CfirDeclarationChecker<D>`，而非不存在的 `CfirChecker`；
  ② 过滤必须落在生成器模板，而非"分派处"（后者是写着"请勿手动修改"的生成物）；
  ③ 判定依据是文件种类而非 `configuration.compileCjd` —— 后者是会话级布尔，
     在"`.cj` 与 `.cj.d` 同时可见"的 IDE 会话里**无法表达**（C-2）。

#### F6：`common` 抽类方案 —— **评估后否决**（保留内联）

**曾考虑**：`project-model` 引不到 `cfir-serialization`（4.9.4 已证），于是内联复制了路径推导；
而 `.cjo → .cj.d` 的推导是**纯字符串运算**，不依赖任何 CFIR 类型 ⇒ 看起来应该下沉到零依赖的 `common` 让两边共用。

**否决理由**（完整论证见 4.9.4）：

1. 实际使用点只有 2 个，其中 `project-model` 因依赖边界**必须**自己实现 ⇒ 抽类只能消除 **3 行**重复。
2. `common` 是全局最底层模块，为"库元数据布局"这类知识新增公共类型，会把它塞进所有模块的编译面。
3. **单一真源的目标已用更小的手段达成**：真正需要唯一的不是"函数"，而是**后缀字面量** ——
   复用 `CjSourceKind.DECLARATION_SUFFIX` 常量即已满足（3.1 原则 5 的原文也只约束"判定实现"与"字面量"）。

**这条否决记录本身就是判据的一部分**：框架正确性要求的是**语义归属正确**，不是"函数必须唯一"。
机械套用 J5 会从"纯增量"一路滑到"过度抽象"——两者同样破坏框架。

#### F7：两条解析路径的模式判定必须同源

`CangJieParser`（PSI）与 `CangJieLightParser`（LightTree）今天各自决定"用哪种模式"。
F2 落地后，两者都只需从**文件**取 `sourceKind` 并传给解析器 ⇒ 判定同源（J5 满足），
`CangJieLightParser` 的阻塞（V2-3）也随之消解。

### 8.4 破坏性改动的排序与风险控制

按"收益 / 风险"排序，建议按此顺序推进：

| 序 | 改动 | 是否阻断功能 | 外部可见行为变化 | 建议 |
|---|---|---|---|---|
| 1 | **F4**（`CjFile` 持有 `sourceKind`） | 是（F2 依赖它） | 无 | 先做，破坏半径最小（3 处） |
| 2 | **F2/F3**（模式上移到解析器实例） | 是 | 无 | 紧随 F4；顺带删 **9** 个死预设可**独立提交** |
| 3 | **F1**（`reportMissingBody` 统一入口） | 是 | **有**：5 处错误文案变化 | 需评审确认；是本方案唯一有外部可见变化的改动 |
| 4 | **F5**（checker 标记） | 否（可后补） | 无 | 可与主体并行；越晚做，散弹越多 |
| 5 | **F6**（路径工具下沉 `common`） | 否 | 无 | 低风险，可随时做 |
| 6 | **F7**（两解析路径同源） | 否 | 无 | F2 的自然结果，无需单独排期 |

**风险控制**：

- **F1 的文案变化**是唯一需要"对外解释"的项。建议单独提交、单独评审，并在提交信息中给出
  "改动前 / 改动后"的对照，避免被误认为"顺手改了错误信息"。
- 所有破坏性改动都应**先补测试再改**：本仓已有双向验证约定（6.1），
  F1 的反向用例恰好就是 B 组 5 个残缺输入（`class C { func }` 等）**必须仍报错**。
- **不要为了破坏而破坏**：本章只主张 7 处；第 4 章其余落点（sidecar、IDE 注册、依赖模型）本身是
  增量式新增能力，不存在"框架错配"，保持增量即可（见 8.5）。

### 8.5 明确**不**建议破坏的地方（防止过度重构）

| 项 | 为什么保持增量 |
|---|---|
| `CangJieDeclarationFileType` 新增 FileType（D1） | 这是**新增能力**，不是"在旧结构里塞新语义"。IDE 的 FileType 扩展点就是为这种场景设计的 |
| `CjoPackageWriter` / `.cjo` 格式 | 不动（D9，`.cjo` 由外部 `cjc` 产出），无框架问题 |
| `CfirDeclDeserializer` 的 sidecar 合并点 | 是**新增一条数据来源**，不改变既有语义；合并点位置由既有代码顺序决定，属正确适配 |
| sidecar 的 `DeclarationMatchKey` | 是对官方不确定匹配的**有意严格化**（DIFF-2），属新增实现，非框架问题 |
| `AbstractFrontendPipeline` | 既有骨架未接通，与本特性正交（OP4），不在此处补 |
| `deveco` / `intellij-ide` 的两份同名收集器 | 根因是"两个独立构建"（OP3），修它需要构建基础设施改造，**收益（12 行）远小于风险**；F6 的"下沉 common"不适用于它（那是源码级复用，受构建边界限制） |

### 8.6 采纳本章后的净效果

| 指标 | 第 3–4 章方案（纯增量） | 采纳本章后 |
|---|---|---|
| 需要逐点人工判断的解析抑制点 | 16（分 A/B/C 三组） | **1**（`reportMissingBody`） |
| 需要逐点 early-return 的 checker | 12 | **1**（分派处过滤） |
| "是否声明文件"的事实来源 | 4 处 | **1 处**（`CjSourceKind`） |
| 文件级解析模式的载体 | `ParsingContext` 字段（可被局部覆盖） | 解析器实例不可变属性 |
| 新增同类实体时的默认行为 | 需要人工枚举判断 | **默认正确**（J1 满足） |
| 两条解析路径的模式判定 | 各自实现 | 同源 |
| 死代码 | 保留 **9** 个未使用的 `ParsingContext` 预设（B-1） | 清理 |

### 8.7 施工级「不要动」清单（从 v2 继承，防误操作）

> **为什么单独列**：8.5 是**设计层面**的"不建议破坏"，本清单是**施工层面**的"不要顺手改"。
> 两者不重叠 —— 前者防止过度重构，后者防止"改 A 时顺手动了 B"。
> 本设计的破坏性改动面较广（改解析器构造、改 checker 接口、改诊断文案），**误伤风险随之升高**，
> 因此这份清单需要在施工时逐条对照。

| # | 位置 | 约束 | 依据 |
|---|---|---|---|
| 1 | `CangJieFileType.INSTANCE` 的其余 **6** 处引用（`virtualFileUtil.kt:39`、`CangJieDeclarationUseScopePolicy.kt:47`、`CjCodeFragment.kt:187/199/307`、`CjPsiFactory.kt:760`）；以及 `CangJieMacroCallFileType.kt:34` 的 `getFileType()` 覆写 | **一律不动**。本次只改 `CjFile.kt:66` 一处 | 4.2.2 爆炸半径枚举（B-3 修正口径） |
| 2 | `isDefaultInterfaceMember`（PSI）与 LightTree 的对应函数 | **不动**。两条路径判定结果本来就一致，加护栏只是噪声 | 4.5.2 护栏 2 |
| 3 | `hasSyntaxBody`（`LightTreeRawCfirDeclarationBuilder.kt:3323-3324`）与其上的 lazy body 防御注释 | **不动**。它与声明模式护栏正交（护栏 3） | 4.5.2 护栏 3 |
| 4 | `parseSynchronized` / `parseForeign` / `parseEnumBody` 的缺块诊断；`parseMacro` 宏体诊断 | **不动**。这些不是 P1；宏体问题属独立课题（R4） | 4.2.6 排除清单（5 条） |
| 5 | `parsing.error.field.requires.type.or.initializer`（`CangJieParsing.kt:2297`） | **不动**。官方 P2/P3 未覆盖该语义 | 4.2.6 结论 |
| 6 | `CjoPackageWriter` 与 `.cjo` 写出格式 | **不动**。`.cjo` 由外部 `cjc` 产出（D9） | D9 |
| 7 | `AbstractFrontendPipeline` 及其 `pipeline/` 目录 | **不动**。既有骨架未接通，与本特性正交 | OP4 |
| 8 | `external/` 下一切内容 | **只读取证，不修改** | 项目硬约束 |
| 9 | `build/` 下一切内容 | **是产物**：不作为改动目标，也**不得作为设计依据** | 4.9.1（`cjd` 草稿教训） |
| 10 | 两份 `CaIdeScopeCangJieFileCollector` 的**正文逻辑** | 本次只加同步注释与一致性校验，**不改排除策略** | OP3 |

> **注意第 5 条与 4.2.6 的关系**：4.2.6 明确列出"其它 `expecting.symbol "{"` 上报点不豁免"，
> 那是指**抑制逻辑**不改；第 5 条指**诊断本身**不改。两者一致，但角度不同，故分开表述。
>
> **注意第 10 条与 F6 的区别**：F6 讨论的是"是否把路径推导抽成公共类"（否决）；
> 第 10 条说的是"两份收集器的正文逻辑不动"。二者都因**跨构建边界**而保持现状，但对象不同。

---

## 附录 A：官方源码取证索引

| 主题 | 文件:行 |
|---|---|
| `-d` 选项定义 | `external/cangjie_compiler/include/cangjie/Option/Options.inc:245-247` |
| `compileCjd` 选项装配 | `external/cangjie_compiler/src/Frontend/FrontendOptions.cpp:46` |
| `GlobalOptions.compileCjd` 字段 | `external/cangjie_compiler/include/cangjie/Option/Option.h:721` |
| 输入分类真值表 | `external/cangjie_compiler/src/Option/Option.cpp:733-755` |
| `.cj` 输入处理 | `external/cangjie_compiler/src/Option/Option.cpp:656-673` |
| `.cj.d` 输入处理 | `external/cangjie_compiler/src/Option/Option.cpp:675-688` |
| `.cj.d` 后缀判定 | `external/cangjie_compiler/src/Utils/FileUtil.cpp:230-245` |
| `.cj.d` 后缀常量 | `external/cangjie_compiler/include/cangjie/Utils/ConstantsUtils.h:45` |
| 声明模式编译实例 | `external/cangjie_compiler/include/cangjie/FrontendTool/CjdCompilerInstance.h:20-63` |
| 实例分流 | `external/cangjie_compiler/src/FrontendTool/FrontendTool.cpp:199-206` |
| 编译阶段编排 | `external/cangjie_compiler/src/FrontendTool/FrontendTool.cpp:128-154` |
| `parseDeclFile` 语义 | `external/cangjie_compiler/include/cangjie/Parse/Parser.h:73-86` |
| `DiagMissingBody` 抑制 | `external/cangjie_compiler/src/Parse/ParserDiag.cpp:1216-1225` |
| `DiagConstVariableExpectedInitializer` 抑制 | `external/cangjie_compiler/src/Parse/ParserDiag.cpp:1084-1091` |
| `DiagExpectedInitializerForToplevelVar` 抑制 | `external/cangjie_compiler/src/Parse/ParserDiag.cpp:1111-1118` |
| `DiagMissingPropertyBody` 抑制 | `external/cangjie_compiler/src/Parse/Parser.cpp:396-406` |
| 属性无 `{}` 即 abstract | `external/cangjie_compiler/src/Parse/Parser.cpp:340-394` |
| `CheckFuncBody` 豁免 | `external/cangjie_compiler/src/Parse/ParseDecl.cpp:1533-1562` |
| finalizer 无体 | `external/cangjie_compiler/src/Parse/ParseDecl.cpp:532-543` |
| constructor 无体 | `external/cangjie_compiler/src/Parse/ParseDecl.cpp:575-587` |
| 属性访问器无体 | `external/cangjie_compiler/src/Parse/ParseDecl.cpp:347-357` |
| `ParseFuncBody` 无 `{` 分支 | `external/cangjie_compiler/src/Parse/ParseDecl.cpp:1668-1713` |
| sidecar 路径推导 | `external/cangjie_compiler/src/Modules/ImportManager.cpp:289-295` |
| 路径登记调用点 | `external/cangjie_compiler/src/Modules/ImportManager.cpp:297-310` |
| `cjdFilePaths` 字段 | `external/cangjie_compiler/include/cangjie/Modules/ImportManager.h:327-330, 453` |
| sidecar 触发时机 | `external/cangjie_compiler/src/Frontend/CompileStrategy.cpp:293-298` |
| sidecar 处理流程 | `external/cangjie_compiler/src/Frontend/CompileStrategy.cpp:300-332` |
| 合并入口 | `external/cangjie_compiler/src/Frontend/MergeAnnoFromCjd.cpp:475-572` |
| 方向注释（l 来自 `.cj.d`） | `external/cangjie_compiler/src/Frontend/MergeAnnoFromCjd.cpp:357` |
| 顶层挂接 | `external/cangjie_compiler/src/Frontend/MergeAnnoFromCjd.cpp:477-518` |
| 成员挂接 | `external/cangjie_compiler/src/Frontend/MergeAnnoFromCjd.cpp:527-549` |
| 参数挂接 | `external/cangjie_compiler/src/Frontend/MergeAnnoFromCjd.cpp:550-567` |
| 完整性校验 | `external/cangjie_compiler/src/Frontend/MergeAnnoFromCjd.cpp:463-472, 569-571` |
| `IsSameDeclByIdentifier` | `external/cangjie_compiler/src/Frontend/MergeAnnoFromCjd.cpp:358-461` |
| `IsSameFuncByIdentifier` | `external/cangjie_compiler/src/Frontend/MergeAnnoFromCjd.cpp:271-302` |
| `IsSameType` | `external/cangjie_compiler/src/Frontend/MergeAnnoFromCjd.cpp:26-269` |
| 类型别名限制 | `external/cangjie_compiler/src/Frontend/MergeAnnoFromCjd.cpp:169` |
| `Rune`/`UInt8` 名称归一后仍要求 kind 相同，不等价 | `external/cangjie_compiler/src/Frontend/MergeAnnoFromCjd.cpp:143-154` |
| 注解清理 | `external/cangjie_compiler/src/Sema/CheckAPILevel.cpp:284-304, 708-709` |
| `@IfAvailable` import 校验 | `external/cangjie_compiler/src/Sema/CheckAPILevel.cpp:252-282` |
| `IfAvailable` 脱糖 | `external/cangjie_compiler/src/Sema/CheckAPILevel.cpp:309-337` |
| Sema 豁免：声明属性 | `external/cangjie_compiler/src/Sema/DeclAttributeChecker.cpp:284-291, 332-336` |
| Sema 豁免：初始化 | `external/cangjie_compiler/src/Sema/LegalityOfUsage/InitializationChecker.cpp:503, 1517` |
| Sema 豁免：main 入口 | `external/cangjie_compiler/src/Sema/TypeChecker.cpp:2215-2230` |
| Sema 豁免：未实现接口成员 | `external/cangjie_compiler/src/Sema/InheritanceChecker/StructInheritanceChecker.cpp:830-842` |
| 宏实参重解析 | `external/cangjie_compiler/src/Macro/MacroEvaluation.cpp:296-310, 365-387, 562, 834, 866` |
| 宏展开重解析 | `external/cangjie_compiler/src/Macro/MacroExpansion.cpp:364` |
| 增量日志/缓存跳过 | `external/cangjie_compiler/src/Frontend/CompilerInstance.cpp:143-158, 325-341, 446-456` |
| ASTWriter 声明模式 | `external/cangjie_compiler/src/Modules/ASTSerialization/ASTWriter.cpp:915-935` |
| ASTWriter 配置 | `external/cangjie_compiler/include/cangjie/Modules/ASTSerializationTypeDef.h:31` |

## 附录 B：本仓取证索引

| 主题 | 文件:行 |
|---|---|
| `CjSourceFile` | `common/src/org/cangnova/cangjie/CjSourceFile.kt:16-129` |
| 真/假来源（与文件种类正交） | `common/src/org/cangnova/cangjie/source/CjSourceElement.kt:17-50, 754` |
| 二进制来源元素 | `common/src/org/cangnova/cangjie/source/CjBinarySourceElement.kt:18-63` |
| `CangJieFileType`（`open`） | `psi/src/org/cangnova/cangjie/lang/CangJieFileType.kt:10-42` |
| `CangJieLanguage` | `psi/src/org/cangnova/cangjie/lang/CangJieLanguage.kt:33-55` |
| `CangJieBuiltInFileType`（`.cjo`） | `psi/src/org/cangnova/cangjie/lang/declarations/CangJieDeclarationsFileType.kt:37-77` |
| 方言语言先例 | `psi/src/org/cangnova/cangjie/lang/CangJieMacroCallLanguage.kt:21-43` |
| 方言 ParserDefinition 先例 | `psi/src/org/cangnova/cangjie/parsing/CangJieMacroCallParserDefinition.kt:51-118` |
| `CangJieParserDefinition` | `psi/src/org/cangnova/cangjie/parsing/CangJieParserDefinition.kt:54-131` |
| 文件感知解析入口 | `psi/src/org/cangnova/cangjie/parsing/CangJieParser.kt:147-172` |
| 解析驱动（`doParseContents`） | `psi/src/org/cangnova/cangjie/psi/stubs/elements/CjFileElementType.kt:92-98` |
| `ParsingContext` | `psi/src/org/cangnova/cangjie/parsing/AbstractCangJieParsing.kt:85-168` |
| `parseFile` / `parseOnlyAnnotationFile` | `psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt:1310` / `:1280` |
| `parseFunction`（定义 `:3801-3880`；**缺体诊断 `:3815`、`:3876`**） | `psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt:3801-3880` |
| `parseProperty`（**缺属性体诊断 `:2780-2781`**） | `psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt:2759-2786` |
| `parseFunctionBody`（**缺体诊断 `:4157`**） | `psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt:4149-4159` |
| `parseInitFunctionBody`（**`mark.error :4137` / `error :4145`**） | `psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt:4133-4147` |
| `parsePrimaryInitFunc`（**缺体诊断 `:3595`、`:3642`**） | `psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt:3590` |
| `parseMainFunc`（**缺体诊断 `:3755`、`:3777`**） | `psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt:3750-3780` |
| `parsePropertyGet` / `parsePropertySet`（**缺体诊断 `:2838` / `:2879`**） | `psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt:2821-2842` / `:2860-2893` |
| `parseMacro`（**诊断 `:4089`、`:4124`**） | `psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt:4084-4127` |
| `CjFile` | `psi/src/org/cangnova/cangjie/psi/CjFile.kt:47-393` |
| `CjDeclarationWithBody` | `psi/src/org/cangnova/cangjie/psi/CjDeclarationWithBody.kt:32-80` |
| 后缀工具 | `psi/src/org/cangnova/cangjie/utils/virtualFileUtil.kt:34-40` |
| 配置 Key 与扩展属性 | `compiler/config/src/org/cangnova/cangjie/config/CommonConfigurationKeys.kt:57-63` |
| `CompilerConfiguration.useFir`（**配置层，非 CLI 参数**） | `compiler/config/src/org/cangnova/cangjie/config/CommonConfigurationKeys.kt:178-180` |
| CLI 参数 DSL（**4 条**） | `compiler/arguments/src/org/cangnova/cangjie/arguments/description/compilerArguments.kt:10-46` |
| 参数生成接线 | `compiler/frontend/build.gradle.kts:20-31` |
| 生成的参数类（**6 条 = 4 + 2 合成**） | `compiler/frontend/gen/org/cangnova/cangjie/frontend/arguments/CommonCompilerArguments.kt:7-44` |
| 生成器合成参数注入 | `compiler/frontend-arguments-generator/src/org/cangnova/cangjie/frontend/arguments/generator/Main.kt:116` |
| 源收集（LightTree） | `compiler/frontend/src/org/cangnova/cangjie/frontend/sources/GroupedCjSources.kt:85-166` |
| 源收集（PSI） | `compiler/frontend/src/org/cangnova/cangjie/frontend/environment/coreEnvironmentUtils.kt:57-126` |
| 前端流水线 | `compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/AbstractFrontendPipeline.kt:12-41` |
| Phase 裁剪机制 | `compiler/phaser/src/org/cangnova/cangjie/phaser/PhaseConfig.kt:10, 51`；`PipelinePhase.kt:68-75` |
| `CfirFunction.body` 可空 | `cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/declarations/CfirFunction.kt:40` |
| `CfirFile` | `cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/declarations/CfirFile.kt:31-60` |
| `CfirDeclaration.replaceAnnotations` | `cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/declarations/CfirDeclaration.kt:25, 37` |
| `BodyBuildingMode` | `cfir/raw-cfir/raw-cfir-common/src/org/cangnova/cangjie/cfir/builder/BodyBuildingMode.kt:9-14` |
| PSI→CFIR 无体 | `cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt:3555-3562` |
| 隐式 abstract（PSI，**函数+属性同在一个 `when`**） | `cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt:3887-3899`（函数分支 `:3895`、属性分支 `:3896`） |
| interface default 判定（PSI，**不改**） | `cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt:3902-3911` |
| LightTree 无体 | `cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt:3082` |
| 隐式 abstract（LightTree，**两个独立函数**） | `LightTreeRawCfirDeclarationBuilder.kt:3287-3293`（函数，**需护栏**）、`:3300-3308`（属性，**不动**）、`:3315-3316`（上下文判定）、`:3323-3324`（`hasSyntaxBody`，**不动**） |
| 构建入口 | `cfir/entrypoint/src/org/cangnova/cangjie/cfir/pipeline/analyse.kt:55-78, 106-142` |
| `.cjo` 路径常量 | `cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/CjoConstants.kt:10-26` |
| `.cjo` 搜索路径 | `cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoSearchPath.kt:16-101` |
| `.cjo` 加载 | `cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoManager.kt:15-61` |
| `.cjo` 写出（不写注解） | `cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CjoPackageWriter.kt:15-33, 136-163` |
| 注解反序列化 | `cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirDeclDeserializer.kt:131-167, 319-340` |
| 反序列化上下文 | `cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirDeserializationContext.kt:16-57` |
| 库声明 provider | `cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/provider/AbstractCfirDeserializedSymbolProvider.kt:43-172` |
| `.cjo` FlatBuffers schema | `flatbuffers-gen/flatbuffers/ModuleFormat.fbs:272-283, 659-680` |
| 注解可用性数据源 | `cfir/providers/src/org/cangnova/cangjie/cfir/declarations/CfirDeclarationAvailabilityProvider.kt:33-231` |
| APILevel 引用检查 | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirApiLevelRefHigherChecker.kt:30-129` |
| `@IfAvailable` 检查 | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/expression/CfirIfAvailableExpressionChecker.kt:25` |
| 注解声明语义检查 | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirBuiltInAnnotationSemanticsChecker.kt:52, 169-206` |
| checker 分组 | `cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/CommonDeclarationCheckers.kt:30`；`CommonExpressionCheckers.kt:30-163` |
| checker 注册 | `cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/checkers/CheckersContainers.kt` |
| `.cjo` 二进制读取 | `analysis/decompiled/decompiler-to-file-stubs/src/org/cangnova/cangjie/analysis/decompiler/stub/file/CjoBinaryFileReader.kt:16-57` |
| `.cjo` stub 构建 | `analysis/decompiled/decompiler-to-stubs/src/org/cangnova/cangjie/analysis/decompiler/stub/CallableCjoStubBuilder.kt:184, 217` |
| light declaration 惰性注解 | `analysis/light-declarations/src/org/cangnova/cangjie/analysis/light/declarations/CaLightDeclarationModels.kt:40` |
| IDE 文件类型注册 | `intellij-ide/modules/ide/base/src/main/resources/META-INF/cangjie-filetypes.xml:3-12` |
| IDE parserDefinition 注册 | `intellij-ide/modules/ide/base/src/main/resources/META-INF/cangjie-language-shell.xml:28-30` |
| IDE 文件收集 | `intellij-ide/modules/ide/base/src/main/kotlin/org/cangnova/cangjie/ide/base/analysisApiPlatform/CaIdeScopeCangJieFileCollector.kt:36-111` |
| 收集结果消费 | `intellij-ide/modules/ide/base/src/main/kotlin/org/cangnova/cangjie/ide/base/analysisApiPlatform/CaIdeDeclarationProviderFactory.kt:26-35` |
| 依赖模型 | `intellij-ide/modules/domain/project-model/src/main/kotlin/org/cangnova/cangjie/project/model/CjDependency.kt:290-339` |
| 依赖解析 | `intellij-ide/modules/domain/package-manager/src/main/kotlin/org/cangnova/cangjie/cjpm/dependency/CjpmDependencyResolver.kt:767-863, 1067-1090` |
| Workspace Model 同步 | `intellij-ide/modules/domain/project-model/src/main/kotlin/org/cangnova/cangjie/project/workspace/CjWorkspaceModelSync.kt:1193, 1375-1380` |
| 构建入口（cjpm） | `intellij-ide/modules/domain/package-manager/src/main/kotlin/org/cangnova/cangjie/cjpm/run/CjpmCommandExecutor.kt:48-134` |
| 工具链命令行封装 | `intellij-ide/modules/domain/toolchain/src/main/kotlin/org/cangnova/cangjie/toolchain/command/commandLine.kt:45-254` |
| 直接调编译器的先例 | `intellij-ide/modules/ide/macro/src/main/kotlin/org/cangnova/cangjie/macro/compiler/CompilerMacroExpansionProvider.kt:249-296` |
| 构建产物解析 | `intellij-ide/modules/domain/package-manager/src/main/kotlin/org/cangnova/cangjie/cjpm/build/CjpmBuildOutputParser.kt:129-175` |
| 过期的 `cjd` 注册草稿（**构建产物，不得作为设计依据**） | `intellij-ide/build/resources/main/META-INF/cangjie-filetypes.xml:49-51` —— v2 已证伪其"历史旁证"地位：该文件在 `build/` 下，其 `.cjo` 实现类写作 `CangJieBinaryObjectFileType`（当前源码为 `CangJieBuiltInFileType`），且草稿用 `extensions="cjd"` 这一**错误**表达（见 4.9.1） |
| 参考实现 `.cj.d` 注册 | `external/cangjie_deveco_plugins/lsp-client/src/main/resources/META-INF/cangjie-lsp.xml:27-32, 43, 48, 64` |
| 参考实现 FileType | `external/cangjie_deveco_plugins/lsp-client/src/main/java/com/huawei/ideacj/filetypes/CangjieDeclarationFile.java:26-61` |
| 参考实现 ParserDefinition | `external/cangjie_deveco_plugins/lsp-client/src/main/java/com/huawei/ideacj/language/CangjieDeclarationParserDefinition.java:67-189` |
| SDK 布局 KDoc 提及 `.cj.d` | `deveco/modules/domain/toolchain/src/main/kotlin/org/cangnova/cangjie/toolchain/api/CangjieSdkLayout.kt:55-68, 492, 535-550` |
| `deveco` 收集器副本 | `deveco/modules/ide/base/src/main/kotlin/org/cangnova/cangjie/ide/base/analysisApiPlatform/CaIdeScopeCangJieFileCollector.kt:100-112`（与 `intellij-ide` 同名文件**正文一致**，见 OP3） |
| 解析诊断消息资源（v2 新增） | `psi/resources/messages/CangJieParsingBundle.properties:77, 81, 93, 125` |
| `field.requires.type.or.initializer` 调用点（v2 新增） | `psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt:2297` |
| `CfirDeclarationAvailabilityProvider` 直读 `annotations`（v2 新增） | `cfir/providers/src/org/cangnova/cangjie/cfir/declarations/CfirDeclarationAvailabilityProvider.kt:88-91, 104-119` |
| `CfirMemberDeclaration`（sealed，合并生效范围，v2 新增） | `cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/declarations/CfirMemberDeclaration.kt:19` |
| `CfirCallableDeclaration` / `CfirClassLikeDeclaration`（v2 新增） | `cfir/cfir-tree/gen/.../declarations/CfirCallableDeclaration.kt:21`；`CfirClassLikeDeclaration.kt:22` |
| `deserializeDecl` 合并点上下文（v2 修正为 `:131-175`） | `cfir/cfir-serialization/src/.../deserialize/CfirDeclDeserializer.kt:132, 147, 152-169` |
| `appendSerializedAnnotationMarker`（marker 去重，v2 新增） | `cfir/cfir-serialization/src/.../deserialize/CfirDeclDeserializer.kt:724-762` |
| `PackageDeserializers` 持有 context（v2 新增） | `cfir/cfir-serialization/src/.../provider/AbstractCfirDeserializedSymbolProvider.kt:393-397` |
| provider 的 `contextCache`（v2 新增） | `cfir/cfir-serialization/src/.../provider/AbstractCfirDeserializedSymbolProvider.kt:56, 172-186` |
| 每次新建 context（v2 新增） | `cfir/cfir-serialization/src/.../provider/CfirDeserializedSymbolProvider.kt:35-40` |
| CLI 参数生成器合成参数（v2 新增） | `compiler/frontend-arguments-generator/src/org/cangnova/cangjie/frontend/arguments/generator/Main.kt:116` |
| 生成物属性清单（v2 新增） | `compiler/frontend/gen/org/cangnova/cangjie/frontend/arguments/CommonCompilerArguments.kt:7-44` |
| `CompilerConfiguration.useFir`（非 CLI 参数，v2 新增） | `compiler/config/src/org/cangnova/cangjie/config/CommonConfigurationKeys.kt:178-180` |
| `CangJieBuiltInFileType`（独立 FileType，不继承 `CangJieFileType`） | `psi/src/org/cangnova/cangjie/lang/declarations/CangJieDeclarationsFileType.kt:37-77` |
| `CjCommonFile.getFileType()` 硬编码（OP1 改动点） | `psi/src/org/cangnova/cangjie/psi/CjFile.kt:66` |
| `CangJieFileType.DOT_DEFAULT_EXTENSION` | `psi/src/org/cangnova/cangjie/lang/CangJieFileType.kt:37-40` |
| source roots 构造（OP2，目录粒度，v2 新增） | `intellij-ide/modules/domain/project-model/src/main/kotlin/.../workspace/CjWorkspaceModelSync.kt:560-573` |
| 过期 `cjd` 注册草稿（**构建产物，不得作为设计依据**） | `intellij-ide/build/resources/main/META-INF/cangjie-filetypes.xml:49-51`（v2 已证伪其"历史旁证"地位，见 4.9.1） |

---

## 附录 C：版本变更记录

本设计经历过三稿。**本附录只记录“结论层面”的变化**，便于评审时对照旧稿；
已被推翻的旧结论不要再用。

### C.1 v1 → v2：事实性错误更正与未闭环项闭环

| # | v1 的表述 | v2 实测结论 |
|---|---|---|
| 1 | “CLI 参数 DSL 与生成产物**有漂移**（gen 里有 `useFir`）” | **无漂移**。`useFir` 是 `CompilerConfiguration` 扩展属性（`CommonConfigurationKeys.kt:178-180`），与 CLI 参数无关；生成物 6 = DSL 4 + 生成器合成 2 |
| 2 | “不改 `createFile` 会让 `.cj.d` **fallthrough 到 `PsiPlainTextFileImpl`**” | **不会**。`CangJieDeclarationFileType` 继承 `CangJieFileType`，会被 `is CangJieFileType` 接住。真实故障是**静默降级为普通 `CjFile`**（更隐蔽） |
| 3 | `getFileType()` 建议“**不改**，列技术债” | **决策翻转：本次改**。爆炸半径已枚举（覆写仅 1 处、6 处 `INSTANCE` 引用无一读取它） |
| 4 | P1 抑制点含 3 个不存在的行号，且遗漏大半 | 补全为完整清单（v3 进一步改为统一入口，见 C.2） |
| 5 | P2/P3 留“psi 中对应的校验点”占位符 | **psi 层不存在该诊断（N/A）**，落点转移到语义层 checker |
| 6 | “过期构建产物留有 `cjd` 草稿，应当清掉那段注释” | 该草稿在 `build/`（产物），源码 XML 本无此注释 ⇒ **无执行对象** |
| 7 | 4.6.5 合并点理由写“provider 读快照” | **不准确**：provider 直读 `declaration.annotations`。真实约束是三条硬条件（target 归一化 / 快照完整 / marker 去重） |
| 8 | 4.6.6 “**必须验证**跨 provider 共享” | **已关闭**：四段证据链证明 `declCache` 每 provider 独立，无需显式清理 |
| 9 | “`deveco` 与 `intellij-ide` 收集器重复代码建议本次对齐” | 已确认两份**内容一致**；根因是两个独立 Gradle 构建 ⇒ 本次不收敛，改“同步注释 + 一致性校验” |
| 10 | 4.5.2 护栏 1 只说"给 `isImplicitAbstractClassLikeMember` 加护栏" | **必须只作用于函数分支**：该函数内含函数与属性两条分支，属性分支加护栏会**直接破坏 R4**；LightTree 侧同理（两个独立函数）。这是 v2 最重要的一处更正 |
| 11 | 4.5.2 护栏 2 "这一条需要复核" | v2 曾复核为**不改**，理由是"`.cj` / `.cj.d` 两条路径的判定结果本来就一致"。**该结论在 v3 定稿被推翻**：它比较的是"本仓 `.cj` vs 本仓 `.cj.d`"，而不是"本仓 vs 官方"，因而漏掉了官方在声明模式下新增的 `Attribute::DEFAULT`（1.3.5）。正解是**改谓词来源**（`DEFAULT := !isAbstractEffective`），而非加护栏 |
| 12 | 4.4.5 "需要逐项核对各槽位" | 给出逐 checker 清单；**v3 进一步把实现方式从"12 处 early-return"升级为框架级标记**（见 C.2） |
| 13 | 4.3.2 "建议在 DSL 里补 `compilerName`（**以支持为准**）" | **已确认支持**：字段在 `CangJieCompilerArgument.kt:55/137/192`，唯一消费点是 `Generator.kt:105-106, 130` 的 `calculateName()`，语义正是"生成类中的属性名"。可确定写 `compilerName = "compileCjd"` ⇒ 生成 `var compileCjd`；并补充了 `?:` 优先级导致的"`compilerName` 不被 kebab 二次加工"这一易踩细节 |
| 14 | 4.2.1 "建议一并把 `CangJieBuiltInFileType` 迁到同名文件（**可选，不阻塞**）" | **决策：不改文件名**。Kotlin 不要求文件名 = 顶层声明名；重命名属无关重构（`AGENTS.md` §11 应剥离）。同时实测 `CangJieLanguage` 未覆写 `displayName`（`CangJieLanguage.kt:33`）⇒ `getName()` 返回 `"CangJie"`，故新 FileType **必须**覆写 `getName()` 以防 `FileTypeRegistry` 重名 |
| 15 | 附录 B 末尾混入一行被截断的 P2 表格行（文档损坏） | 已清除并替换为正确条目 |
| 16 | 开放问题 **OP2–OP6** 的逐条处置（v1 全部悬置） | OP2 不进 source roots + 补索引侧排除；OP3 不收敛（改"同步注释 + 一致性校验"）；OP4 不补；OP5 不进入本次但给出无前置依赖路径；OP6 不修复并升为**显式已知限制**。完整处置表见 7.3 |

### C.2 v2 → v3：从“纯增量”改为“框架正确性优先”

**v2 的落点方案本质是在既有框架上逐点加条件**（7 个抑制点、12 个 checker early-return、新增第三个解析入口、
内联复制路径推导）。v3 按 3.1.1 的 5 条判据重新评估，改为：

| 维度 | v2（纯增量） | v3（框架正确） | 依据 |
|---|---|---|---|
| 缺体诊断抑制 | 7 个点各加 `if (!isDeclarationFile)` | **1 个统一入口** `reportMissingBody()`（4.2.6） | J1 |
| 被误用的诊断 | 未处理（被迫人工分组） | **修正 5 处文案**（`func }` 应报“缺标识符”） | J1 |
| 解析模式载体 | `ParsingContext.isDeclarationFile` 字段 | `CangJieParsing.sourceKind` **构造属性**（4.2.3） | J3 |
| 文件级入口 | 新增第 3 个入口 `parseDeclarationFile()` | **仍是 2 个**，模式由构造属性派生（4.2.5） | J3 |
| `CjFile` 侧表达 | `isDeclarationFile: Boolean` 构造参数 | 由 `viewProvider.fileType` 推导的 `sourceKind`（4.2.2） | J2 |
| Sema 豁免 | 12 个 checker 各自 early-return | **1 处**：`CfirDeclarationChecker.requiresImplementation` 标记 + 生成器过滤（4.4.5） | J1 J4 |
| `CjSourceKind` 取值 | 2 个（`SOURCE` / `DECLARATION`） | **3 个**（+ `MACRO_CALL`）⇒ `when` 无缺口（4.1.1） | J5 |
| `project-model` 路径推导 | 内联 | 内联 + 复用常量；**不抽公共类**（4.9.4） | 见该节判据 |

**v3 修正的 v2 自身错误**（3 条）：

| # | v2 的错误 | v3 修正 |
|---|---|---|
| 1 | 把 12 个诊断点统一列为“P1 全部豁免” | 必须分 A/B 两类：只有 7 个是“合法无体”，另 5 个是**语法错误恢复路径**，豁免会静默吞掉真语法错误 |
| 2 | 建议 `parseFile(parseContext = DEFAULT)` 参数化 | 改为“模式随构造注入”，两个入口都不改签名（4.2.5） |
| 3 | 建议“优先复用 LightTree”，未发现 `CangJieLightParser` 无 `ParsingContext` 入口 | 该阻塞在 v3 下消解（只需多传一个 `sourceKind` 参数，4.2.5 末） |

### C.3 v1/v2 共同遗漏、v3 补入的约束

| # | 缺口 | 补入位置 |
|---|---|---|
| 1 | **跨仓发布边界**：`intellij-ide` 通过固定版本（`cangjie = “1.1.1”）的发布工件消费主仓 11 个 `-for-ide` 工件；P5 的真实前置是「P0+P1+P4 + 主仓发布」，且联调须 `--refresh-dependencies` | 4.10 |
| 2 | **测试落点与基类约定**：P1 → `psi/test/org/cangnova/cangjie/psi/`（包名是 `psi` 非 `parsing`）；P2 → 两条 CFIR 路径的 testFixtures **成对共享**，且护栏测试须走 **AST/Stub 双变体**并由 `TestGeneratorForPsi2Cfir.kt` 生成 | 6.0 |
| 3 | `project-model` 内联推导是**依赖边界所迫**（引不到 `cfir-serialization`），但须复用 `CjSourceKind.DECLARATION_SUFFIX` 常量 | 4.9.4 |
| 4 | `analysis/decompiled` **已依赖** `cfir-serialization` ⇒ 4.8.1 的复用方案无需新增模块依赖 | 4.8.1 |
| 5 | `CfirMemberBodyDeclarationChecker`（本仓特有）必须豁免，v1 完全遗漏 | 4.2.6 / 4.4.5 |
| 6 | 源码根下的 `.cj.d` **仍会被 Stub 索引**（v1 只说了"不建 source root"，漏了索引侧） | 4.9.3 |
| 7 | `isImplicitAbstractClassLikeProperty`（LightTree 侧）在 v1 中完全缺失 ⇒ 极易被误加护栏 | 4.5.2 明确"保持不动"；5.1-P2 与 6.2 增加互斥断言 |
| 8 | `CfirMemberDeclaration` 是 **sealed class**，v1 未记录 ⇒ 实现者无法判断"顶层声明是否也会走 `publishAnnotationInfo`" | 4.6.5 补充继承链，给出"顶层 func/class/prop/var 均属其后代"的结论 |
| 9 | `CangJieParsingBundle` 中唯一与初始化相关的键 `field.requires.type.or.initializer` 在声明模式下是否豁免？ | 4.2.6 明确**不豁免**并给出理由（官方 P2/P3 未覆盖该语义） |

### C.4 v3 尚未闭环的项（需真实环境，非文档缺陷）

| # | 未决项 | 何时确认 |
|---|---|---|
| R1 | 新增 `-d` 后生成产物的实际 diff（应为“只多出 `compileCjd`”） | P0 施工时 |
| R2 | 6.5 端到端测试所需的 `.cjo` + `.cj.d` fixture（需外部 `cjc` 产出） | P4 施工时 |
| R3 | `--refresh-dependencies` 是否真能破解同版本缓存（建议先做一次空发布往返验证） | P5 开工前 |
| R4 | `parseMacro` 的 `functionBody?` 注释与实现矛盾 | 独立课题 |
| R5 | `deveco` 侧跟随（同样在跨仓边界外） | 后续迭代 |

### C.5 v3 → v3.1：复核报告的处置

**来源**：`cjd-declaration-file-support-v3-review.md`（2026-09-16，对 v3 初稿逐条回查官方与本仓源码）。
**复核结论**：R1–R14、13 处官方取证、`DIFF-1`（官方越界 UB）**逐条成立**；但发现 4 处必修、4 处需补正、4 处需澄清。

#### C.5.1 必修项

| # | 复核发现 | 本版处置 | 位置 |
|---|---|---|---|
| **A-1** | 官方 `SetDefaultFunc`（`ParseDecl.cpp:874-898`）无 `parseDeclFile` 门禁 ⇒ `.cj.d` 的 interface 无体函数会获得 `Attribute::DEFAULT`；初稿的"四状态表"与"护栏 2：确认不改"均未覆盖 | **采纳并升级解法**：新增 [1.3.5](#135-p6-的连带后果interface-无体函数会获得-default) 完整论证；**正解不是复核建议的"复刻"或"登记 DIFF-6"，而是让 `DEFAULT` 与 `ABSTRACT` 同源派生**（`DEFAULT := !isAbstractEffective`）⇒ 无需任何 `.cj.d` 分支，护栏 1 一生效即自动正确。改写 1.3.2 状态表、1.3.5→1.3.6、4.5.2 护栏 2、6.2 新增 D1–D4 用例组、7.4-A、1.5 新增 **R15** | 1.3.2 / 1.3.5 / 4.5.2 / 6.2 / 1.5 |
| **A-2** | `ParsingContext.DECLARATION_FILE` 在 4.2.3 说"不新增"、4.2.5 又直接使用，自相矛盾；且它在 v3 框架下与 `DEFAULT` 恒等，是语义空壳 | **删除该预设，并连同 `fileParsingContext()` 一起删**（只删预设会让"两入口同体"的冗余固化）。补上**根因说明：`ParsingContext` 承载"用哪套文法"，文件种类承载"解析出来的东西意味着什么"，二者正交**。新增"维度分工表" | 4.2.5 |
| **A-3** | 4.4.2 把 `convertToSourceFiles` 的条件改成 `sourceKind == SOURCE` 是**空操作**（`.cj.d` 仍走插件扩展路径）；且伪代码抹平了 else 分支的 `ensurePluginsConfigured()` 与错误上报副作用 | 改为 `ext == "cj" \|\| sourceKind().isDeclaration`；**else 分支逐字保留**；顺带把 `sourceKind()` 抽成一份共用实现（filter 与 convertToSourceFiles 不得各写一份） | 4.4.2 |
| **A-4** | F5/4.4.5 的 `interface CfirChecker` **本仓不存在**；"分派处 1 处"落在**生成代码**里且全文未提生成器；`DeclarationCheckers` 路径也写错 | ① 标记改加在真实基类 **`CfirDeclarationChecker<D>`**；② 明确过滤点在**生成器模板** `Generator.kt:391-413`，**不得手改 `gen/**`**；③ **判定依据由会话级 `compileCjd` 改为文件种类**（见下）；④ **A 表 12 → 11 项**（见下） | 4.4.5 / 8.3-F5 |

#### C.5.2 需补正项

| # | 复核发现 | 本版处置 |
|---|---|---|
| **B-1** | `ParsingContext` 统计错误：实际 **15 字段 / 11 预设 / 9 个死预设**（初稿写 20/14/11，且自身算术不自洽 `14-2≠11`） | 全部改为实测值，并**把复核命令与期望输出写进正文**（可复现） |
| **B-2** | 缺体诊断上报点"穷举"漏 `CangJieParsing.kt:3004`（实际 **17 处** = A 7 + B 5 + 排除 5，初稿覆盖 16） | 排除清单 4 → **5** 条；加**计数自检**（17 处对不上即说明清单过期）；F1 表述改用 17 |
| **B-3** | OP1 说"`getFileType()` 覆写只有这一处"不实（实际 **2 处**）；`CangJieFileType.INSTANCE` 计数口径不一（说 6 却列 7） | 覆写改述为 2 处（`CjFile.kt:66` 与 `CangJieMacroCallFileType.kt:34`）；**并补上漏掉的正向论据**——后者正是"子类返回自身真实 FileType"的现成先例，支持 OP1 决策；统一为"共 7 处，1 处改、6 处不动" |
| **B-4** | 4.9.2 的 `createFile` 改法用了 v2 的构造参数（v3 下**编译不过**），且 v3 下**根本不用改** | 4.9.2 重写为"**本文件不动**"，并给出完整链条证明；删除"声明分支必须前置"这一失效约束（连带 5.1-P5、6.6、7.4-E）；在 4.2.1 显式登记"必须继承 `CangJieFileType`"这一承载性约束 |

#### C.5.3 澄清项（含比复核建议更进一步的修正）

| # | 复核建议 | 本版处置 |
|---|---|---|
| **C-1** | `CangJieParser.parse` 的"宏调用"判定与 `CjSourceKind.MACRO_CALL` 构成两个真源，应按 `sourceKind` 收敛 | **采纳**：入口改为对 `sourceKind` 的**穷尽 `when`（无 else）**，删除 `name.endsWith(".cj.macrocall")`。**补充复核未指出的一点**：`parseAnnotationOnly` 的真实身份是**宏展开片段的内部重解析入口**（`LightTreeRawCfirDeclarationBuilder.kt:1477`），与文件种类无关 —— 因此"入口按 sourceKind 收敛"只适用于**文件级**入口，片段级入口必须保持显式（否则该路径会静默丢失 `ANNOTATION_ONLY` 上下文） |
| **C-2** | 补一条验收：被排除出索引的 `.cj.d`，打开/编辑/补全/跳转是否仍正常 | **采纳**：6.6 新增 6 条能力验收表，并注明"若第 4/5 项失败，需重新权衡 4.9.3 的排除策略，这是一个真实决策点" |
| **C-3** | 两条 `.cjo → 声明` 路径的注解一致性未覆盖（D5 写回式方案下"物化次数 = 追加次数"） | **采纳并升格为 P4 准入条件**：要求先答"两条路径是否产出同一对象"，并给出两种结论各自的保障；若为两份对象，4.6.5 的三条约束需**升级为四条**（增加幂等性） |
| **C-4** | `DIFF-5`（sidecar 不做宏展开）的缓解论证未对真实 SDK 验证 | **采纳并升格为 P4 准入条件**：给出三项统计口径与准入判定（半小时量级，但决定 P4 的方案形状） |

#### C.5.4 本版在复核之外新发现并修正的问题

| # | 问题 | 处置 |
|---|---|---|
| 1 | **A-1 的第三解**：复核给出"复刻 `DEFAULT`"或"登记 `DIFF-6`"二选一。实际上两者都不必 —— 根因是本仓把 `ABSTRACT` 与 `DEFAULT` 写成**两个独立谓词**（官方是同一函数体内顺序推导）。改为派生后，`.cj.d` 自动正确且**无专属分支** | 1.3.5 / 4.5.2 |
| 2 | **官方豁免粒度是"函数"不是"诊断"**：`CheckAttributesForPropAndFuncDeclInClass` 开头的 `if (opts.compileCjd) return;` 一次关掉 4 条诊断 ⇒ 初稿把 `CfirOpenMemberChecker` / `CfirFinalizerDeclarationChecker` / `CfirMemberBodyDeclarationChecker` 拆成 3 行、各引不同行号的做法，掩盖了 **1 个官方函数 ↔ 3 个本仓 checker** 的对应关系 | 4.4.5 |
| 3 | **`CfirMemberBodyDeclarationChecker` 不该打标记**：它有 `if (!member.status.isAbstract) return` 守卫，护栏 1 生效后自动沉默；打标记反而会**掩盖护栏 1 的缺陷**。应**保留启用**作为免费回归探测器，并在 6.2/7.4 增加"`.cj.d` 下不得出现 `MISSING_FUNC_BODY`"断言 | 4.4.5 / 6.2 / 7.4-C |
| 4 | **过滤依据不能是 `compileCjd`**：它是会话级布尔，在"`.cj` 与 `.cj.d` 同时可见"的 IDE 会话里**无法表达**（正是 C-2 的场景）。改为读文件种类，并给出已验证的回指路径 `CheckerContext.containingFileSymbol`（`:79`）→ `CfirFileSymbol.sourceFile`（`CfirBasedSymbol.kt:748-753`）→ `CjSourceKindCarrier` | 4.4.5 |
| 5 | **`CfirSession.Kind` 是陷阱**：它是 `{Source, Library}`，与声明模式正交（`cjc -d` 编译的 `.cj.d` 是 `Kind.Source`）。用它当判据会改变既有 `.cjo` 加载行为 | 4.4.5 |
| 6 | **`MACRO_CALL` 必须被源收集显式拒绝**，而非"恰好落进 else 被拒"（巧合不是规则） | 4.4.1 / 4.4.2 |
| 7 | 4.5.2 的「护栏 3」段落**重复出现两次**（文档自身缺陷） | 已去重 |
| 8 | 核实后**否定**的一个怀疑：4.4.2 的 `filter` 是否需要额外修正 —— 初稿的 `filter` 已按 `kind.isDeclaration` 分流，无需再动。记录于此，避免后人重复怀疑 | — |
