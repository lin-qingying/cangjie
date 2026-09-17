# `.cj.d` 声明文件支持设计

> 状态：**设计稿 v2 —— 问题闭环版（可进入施工）**
> 适用范围：主仓库（`common` / `psi` / `compiler` / `cfir` / `analysis` / `lsp`）+ `intellij-ide`
> 证据基线：`external/cangjie_compiler`（官方实现镜像，只读） + 本仓当前工作树
> 修订说明：v1 中存在 5 处事实性错误、9 处"待复核/需验证/占位"未闭环、5 个未决开放问题、1 行损坏残留；
> v1 与 v2 初稿还**共同遗漏**了跨仓发布边界等 4 项关键约束。
> v2 已**逐条实测取证并给出结论**，不再保留任何"稍后确认"式表述。
> 修订清单见 [第 8 章](#8-v2-修订记录问题闭环)（相对 v1）；
> **v2 初稿自身的缺陷与二次修正见 [第 9 章](#9-v2-二次修正自查发现的自身缺陷)**；
> 剩余未决项（需真实运行环境，非文档缺陷）见 9.3。
>
> ⚠️ **重要**：第 3–4 章的落点方案是"纯增量"（逐点加条件）。**第 10 章从框架正确性出发重新评估了这些落点**，
> 指出 7 处必须做破坏性改动，并给出替换方案。**施工前请先读第 10 章**——它会改写 4.2.3 / 4.2.5 / 4.2.6 / 4.4.5 / 4.9.4 的部分结论。
> 判据见 10.1，优先级与风险见 10.4，不建议动的项见 10.5。
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

`(isMember && !parseDeclFile)` 这个合取意味着：**`.cj.d` 中无函数体的类成员函数，既没有 `ABSTRACT` 属性，也不报错。**

`FuncDecl` 因此存在**第四种状态**：

| 状态 | 有无 body | 有无 `ABSTRACT` | 语言是否接受 | 出现场景 |
|---|---|---|---|---|
| 实现 | 有 | 否 | 是 | `.cj` |
| 纯虚 | 无 | **是** | 是 | `.cj` 的 class / interface 成员 |
| 非法 | 无 | 否 | **否**（报错） | `.cj` 的其它 scope |
| **声明** | 无 | **否** | 是 | **`.cj.d`** |

⇒ 把 `.cj.d` 的无体函数一律标成 `abstract` 是**语义错误**：`abstract` 会引出一整套继承/override/实例化约束（不可实例化、必须被重写、可见性限制），而 `.cj.d` 的声明对应的是**有真实实现的外部声明**。

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

#### 1.3.5 解析后的 AST 形态

声明模式下 **AST 结构与非声明模式完全一致**，唯一差别是属性位：

```text
FuncDecl
  funcBody: FuncBody        ← 非空
    body: Block? = nullptr  ← 空
  attributes: 不含 ABSTRACT / HAS_BROKEN
```

`ParseFuncBody`（`ParseDecl.cpp:1668-1713`）在无 `{` 时仍构造 `FuncBody`，只是不设置 `body`，并把 `end` 定位到 `retType->end` 或 `)` 之后。

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

`PRIMITIVE_TYPE` 有 `Rune → UInt8` 的归一化（`MergeAnnoFromCjd.cpp:147-148`）。

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

1. **语义优先于对称**。函数与属性在声明模式下的处理不对称（R3 vs R4），不为了代码整齐而抹平。
2. **一种语义，一个开关**。声明模式是**语言语义模式**，不是性能策略，绝不复用 `BodyBuildingMode.LAZY_BODIES`（后者是"延迟构建 body"，语义上 body 仍然存在）。
3. **两种形态分开实现**。形态 A 是"输入模式"，形态 B 是"注解供给"，共享解析器与匹配谓词，但不共享生命周期。`.cj.d` 绝不作为普通 `.cj` 进入源码集。
4. **不在消费侧打补丁**。`.cj.d` 的支持落在"解析 → 建 CFIR → 加载 `.cjo`"这几条供给链上，`CfirApiLevelRefHigherChecker` 等消费方不做任何改动。
5. **单一真源**。文件种类判定只允许存在一份实现（后缀判定 + PSI 标记），禁止在各模块重复写 `endsWith(".cj.d")`。
6. **可裁剪、可降级**。任一形态可独立交付；形态 B 不可用时，形态 A 的解析与 IDE 编辑能力仍然成立。

### 3.2 分层模型

```text
┌─ 语义开关（唯一真源）─────────────────────────────────────────────┐
│  CjSourceKind = SOURCE | DECLARATION                              │
│  CompilerConfiguration.compileCjd : Boolean                       │
│  ParsingContext.isDeclarationFile : Boolean                       │
│  CjFile.isDeclarationFile : Boolean                               │
└───────────────┬──────────────────────────────┬────────────────────┘
                │                              │
   形态 A：声明编译                     形态 B：sidecar 合并
   （显式输入 .cj.d）                    （.cjo 旁的 .cj.d）
                │                              │
   ┌────────────▼──────────┐        ┌──────────▼────────────────┐
   │ psi 解析模式           │        │ CjdSidecarAnnotationIndex │
   │  · 6 个抑制点          │        │  · 路径推导 .cjo → .cj.d  │
   │ compiler/frontend      │        │  · 轻量解析 + 声明签名索引 │
   │  · 源收集互斥          │        │  · 匹配谓词（同 R12/R13）  │
   │  · Phase 裁剪          │        │ CfirDeclDeserializer      │
   │ cfir/raw-cfir          │        │  · merge 进 annotations    │
   │  · declaration 护栏    │        │  · provider 级失效         │
   │ intellij-ide           │        │ CfirDeclarationAvailability│
   │  · toolchain 调 cjc -d │        │  · 消费（不改）            │
   └───────────────────────┘        └───────────────────────────┘
```

### 3.3 关键决策记录

#### D1：`.cj.d` 使用**独立 FileType** 但**复用 `CangJieLanguage`**

- **决策**：新增 `CangJieDeclarationFileType : CangJieFileType`（继承，因为 `CangJieFileType` 已是 `open class`），注册 `patterns="*.cj.d"`，`language="CangJie"`；**不新增 `lang.parserDefinition`**。
- **理由**：
  - 复用 `CangJieLanguage` 后，语法高亮、折叠、注释器、括号匹配、`lang.syntaxHighlighterFactory` 等 language 级扩展**全部自动继承**，无需重复注册。官方参考实现因为使用**独立语言** `CangjieDeclaration`，被迫把 `syntaxHighlighterFactory` / `semanticHighlighter` / `externalAnnotator` 逐一重注册（`cangjie-lsp.xml:43,48,64`），代价明显。
  - 本仓的 `CangJieParserDefinition.createFile` 是 `when (viewProvider.fileType)`，天然支持多 FileType 分派。
  - 本仓的解析入口 `CangJieParser.Companion.parse(builder, psiFile)` 已经是**文件感知**的，不需要靠 Language 来区分解析模式。
- **必须用 `patterns` 而非 `extensions`**：`extensions` 按最后一个 `.` 取后缀，`foo.cj.d` 会被解释成扩展名 `d`。参考实现 `CangjieDeclarationFile.java:54` 的 `getDefaultExtension()` 也返回 `"cj.d"`。
- **反例（被否决）**：新建 `CangJieDeclarationLanguage`。否决理由是它会让所有 language 级扩展需要二次注册，而收益（解析模式隔离）已被 `psiFile` 感知入口覆盖。

#### D2：解析模式通过 `ParsingContext` 显式传递，**不**通过全局状态

- **决策**：`ParsingContext` 增加 `isDeclarationFile: Boolean = false`，并新增伴生预设 `DECLARATION_FILE = ParsingContext(isDeclarationFile = true)`；`parseFile()` 改为接收 context 参数；`CangJieParser.Companion.parse(builder, psiFile)` 按 `psiFile` 判定后传入。
- **理由**：`ParsingContext` 已经是本仓传递解析模式的既有机制（`ANNOTATION_ONLY` / `LEGACY` 先例，`AbstractCangJieParsing.kt:158-168`），且通过 `context(parseContext: ParsingContext)` 上下文接收器显式下传，无需线程局部变量。
- **反例（被否决）**：用 `ThreadLocal` 或在 `CangJieLexer` 里埋开关。否决理由是不可重入、对宏展开重解析不安全（宏展开会在同一线程上对另一份文本重新建树）。

#### D3：`.cj.d` 中无体函数**不获得 `abstract`**，但无体类成员属性**仍然获得 `abstract`**

- **决策**：严格对齐 R3 + R4。
- **理由**：见 1.3.2 / 1.3.3。这是官方语义的不对称，抹平任何一边都是错的。
- **实现**：`parseFunction` 的豁免条件增加 `isDeclarationFile`；`parseProperty` 的分支**保持不变**（因为它在 class/interface 作用域下打 `abstract` 是 `.cj` 的既有正确语义），只把诊断抑制接到 `DiagMissingPropertyBody` 对应的上报点。

#### D4：CFIR 层必须显式区分"声明模式"，禁止用"无 body ⇒ 隐式抽象"推断

- **决策**：`isImplicitAbstractClassLikeMember`（PSI 路径）与 `isImplicitAbstractClassLikeFunction`（LightTree 路径）增加 `!isDeclarationFile` 护栏。
- **理由**：这是本仓特有的风险点。本仓在 CFIR 层**重新推断**了官方的语法层属性（官方在 `CheckFuncBody` 里由解析器直接 `EnableAttr(ABSTRACT)`），而在声明模式下官方**不打**这个属性。若不加固护栏，`.cj.d` 的每个无体成员都会被下游当成隐式抽象，触发 override 缺失、`open` 可见性、实例化约束等一连串级联误报。
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

#### D8：`CjSourceKind` 用**带默认实现的接口属性**引入，避免破坏性变更

- **决策**：`CjSourceFile` 增加 `val sourceKind: CjSourceKind`，**带默认实现**（基于文件名后缀判定）；`CjPsiSourceFile` 覆写为"先读 PSI 标记，再回退到文件名"。
- **理由**：`psi` 依赖 `common`（`CjFile` 引用 `org.cangnova.cangjie.*`），反向依赖不成立。因此 `common` 不能直接判断"这个 PsiFile 是不是声明文件"。
  - `common` 定义**标记契约**（一个只读属性接口），`psi` 侧的 `CjFile` 实现它，`CjPsiSourceFile` 通过类型判定读取。
  - 接口属性带默认实现 → 已有实现类无需全部改写，且 `logical`（JVM default method）在本仓已全局开启（`-Xjvm-default=all`）。
- **反例（被否决）**：只在 `common` 里做后缀判定。否决理由是内存文件、测试夹具、重命名文件会判定失败（当前 PSI 上下文已经知道答案，不应再猜）。

#### D9：形态 A 的"独立编译"在本仓**不走 `compiler/frontend` 流水线**

- **决策**：
  - **产出 `.cjo`** 由外部 `cjc -d` 完成（IDE toolchain 拼命令行），不在本仓自研 `.cjo` 写出。
  - `compiler/frontend` 只承担"按声明模式收集源 + 构建 declaration CFIR"的能力，供 **IDE / LSP 直接分析 `.cj.d`** 使用。
- **理由**：
  - 本仓 `.cjo` **本来全部来自外部 `cjc`**：`CjoPackageWriter` 除定义与单测外无调用点，且不写 `annotations`（`CjoPackageWriter.kt:15-33`）。
  - `AbstractFrontendPipeline` 无子类、`CfirFrontendPipelinePhase` 无调用方（见 2.1 表），自研 CLI 流水线是另一个独立课题，不应与 `.cj.d` 支持捆绑。
  - `compiler/frontend` 的源收集与 CFIR 构建能力**已经被测试与 IDE 路径使用**（`analyse.kt:CfirSession.buildCfirFromCjFiles` / `buildCfirViaLightTree`），是可靠的落点。

#### D10：诊断呈现沿用官方文案语义

- **决策**：`.cj.d` 中"无函数体"在**解析期不再产生诊断**（对齐 P1/P4/P6），因此 IDE 的语法高亮层不会冒出红色波浪线；同时也不新增任何"声明模式"专有诊断。
- **理由**：官方没有为声明模式引入任何新诊断，只是抑制旧诊断。引入新诊断会破坏与官方的一致性。
- **补充**：`HAS_BROKEN` 不打（对齐 P5），因此不会有"破损节点"降级路径被触发。

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
    ;

    val isDeclaration: Boolean get() = this == DECLARATION

    companion object {
        /** 官方 `CJ_D_FILE_EXTENSION`。 */
        const val DECLARATION_SUFFIX: String = ".cj.d"

        /** 官方 `CJ_EXTENSION`。 */
        const val SOURCE_SUFFIX: String = ".cj"

        /**
         * 按文件名判定种类。
         *
         * 对齐官方 [HasCJDExtension]：字面后缀匹配，且 `.cj.d` 之前必须存在非空文件名。
         * 因此 `.cj.d` 与 `a.cj.d` 判定不同——前者返回 [SOURCE]。
         */
        fun fromFileName(name: String): CjSourceKind =
            if (name.length > DECLARATION_SUFFIX.length && name.endsWith(DECLARATION_SUFFIX)) DECLARATION else SOURCE
    }
}
```

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

#### 4.1.3 新增标记契约

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
    /** 是否来自 `.cj.d` 声明文件。 */
    val isDeclarationFile: Boolean = false,
) : CjCommonFile(viewProvider, isCompiled), CjSourceKindCarrier {

    override val sourceKind: CjSourceKind
        get() = if (isDeclarationFile) CjSourceKind.DECLARATION else CjSourceKind.SOURCE
}
```

同时把 `getFileType()` 从硬编码改为尊重 view provider：

```kotlin
// 现状（CjFile.kt:66）
override fun getFileType(): FileType = CangJieFileType.INSTANCE

// 改为
override fun getFileType(): FileType = viewProvider.fileType
```

> **风险提示**：`getFileType()` 返回 `CangJieFileType.INSTANCE` 是当前的行为契约，改动的爆炸半径需要评估。若不改，`.cj.d` 的 `CjFile` 会自称是 `.cj` 类型，导致 `FileTypeRegistry` 相关判断（如保存时的文件类型推断、图标、`isCangJieFileType()`）出现偏差。
> **v2 决策（与 7.3-OP1 一致，v1 此处结论已翻转）**：**本次就改**为 `viewProvider.fileType`。
> v1 曾建议"不改，列为独立技术债"。该建议在 v2 被推翻，理由是爆炸半径**已经枚举清楚**而非"未知"：
> `psi` 内 `getFileType()` 覆写仅此一处；`CangJieFileType.INSTANCE` 的 6 处引用**无一**读取 `PsiFile.getFileType()`；
> 且 `CjFile` 只可能由 `createFile` 的两个分支创建（4.9.2），故 `viewProvider.fileType` 的取值域**只有
> `CangJieFileType` 与 `CangJieDeclarationFileType` 两个值**，改动后 `.cj` 行为不变、`.cj.d` 得到期望值。
> 完整论证与验收断言见 7.3-OP1 与 6.6。**不改反而会留下 `psiFile.getFileType() ≠ viewProvider.fileType` 的自相矛盾状态。**

#### 4.2.3 扩展 `ParsingContext`

文件：`psi/src/org/cangnova/cangjie/parsing/AbstractCangJieParsing.kt`（修改）

在 `ParsingContext` data class 中新增：

```kotlin
/**
 * 声明文件模式（`.cj.d`）。
 *
 * 对齐官方 `Parser::parseDeclFile`：抑制"缺函数体/缺初始化/缺属性体"诊断，
 * 且**不**为无函数体的类成员打 `abstract`。
 */
val isDeclarationFile: Boolean = false,
```

并新增伴生预设：

```kotlin
/** 声明文件模式（`.cj.d`）。 */
val DECLARATION_FILE = ParsingContext(isDeclarationFile = true)
```

#### 4.2.4 接入解析入口

文件：`psi/src/org/cangnova/cangjie/parsing/CangJieParser.kt`（修改）

```kotlin
@NotNull
@JvmStatic
fun parse(psiBuilder: PsiBuilder, psiFile: PsiFile): ASTNode {
    psiBuilder.setDebugMode(true)

    val languageModuleName = if (psiFile is CjFile) {
        psiFile.parsedLanguageModuleName = null
        psiFile.parserLanguageModuleName
    } else ""

    val cjParsing = createForTopLevel(
        SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder),
        languageModuleName,
    )

    when {
        psiFile is CjMacroCallFile || psiFile.name.endsWith(".cj.macrocall") ->
            cjParsing.parseOnlyAnnotationFile()

        psiFile is CjFile && psiFile.isDeclarationFile ->
            cjParsing.parseDeclarationFile()          // v2：新增入口，见 4.2.5

        else ->
            cjParsing.parseFile()
    }

    if (psiFile is CjFile) psiFile.parsedLanguageModuleName = cjParsing.languageModuleName
    return psiBuilder.treeBuilt
}
```

> **关键点**：这里能直接拿到 `psiFile`，是因为 `CjFileElementType.doParseContents` 传的是 `psi.containingFile`（`CjFileElementType.kt:97`）。因此**不需要**为 `.cj.d` 新建 Language 或 ParserDefinition（D1）。
>
> **v2 补充（现状与最小改法）**：实测 `CangJieParser.parse`（`:147-172`）当前是 **if-else** 结构，不是上面示意的 `when`：
> ```kotlin
> if (psiFile is CjMacroCallFile || psiFile.name.endsWith(".cj.macrocall")) {
>     cjParsing.parseOnlyAnnotationFile()
> } else {
>     cjParsing.parseFile()
> }
> ```
> ⇒ 最小改动是把它扩成三分支（宏调用 / 声明文件 / 普通源文件），**`when` 只是等价的整理写法**，
> 不必为了它额外做一次结构调整（避免与 `.cj.d` 特性无关的重构）。三分支的判定顺序不影响正确性
> —— 三个条件互斥（`CjMacroCallFile` 与 `CjFile` 是不同 FileType，`isDeclarationFile` 只在 `CjFile` 上为真）。

#### 4.2.5 声明模式的文件入口（v2 修正：**新增入口**，不改 `parseFile` 签名）

文件：`psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt`（修改，**实测 `:1310-1327`**）

**实测现状**（注意：`parseFile` 是 **no-arg** 的，`with(ParsingContext.DEFAULT)` 硬编码在函数体内）：

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

**本仓既有先例**：`parseOnlyAnnotationFile()`（`:1280-1297`）是**完全同构**的另一个入口 ——
同样 no-arg，同样在函数体内硬编码自己的上下文 `with(ParsingContext.ANNOTATION_ONLY)`。

⇒ 本仓的既定模式是「**一个文件级入口 = 一个硬编码的 `ParsingContext`**」，而不是"一个入口 + 上下文参数"。

**v2 决策：照既有模式新增第三个入口，不修改 `parseFile` 的公共签名。**

```kotlin
/** 声明文件模式（`.cj.d`）的文件入口。 */
fun parseDeclarationFile() {
    with(ParsingContext.DECLARATION_FILE) {
        parseFileBody()
    }
}

fun parseFile() {
    with(ParsingContext.DEFAULT) {
        parseFileBody()
    }
}

/** 文件体解析的共享实现（`parseFile` / `parseOnlyAnnotationFile` / `parseDeclarationFile` 共用）。 */
private fun parseFileBody() {
    val fileMarker = mark()
    parsePreamble()
    while (!eof()) { parseTopLevelDeclaration() }
    checkUnclosedBlockComment()
    fileMarker.done(CJ_FILE)
}
```

> **为什么放弃 v1/v2 初稿的 `parseFile(parseContext = DEFAULT)` 方案**：
> ① 与本仓既有约定不符 —— `parseOnlyAnnotationFile` 已经确立了"独立入口"模式，改成参数化会让三个同类入口出现两种风格；
> ② `parseFile()` 是 no-arg 公共 API（被 `CangJieParser.parse`、`CangJieLightParser.parseWith` 等调用），
> 参数化需要 `@JvmOverloads` 并触碰所有调用点，改动面大于新增入口；
> ③ 新增入口**不改变任何既有调用点的语义**（`parseFile()` 行为逐字不变），对测试夹具零影响。
>
> 代价：若不做 `parseFileBody` 提取会有 ~15 行重复；提取后三个入口各自只有一个 `with` 块，重复归零。

#### 4.2.6 六个抑制点的本仓落位（v2：已逐点实测）

> v1 此表存在两处缺陷：① 把**函数签名行**（`parsePropertyGet` / `parsePropertySet` / `parsePrimaryInitFunc` 的 `fun` 行）
> 误写成**诊断行**，行号整体偏移；② P2/P3 留了"psi 中对应的…校验点"占位符。
> 下表为实测结果，行号对应诊断上报点本身。

**P1 —— 缺函数体诊断：必须分成两组，只有 A 组可豁免**

> ⚠️ **v2 二次修正（重要）**：初稿把"所有产生 `function.body.expected` / `expecting.symbol "{"` 的点"一律列为 P1，
> 这是**错误**的。逐点读代码后发现，其中 5 个是**"关键字后紧跟 `}`"的语法错误恢复路径**，
> 它们对应的输入（如 `class C { func }`）在 `.cj` 和 `.cj.d` 中**同样非法**，
> 官方 `parseDeclFile` 也**不会**抑制它们。把它们放进豁免清单会让 `.cj.d` **静默吞掉真实的语法错误**，偏离官方语义。

**A 组 —— 合法"无体声明"路径：7 个上报点，必须豁免**

| # | 解析函数 | 诊断行 | 触发文本（合法写法） |
|---|---|---|---|
| 1 | `parsePropertyGet` | `CangJieParsing.kt:2838` | `prop p: Int64 { get() }`（getter 无 `{}`）—— 对应官方 `ParsePropMemberBody` → `DiagMissingBody` |
| 2 | `parsePropertySet` | `CangJieParsing.kt:2879` | `prop p: Int64 { set(v) }`（setter 无 `{}`） |
| 3 | `parseMainFunc` | `CangJieParsing.kt:3777` | `func main()` 无体 |
| 4 | `parseFunction` | `CangJieParsing.kt:3876` | `class C { func f(): Int64 }` —— **最典型**，对应官方 P6 |
| 5 | `parseInitFunctionBody` | `CangJieParsing.kt:4145` | `init()` 无体 |
| 6 | `parseInitFunctionBody` | `CangJieParsing.kt:4137` | `init(): Type` 后缺 `{`（`mark.error` 形式） |
| 7 | `parseFunctionBody` | `CangJieParsing.kt:4157` | 通用函数体缺失（被 #3/#4/#5 调用） |

改法统一为 `if (!parseContext.isDeclarationFile) { error(...) }`。

> **注意 `:4137` 是 `error.error(...)`（先 `mark()` 再 `error`）**，不是裸 `error(...)`；
> 抑制时必须连同那个 `mark()`（`:4135`）一起跳过，否则会留下未 `done` 的标记节点。

**B 组 —— 语法错误恢复路径：5 个上报点，禁止豁免**

这些点的共同模式是「**`advance()` 跳过关键字后立即 `if (at(RBRACE))`**」——
即 `func` / `macro` / `main` / `init` / 类名之后**直接就是 `}`**，说明连名字或参数列表都没有。
这是明显残缺的输入，`.cj` 与 `.cj.d` 中一律非法。

| # | 解析函数 | 诊断行 | 触发文本（非法写法） | 位置特征 |
|---|---|---|---|---|
| 1 | `parsePrimaryInitFunc` | `CangJieParsing.kt:3595` | `class C }`（缺 `{`） | `advance() // IDENTIFIER` 之后 `at(RBRACE)`（`:3591-3597`） |
| 2 | `parseInitFuncRest` | `CangJieParsing.kt:3642` | `init }`（缺参数列表） | `advance() // INIT_KEYWORD` 之后 `at(RBRACE)`（`:3633-3644`） |
| 3 | `parseMainFunc` | `CangJieParsing.kt:3755` | `main }`（缺 `()`） | `advance()` 之后 `at(RBRACE)`（`:3751-3757`） |
| 4 | `parseFunction` | `CangJieParsing.kt:3815` | `class C { func }`（缺函数名） | `advance() // FUNC_KEYWORD` 之后 `at(RBRACE)`（`:3808-3817`） |
| 5 | `parseMacro` | `CangJieParsing.kt:4089` | `macro }`（缺宏名） | `advance() // MACRO_KEYWORD` 之后 `at(RBRACE)`（`:4085-4091`） |

> **判别规则（写进实现注释，防止后来者一刀切）**：
> **看 `if (at(RBRACE))` 出现的位置，而不是看诊断消息文本。**
> - 出现在"关键字 `advance()` 之后、且尚未解析名字/参数列表" ⇒ **B 组，不豁免**；
> - 出现在"名字、参数列表、返回类型都已解析完，只是缺 body" ⇒ **A 组，豁免**。
>
> 这两组用的是**同一个诊断消息**（`parsing.error.function.body.expected`），因此**靠消息文本无法区分**，
> 必须在每个上报点按上下文分别处理。这一条是 v2 初稿的主要错误来源。

**C 组 —— 其它 `expecting.symbol "{"` 上报点：不属于缺函数体语义，禁止豁免**

| 行 | 归属 | 为何不豁免 |
|---|---|---|
| `CangJieParsing.kt:635` | `expect(LBRACE, ...)` | 通用块起始，非声明体 |
| `CangJieParsing.kt:3994` | `parseSynchronized` | `synchronized` **表达式**必须有块，与声明模式无关 |
| `CangJieParsing.kt:4021` | `parseForeign` | `foreign { }` 块体必须有 `{}` |
| `CangJieParsing.kt:4124` | `parseMacro` 宏体 | **单独登记为待核事项**：`parseMacro` 的语法注释写的是 `functionBody?`（可选），但实现要求必有体。该矛盾与声明模式无关，本次**不放开**，也不在本特性内解决 |

**P2 / P3 —— 本仓 psi 层不存在对应诊断（N/A，结论已变更）**

v1 在此处留了占位符。实测结论是：**本仓 psi 层根本不产生"const 缺初始化""顶层 var 缺初始化"诊断**。

- 证据：`psi/resources/messages/CangJieParsingBundle.properties` 中与初始化相关的键**只有一个**——
  `parsing.error.field.requires.type.or.initializer`（"Field declaration must have a type annotation or an initializer"），
  其唯一调用点为 `CangJieParsing.kt:2297`，语义是"**类成员字段**必须带类型注解或初始化器"，与官方的 P2/P3 语义不同。
- ⇒ **P2/P3 在本仓由语义层承担**，豁免必须落在 checker 内部 early-return，而不是解析层。
  对应 checker 见 4.4.5：`CfirConstVariableInitializerChecker`（const 变量）、
  `CfirFileStaticGlobalInitializationChecker`（文件级静态/全局初始化）。
- ⇒ **`parsing.error.field.requires.type.or.initializer`（`:2297`）本次不豁免**：官方 P2/P3 未覆盖"字段必须带类型或初始化器"，
  且 `.cj.d` 中 `class C { let x: Int64 }` 本身就带类型，不触发该诊断；无类型无初始化的字段在 `.cj.d` 中同样非法。

**P4 —— 缺属性体**

| 官方点 | 本仓对应位置 | 改法 |
|---|---|---|
| P4 `DiagMissingPropertyBody` | `CangJieParsing.kt:2779-2782`（`parseProperty` 的 `else if (!isInterface)` 分支内两条 `error(...)`） | **只抑制这两条 `error`，保留 `classdetector != null && !classdetector.isAbstractDetected` 的分支结构与 class/interface 作用域下"无 `{}` 即 abstract"的行为**（R4） |

**P5 —— `HAS_BROKEN`：N/A**

`psi` 中不存在 `HAS_BROKEN` 概念（既无属性位也无对应降级路径）。⇒ 本仓无对应落点，也不会触发"破损节点"降级。

**P6 —— `CheckFuncBody`（最关键的一条）**

| 官方点 | 本仓对应位置 | 改法 |
|---|---|---|
| P6 `CheckFuncBody` 整块跳过 | `CangJieParsing.kt:3872-3877`（`parseFunction` 的 `else if` 条件与诊断） | 在条件首位追加 `!parseContext.isDeclarationFile &&` |

⇒ 即 `.cj.d` 中**无体的类成员函数既不打 `abstract`、也不报错**（R3）。函数与属性的不对称在 4.2.6 P4 与
4.5.2 护栏 1 处必须被**分别**实现，任何"统一处理"都会破坏 R3 或 R4 中的一条。

`parseFunction` 改动示意：

```kotlin
if (at(LBRACE)) {
    parseFunctionBody()
    if (isForeign) { error(...) }
} else if (
    !parseContext.isDeclarationFile &&                       // ← 新增，对齐 P6
    !(isInterfaceMethod || (detector?.isAbstractDetected == true)) &&
    (detector?.isForeignDetected != true)
) {
    error(CangJieParsingBundle.message("parsing.error.expecting.symbol", "{"))
}
```

`parseProperty` 改动示意（**注意不对称**）：

```kotlin
if (at(LBRACE)) {
    parsePropertyBody(detector)
} else if (!isInterface) {
    if (classdetector != null && !classdetector.isAbstractDetected) {
        if (!parseContext.isDeclarationFile) {                 // ← 只抑制诊断
            error(CangJieParsingBundle.message("parsing.error.unimplemented.abstract.property"))
            error(CangJieParsingBundle.message("parsing.error.missing", "prop body. Expecting '{''"))
        }
    }
}
// class/interface 作用域下"无 {} 即 abstract"的行为保持不变（对齐 R4 / Parser.cpp:371-390）
```

#### 4.2.7 `CjFileElementType` 与 Stub

`.cj.d` 复用 `CjFileElementType`（Stub 类型），因此会正常建 Stub 索引，`.cj.d` 内部的符号导航/补全/Find Usages 可用。

- `CangJieFileStubKind` **不需要**新增 `Declaration` 种类：当前 `WithPackage.File` 已足够表达；是否分类由 `CjFile.isDeclarationFile` 承担。
- **Stub 版本无需升级**：本次不改变任何节点或字段的序列化布局。

> 参考实现对声明文件使用**非 Stub** 的 `IFileElementType`（`CangjieDeclarationParserDefinition.java:165-178`）以避免建索引。本仓选择建索引，因为 `.cj.d` 需要完整的导航与补全能力（见 4.9.3）。**这是一个有意的差异**（`DIFF-3`），理由是本仓的 `.cj.d` 使用场景更偏向"可编辑的 SDK 头文件"，需要索引。
> **评审关注点（v2：已完成双向处置）**：若 `.cj.d` 被放入源码根，Stub 索引会与 `.cjo` 产生同名符号。处置为：
> ① **逻辑侧** —— `.cj.d` 不进入声明提供者聚合（D7，见 4.9.3）；
> ② **索引侧** —— 对源码根下 `*.cj.d` 加排除（4.9.3 "v2 新增缺口"）。
> v1 只做了 ①，且把出处误写成 4.9.2（正确为 4.9.3）。

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

**两级判定，互斥分流**（R1）：

```text
sourceKind == SOURCE       && !compileCjd  → 收集
sourceKind == DECLARATION  &&  compileCjd  → 收集
其它组合                                    → 拒绝
```

> 注意：`compileCjd == true` 时，`.cj` 必须被**拒绝**，而不是"顺便也收下"。

#### 4.4.2 LightTree 路径

文件：`compiler/frontend/src/org/cangnova/cangjie/frontend/sources/GroupedCjSources.kt`（修改，`:114-151`）

```kotlin
val isDeclarationMode = compilerConfiguration.compileCjd

// 新增：统一的种类判定。VirtualFile.extension 对 "a.cj.d" 返回 "d"，不能直接用。
fun VirtualFile.sourceKind(): CjSourceKind =
    if (fileType is CangJieDeclarationFileType ||
        CjSourceKind.fromFileName(nameSequence.toString()) == CjSourceKind.DECLARATION
    ) CjSourceKind.DECLARATION else CjSourceKind.SOURCE

filter = { virtualFile, isExplicit ->
    val kind = virtualFile.sourceKind()
    when {
        virtualFile.extension == "java" -> false
        kind.isDeclaration -> isDeclarationMode
        virtualFile.extension == CangJieFileType.EXTENSION ->
            !isDeclarationMode && virtualFile.fileType == CangJieFileType.INSTANCE
        else -> { /* 保持既有 else 分支：非 .cj 一律拒绝 */ }
    }
}
```

要点：
- 必须用 `nameSequence`（完整文件名）而不是 `extension`（最后一段）做后缀判定（R2）。
- `convertToSourceFiles` 分支（`:139-151`）的 `virtualFile.extension == CangJieFileType.EXTENSION` 判断要改成 `sourceKind == SOURCE`，否则声明文件会误入 `applyCfirProcessSourcesExtension` 插件扩展路径。

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

#### 4.4.5 声明模式下的 Sema 豁免（v2：已逐 checker 落到类名）

**先定判定规则，再列表。** 官方对声明模式的 Sema 豁免是**白名单**：只有 1.2.5 表中的 6 处提前返回，
其余**全部照跑**。因此本仓每个 checker 的处置都可以机械判定，不存在"待定"：

| 判定 | 规则 |
|---|---|
| **豁免** | 该 checker 的语义落在这 6 类之一（见下表"官方依据"列） |
| **保留** | 不在 6 类之内 —— 官方没豁免的，本仓一律不豁免 |
| **天然不触发** | `.cj.d` 无 body ⇒ 依赖 body 内部表达式的 checker 自然无输入 |

**A. 需豁免的 checker（含本仓特有项）**

| checker | 所属槽位 | 官方依据 | 说明 |
|---|---|---|---|
| `CfirConstVariableInitializerChecker` | `callableDeclarationCheckers` | `InitializationChecker.cpp:503` | const 变量缺初始化不报（**这是 4.2.6 中 P2 的实际落点**） |
| `CfirFileStaticGlobalInitializationChecker` | `fileCheckers` | `InitializationChecker.cpp:503` + P3 语义 | 文件级静态/全局初始化不报（**这是 P3 的实际落点**） |
| `CfirConstructorInitializationChecker` | `constructorCheckers` | `InitializationChecker.cpp:1517` | 构造函数初始化完整性检查提前返回 |
| `CfirClassLikeInitializationChecker` | `classLikeCheckers` | `InitializationChecker.cpp:1517` | 同上（类级侧面） |
| `CfirFunctionInitializationChecker` | `functionCheckers` | `InitializationChecker.cpp:1517` | 同上（函数级侧面） |
| `CfirNotImplementedOverrideChecker` | `classLikeCheckers` | `StructInheritanceChecker.cpp:830-842` | `.cj.d` 的成员视为 `ignored`，不报"未实现接口成员" |
| `CfirCommonPackageMainChecker` | `fileCheckers` | `TypeChecker.cpp:2215-2230` | **入口缺失**不报（注意：不是 main 签名检查） |
| `CfirPropertySemanticsChecker` | `propertyCheckers` | `DeclAttributeChecker.cpp:332-336` | 不要求 `var` 属性同时具备 getter/setter |
| `CfirPropertyAccessorDeclarationChecker` | `propertyAccessorCheckers` | `DeclAttributeChecker.cpp:332-336` | 同上（访问器侧面） |
| `CfirOpenMemberChecker` | `classLikeCheckers` | `DeclAttributeChecker.cpp:284-291` | 不检查"非抽象类中成员被标 abstract""abstract/open 成员可见性" |
| `CfirFinalizerDeclarationChecker` | `functionCheckers` | `DeclAttributeChecker.cpp:284-291` | 不检查"open 类中 finalizer" |
| **`CfirMemberBodyDeclarationChecker`** | `memberDeclarationCheckers` | **本仓特有**（推导自 P6 / R3） | 官方在 parser 层就允许无体；本仓把"成员体"判断下沉到 checker，故**必须显式豁免**，否则 `.cj.d` 的每个无体成员都会被它报错。这是 v1 完全遗漏的一项 |

**B. 明确保留的 checker（不得豁免）**

| 槽位 | checker | 保留理由 |
|---|---|---|
| `basicDeclarationCheckers` | `CfirBuiltInAnnotationDeclarationChecker`、`CfirAnnotationTargetChecker`、`CfirCAnnotationChecker`、`CfirConflictsDeclarationChecker`、`CfirModifierChecker`、`CfirTypeConstraintsChecker` | 注解 / 修饰符 / 泛型约束 —— 官方全部保留 |
| `functionCheckers` | `CfirForeignFunctionParameterTypeChecker`、`CfirForeignFunctionReturnTypeChecker`、`CfirFunctionReturnTypeInferenceChecker`、`CfirConstFunctionBodyChecker` | ABI 与签名检查 |
| `mainFunctionCheckers` | `CfirMainFunctionSignatureChecker` | 官方豁免的是**入口缺失**，不是**签名**。签名检查保留 |
| `classLikeCheckers`（其余） | `CfirSupertypesChecker`、`CfirOverrideChecker`、`CfirValueTypeRecursiveChecker`、`CfirAnnotationDeclarationChecker`、`CfirInteropAnnotationChecker`、`CfirInheritanceDeepChecker`、… | 继承 / override / 可见性 —— 官方保留（R6 明确列举） |
| `extendCheckers`（全部 15 个） | `CfirExtendTargetLegalityChecker`、`CfirExtendOrphanRuleChecker`、… | 官方 `StructInheritanceChecker` 只豁免"未实现接口成员"，extend 的合法性检查全部保留 |
| `typeParameterCheckers` | `CfirGenericDeepChecker`、`CfirTypeParameterBoundsChecker` | 泛型约束（1.2.5 明确保留） |
| `valueParameterCheckers` | `CfirValueParameterDefaultValueTypeMismatchChecker`、`CfirConstructorParameterThisOrSuperDefaultValueChecker` | 参数默认值类型（保留） |
| `fieldVariableCheckers` | `CfirFieldVariableInitializerTypeMismatchChecker`、`CfirFieldVariableThisOrSuperInitializerChecker` | 类型不匹配保留（**类型**检查，非**完整性**检查）；`this`/`super` 初始化器检查保留 |
| `fileCheckers`（其余） | `CfirImportsChecker`、`CfirFeaturesDirectiveChecker`、`CfirPlatformAnnotationMacroOrderChecker`、`CfirGeneralSemanticsChecker`、`CfirGenericInstantiationChecker` | import 检查明确保留（R6） |
| `simpleFunctionCheckers` | `CfirOperatorDeclarationChecker`、`CfirFunctionOverloadChecker`、`CfirDefaultParameterChecker`、`CfirFunctionDeclarationStatusChecker` | 运算符 / 重载 / 默认参数（保留） |
| `callableDeclarationCheckers`（其余） | `CfirDeprecatedDeclarationChecker`、`CfirVArrayExtraChecker`、`CfirAnnotationArgNumberCallableChecker`、… | 注解参数与废弃标记（保留） |
| `typeAliasCheckers`、`anonymousFunctionCheckers`、`patternVariableCheckers`、`invalidDeclarationCheckers` | 全部 | 官方无豁免 |
| `CfirPatternVariableInitializerTypeMismatchChecker` | `patternVariableCheckers` | 类型不匹配，保留 |

**APILevel / SysCap 检查**：**保留**，且**不需要任何豁免**。实测确认
`CfirApiLevelRefHigherChecker` 注册在 `CommonExpressionCheckers.qualifiedAccessCheckers`（`CommonExpressionCheckers.kt:125, 141`），
其数据源 `CfirDeclarationAvailabilityProvider.findAnnotations`（`CfirDeclarationAvailabilityProvider.kt:88-91`）与
`ownApiLevelInfo`（`:104-119`）**直接读 `declaration.annotations` 的实时值**，因此 sidecar 合并（4.6.5）一旦写回即自动生效。

> **实现方式（v2 定稿）**：**不采用 v1 的"分组注册裁剪"**。改为与官方同构的**逐 checker early-return**：
> 在 A 表列出的每个 checker 内部读 `configuration.compileCjd` 后提前返回。
> 理由：① 与官方一一对应，可审计；② 分组注册会连同 B 表里必须保留的检查一起被裁掉，粒度过粗；
> ③ `useCheckers(...)` 的注册粒度是"集合"，无法表达"同一集合内只豁免其中 3 个"。
>
> **验收**：A 表每一项都要有一个断言"`.cj.d` 输入下该 checker 不产出诊断"的用例，且必须配一条
> "同一文本在 `.cj` 输入下该 checker 产出诊断"的反向用例（6.1 双向验证原则）。

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

**护栏 2 —— interface 默认成员判定（v2：已复核，结论为「不改」）**

`isDefaultInterfaceMember`（`PsiRawCfirBuilder.kt:3902-3911`）与 LightTree 的对应函数依赖 `hasBody()` /
`hasPropertyBody()`。复核结论：

- `.cj` 的 interface 无体成员走 `isInterfaceMethod` 豁免 → `hasBody()` 为 false → 判为**非 default**。
- `.cj.d` 的 interface 无体成员走 P6 豁免 → `hasBody()` 同为 false → 判为**非 default**。
- ⇒ 两条路径的判定结果**本来就一致**，护栏在此处**无任何行为差异**，加护栏只是噪声。
- ⇒ **确认不改**（v1 的"需要复核"在此关闭）。

**护栏 3 —— `BodyBuildingMode` 不得被复用**

`BodyBuildingMode.LAZY_BODIES` 语义是"延迟构建 body"（PSI 侧产出 `CfirLazyBlock` 占位，LightTree 侧 `body = null`）。它**不是**声明模式：

- `LAZY_BODIES` 下 LightTree 的 `body = null` 与声明模式的 `body = null` **恰好同形**，但前者是"待恢复"，后者是"语义上不存在"，下游（如 `CfirTypeVariablesAfterPCLATransformer.kt:97` 对 `CfirLazyBlock` 的跳过）行为不同。
- 生产入口 `analyse.kt:57` / `:78` 不传该参数，当前 `LAZY_BODIES` 只被测试使用。
- ⚠️ LightTree 侧已有防御注释（`LightTreeRawCfirDeclarationBuilder.kt:3284-3285`："LightTree 路径必须基于源码语法判断，不能受 lazy body 构建模式影响"），
  且 `hasSyntaxBody`（`:3323-3324`）只读 LightTree 结构、不读已构造的 CFIR body。**这条既有防御与声明模式护栏正交，不要顺带改它。**

⇒ **明确禁止**：不得用 `bodyBuildingMode = LAZY_BODIES` 来实现声明模式。声明模式必须走独立的 `isDeclarationFile` 通路。

**护栏 3 —— `BodyBuildingMode` 不得被复用**

`BodyBuildingMode.LAZY_BODIES` 语义是"延迟构建 body"（PSI 侧产出 `CfirLazyBlock` 占位，LightTree 侧 `body = null`）。它**不是**声明模式：

- `LAZY_BODIES` 下 LightTree 的 `body = null` 与声明模式的 `body = null` **恰好同形**，但前者是"待恢复"，后者是"语义上不存在"，下游（如 `CfirTypeVariablesAfterPCLATransformer.kt:97` 对 `CfirLazyBlock` 的跳过）行为不同。
- 生产入口 `analyse.kt:57` / `:78` 不传该参数，当前 `LAZY_BODIES` 只被测试使用。

⇒ **明确禁止**：不得用 `bodyBuildingMode = LAZY_BODIES` 来实现声明模式。声明模式必须走独立的 `isDeclarationFile` 通路。

**护栏 4 —— 主构造参数合成的访问器**

PSI 侧 `PsiRawCfirBuilder.kt:1056-1076`（主构造参数合成 accessor，`body = null`）、LightTree 侧 `:681, :717`。
这些是**语言规定的隐式合成**，与声明模式无关，**不改**。

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

2. **只提取注解与匹配键，不构建 CFIR**。构建完整 declaration CFIR 的成本与复杂度都不必要——sidecar 的产出物只是"签名 → 注解列表"。

3. **支持两种输入源（与 4.4 的源收集一致），且两者都必须走声明模式**：
   - 磁盘上的 `.cj.d` → LightTree 轻量解析。
   - 内存中的 `.cj.d`（IDE 未保存的编辑器内容）→ 走 PSI（4.2 的 `CjFile(isDeclarationFile = true)` 路径，天然已具备）。

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
   > ⇒ **落点（与 4.2.5 同一思路：新增入口，不改既有签名）**：给 `CangJieLightParser` 新增
   > ```kotlin
   > /** 按声明文件语法构造 LightTree。 */
   > fun parseDeclaration(
   >     builder: PsiBuilder,
   >     errorListener: LightTreeParsingErrorListener? = null,
   >     languageModuleName: String = "",
   > ): FlyweightCapableTreeStructure<LighterASTNode> =
   >     parseWith(builder, errorListener, languageModuleName) { parseDeclarationFile() }
   > ```
   > 与既有的 `parse`（→ `parseFile()`）、`parseAnnotationOnly`（→ `parseOnlyAnnotationFile()`）构成完整的三入口族，
   > 风格完全一致。

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
| `Rune` 与 `UInt8` 归一 | 对齐 `MergeAnnoFromCjd.cpp:147-148` |
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

#### 4.9.2 解析与文件创建

文件：`psi/src/org/cangnova/cangjie/parsing/CangJieParserDefinition.kt`（修改，**实测 `:106-115`**）

现状（原文照录）：

```kotlin
override fun createFile(viewProvider: FileViewProvider): PsiFile {
    return when (viewProvider.fileType) {
        is CangJieFileType -> CjFile(viewProvider, false)
        else -> PsiPlainTextFileImpl(viewProvider)
    }
}
```

改为：

```kotlin
override fun createFile(viewProvider: FileViewProvider): PsiFile {
    return when (viewProvider.fileType) {
        is CangJieDeclarationFileType -> CjFile(viewProvider, isCompiled = false, isDeclarationFile = true)
        is CangJieFileType -> CjFile(viewProvider, false)
        else -> PsiPlainTextFileImpl(viewProvider)
    }
}
```

> **v2 更正 —— v1 对"不改会怎样"的描述是错的，风险性质完全不同。**
>
> v1 写"若不改这里，`.cj.d` 会 fallthrough 到 `PsiPlainTextFileImpl`"。**实测并非如此**：
> D1 已决定 `CangJieDeclarationFileType` **继承** `CangJieFileType`，因此 `when` 的第一条
> `is CangJieFileType` 对 `.cj.d` **为真**，它会被该分支接住并创建 `CjFile(viewProvider, false)`。
> 也就是说：**`.cj.d` 永远走不到 `else` 分支**，"退化成纯文本 PSI"这个故障模式在本设计下不会发生。
> （`.cjo` 之所以走 else，是因为 `CangJieBuiltInFileType` 是**独立的 `FileType` object、不继承 `CangJieFileType`** —— 见 `CangJieDeclarationsFileType.kt:37`。）
>
> **真实的故障模式是"静默降级"，比 v1 描述的更难发现**：
> `.cj.d` 会得到 `isDeclarationFile = false` 的普通 `CjFile` ⇒ 4.2.4 的解析分支不生效 ⇒
> 按 `.cj` 语义解析 ⇒ **编辑器里每个无体函数都报"Function body expected"波浪线**，
> 而文件类型、高亮、折叠看起来全部正常。⇒ 这不是"功能缺失"，而是"**看起来能用、实际全错**"。
>
> ⇒ **施工要求**：此处必须改，且 6.6 的验收项要从"`.cj.d` 不是纯文本"改成
> **"`.cj.d` 打开后得到 `CjFile(isDeclarationFile = true)` 且无 `Function body expected` 波浪线"**（后者才是能区分这两种故障的断言）。
>
> **顺序仍然关键**：`CangJieDeclarationFileType` 是 `CangJieFileType` 的子类型，声明分支必须放在前面，
> 否则会被 `is CangJieFileType` 语义上"覆盖"（Kotlin `when` 从上到下匹配）。
> 这个顺序依赖不是风格问题，是**正确性前提**。

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
 * `.cj.d` 自身的 PSI/高亮/导航由 `CangJieDeclarationFileType` + `CjFile(
 * isDeclarationFile = true)` 承载，与此处的声明聚合无关。
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

| 阶段 | 内容 | 依赖 | 验收标准 |
|---|---|---|---|
| **P0** | 公共契约：`CjSourceKind`、`CjSourceKindCarrier`、`CompilerConfiguration.compileCjd`、`compilerArguments` 的 `-d` | — | 单元测试覆盖 `CjSourceKind.fromFileName` 的全部边界（`.cj.d`、`a.cj.d`、`.cjd`、`a.CJ.D`、`a.cj.d.txt`）；**生成产物 `CommonCompilerArguments.kt` 由 6 个属性变为 7 个且 `git diff` 只多出 `d`**（v2：不再要求"记录漂移"，实测无漂移） |
| **P1** | `psi` 声明解析模式：`CangJieDeclarationFileType`、`CjFile.isDeclarationFile`、`ParsingContext.isDeclarationFile`、**新增 `CangJieParsing.parseDeclarationFile()` 入口**、`CangJieParser.parse` 三分支、**P1 的 7 个豁免点（4.2.6 A 组）+ P4 的 1 处 + P6 的 1 处**（P2/P3/P5 为 N/A，见 4.2.6） | P0 | 解析测试：`.cj.d` 中无体函数/构造函数/属性访问器/**main** 均无 error 节点；**且 4.2.6 B 组 5 个 + C 组 4 个点仍照常报错**（尤其 `class C { func }` 必须仍报错）；**同一文本在 `.cj` 下必须报错**（双向验证） |
| **P2** | CFIR declaration 护栏：**仅** `isImplicitAbstractClassLikeMember` 的 `CjNamedFunction` 分支（`PsiRawCfirBuilder.kt:3895`）与 `isImplicitAbstractClassLikeFunction`（`LightTreeRawCfirDeclarationBuilder.kt:3287`）加 `!isDeclarationFile`；**`CjProperty` 分支与 `isImplicitAbstractClassLikeProperty` 保持不动** | P1 | 正向：`.cj.d` 中无体类成员**函数不**被标记 implicit abstract；`.cj` 中同样文本**被**标记。**反向（R4 护栏）**：`.cj.d` 中 `class C { prop p: Int64 }` **仍被**标记 implicit abstract（若此断言失败，说明护栏加错了分支） |
| **P3** | 源收集与声明模式：`GroupedCjSources`、`coreEnvironmentUtils` 的两级互斥过滤；检查器豁免 | P0 | 收集测试：`compileCjd=false` 只收 `.cj`；`compileCjd=true` 只收 `.cj.d`；两者都**不**收对方 |
| **P4** | **sidecar 合并**：`CjdSidecarLocator`、`CjdSidecarIndex`、`DeclarationMatchKey`、`CfirDeclDeserializer` 合并点、provider 失效 | P0 | 见第 6 章 sidecar 测试矩阵；**必须验证 4.6.6 的"声明对象不被跨 provider 复用"** |
| **P5** | IDE 侧：`cangjie-filetypes.xml`、`CangJieParserDefinition.createFile`（**声明分支前置**）、**`CjFile.getFileType()` 返回 `viewProvider.fileType`**（OP1）、**源码根 `*.cj.d` 排除出 Stub 索引**（OP2）、收集器注释与 `deveco` 副本对齐（OP3）、`CjDependency.Binary.derivedCjdPath` | P0, P1, P4 **+ 主仓发布 common/psi 工件到 `build/repo`**（见 4.10；联调须 `--refresh-dependencies`） | 手工验收：打开 `.cj.d` 得到 Cangjie 高亮 + 可导航 + **无 `Function body expected` 波浪线**（注意：不是"不是纯文本"——见 4.9.2 更正）；`.cj.d` 的 `getFileType()` 为 `CangJieDeclarationFileType`；`.cj.d` 不出现在声明提供者聚合中、不出现在 `classesRoots` 中、不出现在 Stub 索引符号中 |
| **P6** | `analysis/decompiled` 侧注解注入、`lsp` 接入、`deveco` SDK 布局 | P4, P5 | light declaration / 文档中能看到 `.cj.d` 提供的 `@APILevel` |

**阶段解释**

- **P4 是本设计的核心**，也是风险最高的部分。它独立于 P0-P3 之外，可以并行推进；P1/P2/P3 不依赖 P4。
- **P2 必须紧接 P1**：若 `.cj.d` 能解析出无体成员、但 CFIR 仍把它们当隐式抽象，会得到"能打开但语义全错"的更糟状态。两者必须同批交付。
- **P3 的检查器豁免与官方的粒度对齐**（4.4.5 结尾）：建议在单个 checker 内部按 `configuration.compileCjd` early-return，而不是靠分组注册开关，以便与官方逐项对照审计。

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
| `let g: Int64`（顶层） | 报缺初始化 | 无诊断 |
| `const c: Int64` | 报缺初始化 | 无诊断 |
| `main()`（无体） | 报缺函数体 | 无诊断 |
| `~init()`（finalizer 无体） | 报缺函数体 | 无诊断 |
| `func f(): Int64 { 1 }`（顶层有体） | 无诊断 | 无诊断（有体本就合法） |
| 同一文件同时含 `.cj` / `.cj.d` 语义 | — | 需独立文件，模式按**文件**而非片段生效 |

> **P2 补充用例**：对 `.cj.d` 的无体类成员函数，断言其在 CFIR 中的 `attributes` **不含** implicit abstract，且不因该属性触发 override/实例化类诊断。
>
> **P2 反向用例（v2 新增，防"护栏加错分支"）**：对 `.cj.d` 中的 `class C { prop p: Int64 }`（无 `{}`、无 getter/setter），
> 断言其在 CFIR 中**仍然**带 implicit abstract。这条与上一条构成**互斥断言对**，
> 用于锁定 R3/R4 的不对称性；若实现者把护栏加在了 `when` 的整体入口（而不是 `CjNamedFunction` 分支），本用例会失败。
> LightTree 路径需有独立的一份相同断言（两个函数是分开实现的）。

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
| **`x.cj.d` 的 `CjFile`**（v2 新增） | `isDeclarationFile == true`，且 `getFileType() === CangJieDeclarationFileType`（OP1 断言） |
| **`x.cj` 的 `CjFile`**（v2 反向断言） | `isDeclarationFile == false`，`getFileType() === CangJieFileType` |
| `.cj.d` 中的无体函数 | **无** `Function body expected` 波浪线（**这是 4.9.2 更正后唯一能区分"静默降级"的断言**） |
| `.cj.d` 中的无体 `main` / 主构造 / getter / setter | 同样**无**波浪线（覆盖 4.2.6 A 组 7 个豁免点） |
| **`class C { func }`（缺函数名）**（v2 二次新增，反向） | **仍报错**（4.2.6 B 组未被误豁免）；同理 `main }`、`init }`、`macro }`、`class C }` 也必须仍报错 |
| `.cj.d` 中的 `synchronized` 表达式缺块（v2 新增，反向） | **仍报错**（4.2.6 排除清单未被误豁免） |
| `.cj.d` 中的 prop | 高亮/折叠/括号匹配正常 |
| `.cj.d` 内部符号导航 | 可用 |
| `.cj.d` 是否出现在声明提供者聚合 | **否**（D7） |
| `.cj.d` 是否加入 `classesRoots` | **否** |
| **源码根下的 `.cj.d` 是否进入项目 Stub 索引符号**（v2 新增） | **否**（OP2 / 4.9.3 新增缺口） |
| `x.cj.d` 与 `x.cjo` 并存时 | 无重复符号告警 |
| 普通项目构建 | 流程不变，不感知 `.cj.d` |

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
- **爆炸半径已枚举（实测，可复核）**：
  - `psi` 内 `getFileType()` 的覆写**只有这一处**（`psi/src` 全量检索，另一处 `CangJieMacroCallFileType.kt:34` 是 `FileType` 自身的接口实现，与 `CjFile` 无关）。
  - `CangJieFileType.INSTANCE` 的引用共 6 处：`utils/virtualFileUtil.kt:39`、`psi/CangJieDeclarationUseScopePolicy.kt:47`、
    `psi/CjCodeFragment.kt:187/199/307`、`psi/CjFile.kt:66`、`psi/CjPsiFactory.kt:760`。
    **其中没有一处读取 `PsiFile.getFileType()`** —— 它们要么自己去查 `FileTypeRegistry`（`virtualFileUtil.kt:39`），
    要么在**构造** `LightVirtualFile` 时把 `CangJieFileType.INSTANCE` 作为参数传入（`CjCodeFragment` / `CjPsiFactory`）。
    后者的行为在改动后**完全不变**（那些文件本来就不是 `.cj.d`）。
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

### 7.4 交付前必须复核的清单（v2 更新）

**原有项（保留，措辞按 v2 结论修正）**

- [ ] 每一条 R1-R14 都有对应的可执行测试用例
- [ ] 每一个 `DIFF-*` 都在评审记录中确认
- [ ] **P1 的 7 个豁免点**（4.2.6 A 组）每个都有 `.cj` 反向用例；**且 B 组 5 个 + C 组 4 个点未被误豁免**（用 `class C { func }` 等残缺输入做守卫用例）
- [ ] `.cj.d` 未出现在任何源码集 / 声明集 / `classesRoots` 中（除声明模式编译）
- [ ] `deveco` 与 `intellij-ide` 的 `CaIdeScopeCangJieFileCollector` **正文一致**（OP3 的一致性校验）
- [ ] `external/` 下无任何改动

**v2 新增项（来自本轮问题闭环）**

- [ ] **CFIR 护栏只加在函数分支**：`isImplicitAbstractClassLikeMember` 的 `CjProperty` 分支、
      `isImplicitAbstractClassLikeProperty` **未被改动**（R4 不被破坏）
- [ ] **`CfirMemberBodyDeclarationChecker` 已豁免**（4.4.5 A 表最后一行，本仓特有项）
- [ ] **sidecar 合并点位于 `CfirDeclDeserializer.kt:153` 与 `:154` 之间**（三条约束同时满足：target 归一化 / 快照完整 / marker 不重复）
- [ ] **`CjFile.getFileType()` 已返回 `viewProvider.fileType`**（OP1 翻转项）
- [ ] **源码根下的 `*.cj.d` 已被排除出 Stub 索引**（OP2 / 4.9.3 新增缺口）
- [ ] **`CangJieParserDefinition.createFile` 的声明分支顺序正确**（声明类型在 `is CangJieFileType` 之前），
      且验收断言是"无 `Function body expected` 波浪线"而非"不是纯文本"（4.9.2 更正）
- [ ] **新增 `-d` 后生成产物只多出 `d` 一个属性**（DSL 与产物无漂移，4.3.2 更正）
- [ ] **`CangJieParsingBundle.properties:2297` 的 `field.requires.type.or.initializer` 保持不豁免**（4.2.6 结论）
- [ ] **CI/评审确认 `build/` 下产物不得作为设计依据**（4.9.1 更正项的教训固化）
- [ ] **主仓已发布 `cangjie-frontend-common-for-ide` / `-psi-for-ide` 到 `build/repo`，且 `intellij-ide` 侧以
      `--refresh-dependencies` 重新解析**（4.10；P5 的真实前置条件，漏掉会表现为"找不到类"的假故障）
- [ ] **`project-model` 的 `derivedCjdPath` 使用 `CjSourceKind.DECLARATION_SUFFIX` 常量，未写 `".cj.d"` 字面量**（4.9.4；否则违反 3.1 原则 5）
- [ ] **P2 的护栏测试按 AST/Stub 双变体提交**，且经 `TestGeneratorForPsi2Cfir.kt` 生成而非手写（6.0）

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
| `Rune`/`UInt8` 归一 | `external/cangjie_compiler/src/Frontend/MergeAnnoFromCjd.cpp:147-148` |
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

## 8. v2 修订记录（问题闭环）

> 本章是 v2 相对 v1 的完整差异清单。**每条都可在本仓工作树中复核**；
> 凡"结论翻转"的项，v1 的原文判断是错的，不要以 v1 为准。

### 8.1 事实性错误更正（6 条，其中 E4 为文档格式损坏）

| # | v1 的错误表述 | v2 实测结论 | 影响章节 |
|---|---|---|---|
| E1 | "`compilerArguments.kt` 与生成产物**已存在漂移**——`gen` 里有 `useFir`" | **无漂移**。`useFir` 是 `CompilerConfiguration` 扩展属性（`CommonConfigurationKeys.kt:178-180`），与 CLI 参数无关；生成物的 6 = DSL 4 + 生成器合成 2 | 2.1、4.3.2、P0 验收 |
| E2 | "若不改 `createFile`，`.cj.d` 会 **fallthrough 到 `PsiPlainTextFileImpl`**" | **不会**。`CangJieDeclarationFileType` 继承 `CangJieFileType`，会被 `is CangJieFileType` 接住。真实故障是**静默降级为 `isDeclarationFile=false` 的普通 CjFile**（更难发现） | 4.9.2、P5 验收 |
| E3 | "`.cj.d` 的 `CjFile` 自称 `.cj` 类型" → **建议本次不改** `getFileType()` | **决策翻转：本次改**。爆炸半径已枚举为"只可能取 `CangJieFileType` / `CangJieDeclarationFileType` 两个值"，无未知风险；不改会留下 `psiFile.getFileType() ≠ viewProvider.fileType` 的自相矛盾状态 | 7.3-OP1、P5、6.6 |
| E4 | 附录 B 末尾混入一行被截断的 P2 表格行（文档损坏） | 已清除，并以正确条目替换 | 附录 B |
| E5 | "过期构建产物留有 `cjd` 草稿，本次应当清掉那段注释" | 该草稿在 `build/`（**产物**），源码 XML 中本无此注释 ⇒ **无执行对象**；且草稿内容（`extensions="cjd"`）本身就是错误表达 | 4.9.1 |
| E6 | **（v1 完全无此内容）** v1 与 v2 初稿都未记录"`intellij-ide` 是独立构建、通过 `libs.cangjie*ForIde` 发布工件消费主仓"这一事实 | 新增 **4.10**；P5 的依赖从"P1, P4"更正为「P0+P1+P4 **+ 主仓发布**」。这是 v1 最可能导致实施中途卡住的遗漏 | 4.10、第 5 章 |

### 8.2 未闭环项闭环（9 条）

| # | v1 的悬置表述 | v2 结论 |
|---|---|---|
| C1 | 4.2.6 P1 落点含 3 个不存在的行号（`3555`/`2821`/`2860`），且遗漏大半 | 补全为完整清单，并给出判别规则。（**注**：本次补全后又经第 9 章复查修正 —— 必须区分 A/B/C 三组，其中只有 A 组 7 点可豁免，见 V2-1） |
| C2 | 4.2.6 P2/P3 留 `psi` 中"对应的…校验点"占位符 | **psi 层不存在该诊断（N/A）**；实际落点转移到语义层 `CfirConstVariableInitializerChecker` / `CfirFileStaticGlobalInitializationChecker` |
| C3 | 4.4.5 "需要逐项核对各槽位" | 给出**逐 checker 清单**（A 表 12 项需豁免 / B 表按槽位保留），并确定实现方式为**逐 checker early-return**（否决分组注册） |
| C4 | 4.5.2 护栏 1 只说"给 `isImplicitAbstractClassLikeMember` 加护栏" | **必须只作用于函数分支**：该函数内含函数与属性两条分支，属性分支加护栏会**直接破坏 R4**；LightTree 侧同理（两个独立函数） |
| C5 | 4.5.2 护栏 2 "这一条需要复核" | **已复核：不改**。`.cj`/`.cj.d` 两条路径判定结果一致，护栏无行为差异 |
| C6 | 4.6.6 "**必须验证**：确认不存在跨 provider 复用的 `.cjo` 声明对象" | **验证完成**（四段证据链）。不存在共享，`provider` 生命周期即清理边界；该项从"验收项"降级为"回归防线" |
| C7 | 4.6.5 合并点理由表述错误（"provider 读快照"） | 改为**三条硬约束**（target 归一化 / 快照完整 / marker 不重复），并精确到 `:153`—`:154` 行间插入 |
| C8 | 4.3.2 "建议在 DSL 里补 `compilerName`（**以支持为准**）" | **已确认支持**：`compilerName` 存在于 `CangJieCompilerArgument.kt:55/137/192`，唯一消费点为 `Generator.kt:105-106, 130` 的 `calculateName()`，语义正是"生成类中的属性名"。可确定写出 `compilerName = "compileCjd"` ⇒ 生成 `var compileCjd`。并补充了 `?:` 优先级导致的"`compilerName` 不被 kebab 转换二次加工"这一易踩细节 |
| C9 | 4.2.1 "建议本次一并把 `CangJieBuiltInFileType` 迁到同名文件（**可选，不阻塞**）" | **决策：不改文件名**。Kotlin 不要求文件名与顶层声明同名；重命名属无关重构，按 `AGENTS.md` §11 应剥离出本特性。同时实测确认 `CangJieLanguage` 未覆写 `displayName`（`CangJieLanguage.kt:33`）⇒ `getName()` 返回 `"CangJie"`，故 `CangJieDeclarationFileType` **必须**覆写 `getName()` 以避免 `FileTypeRegistry` 重名 |

### 8.3 新增缺口（v1 完全未覆盖，v2 补入）

| # | 缺口 | 处置 |
|---|---|---|
| N1 | `CfirMemberBodyDeclarationChecker`（`memberDeclarationCheckers`）会把 `.cj.d` 的每个无体成员报错；官方在 parser 层就允许，本仓需显式豁免 | 列入 4.4.5 A 表，并进入 7.4 清单 |
| N2 | `isImplicitAbstractClassLikeProperty`（LightTree）在 v1 中缺失，易被误加护栏 | 4.5.2 明确"保持不动"；6.2 增加互斥断言 |
| N3 | `.cj.d` 若物理位于源码根，**仍会被 Stub 索引**（v1 只说了不建 source root，漏了索引侧） | 4.9.3 新增；要求加 `*.cj.d` exclude + 6.6 验收项 |
| N4 | `CfirMemberDeclaration` 是 sealed class，v1 未记录 ⇒ 实现者无法判断顶层声明是否会被 `publishAnnotationInfo` 覆盖 | 4.6.5 补充继承链与"顶层声明均属其后代"的结论 |
| N5 | `CangJieParsingBundle` 中唯一与初始化相关的键 `field.requires.type.or.initializer` 在声明模式下是否豁免？ | 4.2.6 明确**不豁免**，并给出理由（官方 P2/P3 未覆盖该语义） |

### 8.4 开放问题处置（6 条，v1 为 5 条且全部悬置）

| # | 问题 | v2 决策 |
|---|---|---|
| OP1 | `CjFile.getFileType()` | **本次改**（翻转） |
| OP2 | `.cj.d` 是否进 source roots | 不进；**补索引侧排除** |
| OP3 | 收集器重复代码收敛 | 不收敛（独立 Gradle 构建的客观障碍）；改"同步注释 + 一致性校验 + 技术债登记" |
| OP4 | `AbstractFrontendPipeline` 补齐 | 不补（已复核无子类无调用方） |
| OP5 | `derivedCjdPath` UI 可见 | 不进入本次；已给出"无前置依赖"的可执行路径 |
| OP6 | 类型别名导致匹配失败是否修复 | 不修复（与官方一致）；从"未提及"升为**显式已知限制**并固化基线用例 |

### 8.5 v2 明确不改动的项（防止实现者"顺手扩大范围"）

- `getFileType()` 之外的 `psi` 侧既有 FileType 装配：`CangJieFileType.INSTANCE` 的其余 5 处引用**一律不动**。
- `isDefaultInterfaceMember` / LightTree 的对应函数：**不动**（C5）。
- `hasSyntaxBody`（`LightTreeRawCfirDeclarationBuilder.kt:3323-3324`）与 lazy body 防御注释：**不动**（与护栏 3 正交）。
- `parseSynchronized` / `parseForeign` / `parseMacro` 宏体的缺块诊断：**不动**（4.2.6 排除清单）。
- `parsing.error.field.requires.type.or.initializer`（`:2297`）：**不动**（N5）。
- `CjoPackageWriter`：**不动**（D9，`.cjo` 由外部 `cjc` 产出）。
- `AbstractFrontendPipeline` 与其 pipeline 目录：**不动**（OP4）。
- `external/` 下一切内容：**只读取证，不修改**。
- `build/` 下一切内容：**产物，不作为改动目标，也不作为设计依据**（E5 教训）。

---

## 9. v2 二次修正（自查发现的自身缺陷）

> 第 8 章记录的是"v2 相对 v1"的修订。**本章记录的是 v2 初稿自身在复查中被发现并已修正的缺陷** ——
> 它们不是 v1 的问题，而是 v2 写作过程中引入或未覆盖的。保留本章的目的是让评审者知道哪些结论经过了二次验证。

### 9.1 修正 v2 初稿自身的错误（3 条）

| # | v2 初稿的错误 | 修正后结论 | 影响章节 |
|---|---|---|---|
| **V2-1** | 把**所有**产生 `function.body.expected` / `expecting.symbol "{"` 的点（12 个）统一列为"P1，全部豁免" | **错误且危险**。逐点读代码后必须分三组：<br>**A 组 7 个**（合法无体声明）→ 豁免：`:2838` `:2879` `:3777` `:3876` `:4137` `:4145` `:4157`；<br>**B 组 5 个**（"关键字后紧跟 `}`"的语法错误恢复路径）→ **禁止豁免**：`:3595` `:3642` `:3755` `:3815` `:4089`；<br>**C 组 4 个**（块/表达式/容器）→ 禁止豁免：`:635` `:3994` `:4021` `:4124`。<br>豁免 B 组会让 `.cj.d` **静默吞掉 `class C { func }` 这类真语法错误**，偏离官方语义。判定必须**看 `if (at(RBRACE))` 的位置**（关键字 advance 之后 = B 组）——**两组用同一个诊断消息，靠消息文本无法区分** | 4.2.6 |
| **V2-2** | 4.2.5 建议 `parseFile(parseContext: ParsingContext = ParsingContext.DEFAULT)`（改公共签名） | 改为**新增 `parseDeclarationFile()` 入口**。理由：本仓既有 `parseOnlyAnnotationFile()`（`:1280-1297`）就是"no-arg + 函数体内硬编码 `with(ParsingContext.ANNOTATION_ONLY)`"的**同构先例** ⇒ 本仓约定是"一个文件级入口 = 一个硬编码上下文"。改签名反而破坏一致性、触碰所有调用点。配套提取 `parseFileBody()` 消除重复 | 4.2.5 |
| **V2-3** | 4.6.3 写"建议优先复用 LightTree（`CangJieLightParser.parse(builder)`）"，未发现它**没有 ParsingContext 入口** | 实测 `parseWith`（`:63-79`）直接 `cjParsing.parseAction()`，而 `parseFile()` 内部硬编码 `DEFAULT` ⇒ **LightTree 当前拿不到声明模式**。后果：`reportErrors`（`:91-109`）扫到的 `ERROR_ELEMENT` 会让 `FUNC`/`PROPERTY` 节点结构不完整 ⇒ sidecar 签名键错 ⇒ **注解全部静默匹配失败**。落点：新增 `CangJieLightParser.parseDeclaration(...)`，与既有 `parse` / `parseAnnotationOnly` 构成三入口族 | 4.6.3 |

### 9.2 v1 与 v2 初稿**共同**遗漏的缺口（已补入）

| # | 缺口 | 补入位置 |
|---|---|---|
| **V2-4** | **跨仓发布边界**：`intellij-ide` 是独立构建，通过 `libs.cangjie{Common,Psi,...}ForIde`（`version.ref = "cangjie"` = **1.1.1**）消费主仓 11 个发布工件。`psi`/`common` 里新增的 `CangJieDeclarationFileType` / `CjSourceKind` 类**必须先发布到 `../build/repo`**，IDE 侧才可能编译/注册。且因版本号固定，联调**必须 `--refresh-dependencies`**，否则会出现"改了、发布了、却仍报找不到类"的假故障。P5 的真实依赖是「P0+P1+P4 + 主仓发布」，不是 v1/v2 写的"P1, P4" | 新增 **4.10**；P5 依赖列 |
| **V2-5** | **测试落点与基类约定**：v1/v2 只写"测什么"。实测约定为：P1 → `psi/test/org/cangnova/cangjie/psi/`（**包名是 `psi` 不是 `parsing`**，先例 `DoubleColonParsingTest` 等）；P2 → `psi2cfir` / `light-tree2cfir` 的 `testFixtures`，且两者**成对共享**（后者 `testFixturesApi(testFixtures(psi2cfir))`），既有 `LazyBodiesByAst/ByStub` 双变体 ⇒ 声明模式护栏测试**必须同样走 AST/Stub 双变体**，并经由 `TestGeneratorForPsi2Cfir.kt` 生成而非手写；P4 → `cfir/cfir-serialization/test/.../cjd/`，端到端参照 `CjoSdkDeserializationIntegrationTest` | 新增 **6.0** |
| **V2-6** | 4.9.4"内联实现 `derivedCjdPath`"缺少依据：实测 `project-model` 的依赖只有 `foundation/toolchain/ux` + `libs.cangjieCommonForIde/PsiForIde` ⇒ **引不到 `cfir-serialization` 的 `CjdSidecarLocator`**，内联是**依赖边界所迫**而非风格选择；同时它**可以**引用 `common` 的 `CjSourceKind.DECLARATION_SUFFIX` ⇒ 后缀字面量单一真源仍然成立（只要不写 `".cj.d"` 字面量） | 4.9.4 |
| **V2-7** | 4.8.1 对 `analysis/decompiled` 复用 `cfir-serialization` 只写"实施建议"。实测 `decompiler-to-stubs/build.gradle.kts` **已经**有 `implementation(project(":cfir:cfir-serialization"))` ⇒ 依赖已存在，无需新增 | 4.8.1 |

### 9.3 本章之后的剩余未决项（诚实清单）

以下项**不属于文档缺陷**，但完成后才能宣称特性可用；它们需要真实运行环境或额外产出，故不在这份设计文档的闭环范围内：

| # | 未决项 | 为什么不能靠文档解决 | 何时能确认 |
|---|---|---|---|
| R1 | 新增 `-d` 后生成产物的**实际 diff**（应为"只多出 `compileCjd` 一个属性"） | 需要真实跑一次 `:compiler:frontend-arguments-generator` 生成任务；文档只能给出预期 | P0 施工时 |
| R2 | 6.5 端到端测试所需的 **`.cjo` + `.cj.d` fixture 实际产物** | 需要外部 `cjc` 编译出配对的 `.cjo`/`.cj.d`（或手工构造）；本仓不自产 `.cjo`（D9） | P4 施工时 |
| R3 | `--refresh-dependencies` 是否真的能消除同版本缓存（4.10.4 的断言） | 依赖 Gradle 版本与缓存实现的行为，需实测一次发布-消费往返 | P5 开工前（建议先做一次空发布往返验证） |
| R4 | `parseMacro` 的"语法注释写 `functionBody?` 但实现要求必有体"这一矛盾 | 是**本仓既有的独立语义问题**，与 `.cj.d` 无关；已在 4.2.6 C 组登记，不属本特性范围 | 独立课题 |
| R5 | `deveco` 侧的跟随（收集器、SDK 布局 `collectCjdFiles()`） | 4.9.7 已定"`intellij-ide` 先落地、`deveco` 跟随"；`deveco` 同样在跨仓边界外（4.10.5） | 后续迭代 |

---

## 10. 框架正确性评估：哪些地方必须破坏性改动

> **本章的立场**：第 3–4 章给出的落点方案，本质是**在既有框架上逐点加条件**。
> 这类改动有一个共同特征：**每新增一个"同类实体"，都要重新人工枚举一遍"这里要不要改"**——
> 这恰恰是"框架不正确"的定义。正确的框架应当让**新增同类实体时默认就是对的**。
>
> 本章先给判据，再逐项评估，最后给出**建议的破坏性改动**及其破坏半径。
> 若本章结论被采纳，将**替换**第 4 章中对应的落点方案（凡涉替换处已注明）。

### 10.1 判据：怎么判断"框架是否正确"

用这 5 条自检，任何一条不满足，就说明该处是"纯增量在掩盖框架缺陷"：

| # | 判据 | 含义 |
|---|---|---|
| J1 | **新增同类实体时，是否需要重新枚举判断点？** | 若"再加一个无体 owner（如 `~init`、`operator func`）"时需要重新想"它要豁免吗"，则框架未表达该语义 |
| J2 | **同一事实是否有多个来源？** | 真源分裂 ⇒ 迟早不一致 |
| J3 | **局部作用域是否会意外清除全局模式？** | 全局属性借用局部机制承载 ⇒ 随时可能被覆盖 |
| J4 | **新增一个同类实现者（新 checker / 新解析器）的作者，是否需要知道 `.cj.d` 的存在？** | 若需要，说明该语义没有被框架吸收，而是在每个实现者身上重复 |
| J5 | **两条同类路径（PSI / LightTree、intellij-ide / deveco）是否共享同一机制？** | 不共享 ⇒ 每次变更都要"同步两处"，必然漂移 |

### 10.2 评估结果总览

| # | 现状（第 3–4 章的纯增量方案） | 违反 | 建议的破坏性改动 | 破坏半径 | 收益 |
|---|---|---|---|---|---|
| **F1** | 16 个解析诊断点各加 `if (!parseContext.isDeclarationFile)`，且需人工区分 A/B/C 三组 | J1 J4 | **新增统一的 `reportMissingBody()` 上报入口**（内含抑制）；**修正 B 组 5 处的诊断语义** | `CangJieParsing.kt` 内 7 个调用点 + 5 个诊断文案 | 抑制点 **7 → 1**；B 组**不可能**被误豁免；未来新增体 owner 自动正确 |
| **F2** | 第 3 个文件级入口 `parseDeclarationFile()`，模式硬编码在函数体 | J3 J5 | `CangJieParsing` **构造时持有 `sourceKind`**，统一为 `parseFile()` 内部派生模式 | 2 个工厂方法 + 3 个入口 | 模式**只有一个来源**；PSI/LightTree 共享同一判定 |
| **F3** | 把"是否声明文件"塞进 `ParsingContext` | J3 | **不上移、也不塞进去**：模式作为解析器实例的不可变属性（见 F2） | 同 F2 | 消除"局部 `with` 覆盖文件级模式"的隐患；顺带清理 11 个死预设 |
| **F4** | `CjSourceKind` / `CjFile.isDeclarationFile` / `ParsingContext.isDeclarationFile` / `compileCjd` 四处表达同一事实 | J2 | **`CjSourceKind` 为唯一真源**；`CjFile` 直接持有 `sourceKind`，`isDeclarationFile` 退化为派生属性 | **仅 3 处**（见 10.3-F4） | 真源唯一；Boolean 换枚举，为未来的第三种源留出空间 |
| **F5** | 12 个 checker 各自 early-return `configuration.compileCjd` | J1 J4 | 给 `CfirChecker` **加一个实现完备性标记**（默认 `false` = 保留），在分派处统一过滤 | 1 个接口 + 12 个 checker 各 1 行 | 豁免点 **12 → 1**；新增 checker **默认正确**（作者不需要知道 `.cj.d`） |
| **F6** | `project-model` 内联复制 `.cjo → .cj.d` 路径推导 | J5 | 把**纯路径函数**下沉到零依赖的 `common`，两边共用 | 1 个小函数迁移 | 复制消失；后缀常量单一真源 |
| **F7** | `CangJieParser` 与 `CangJieLightParser` 各自新增模式入口 | J5 | 模式判定收敛为**一处**（由文件推导），两个解析器都从该处取 | 2 个 object | 两条解析路径不再各自演化 |

### 10.3 逐项详述

#### F1：`reportMissingBody()` —— 让"体缺失"成为一个概念（**最重要的框架修正**）

**现状（纯增量）**：第 4.2.6 把 16 个上报点分成 A/B/C 三组，其中 A 组 7 点加抑制。
为什么需要这么费劲？因为**"缺体"在本仓解析器里不是一个概念，只是 12 处重复的错误上报**。

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
  这是本方案唯一的"外部可见行为变化"，需在评审时确认（见 10.4 的风险控制）。
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

1. **`ParsingContext` 的"局部细粒度开关"定位是名义上的**。它有 20 个字段、14 个预设，
   实际职责只有一件：**给一个文件级入口选一种解析模式**。把 `.cj.d` 再塞进去，是
   用一个已经过度设计、且实际只当"模式枚举"用的结构去承载第三种模式。
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
- **顺带清理（破坏性但正确）**：11 个死预设应当删除。保留死代码会让下一个实现者误以为
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
- **破坏半径**：3 处构造点 + `createFile` 1 处。
- **对第 4 章的替换**：采纳后 3.3-D8 里"CjSourceKind 用带默认实现的接口属性引入（避免破坏 4 个实现类）"
  这条理由**不再成立**——既然只有 3 个构造点，就该直接改，而不是为"避免破坏"引入一个过渡契约
  （`CjSourceKindCarrier`）。**过渡性抽象一旦为了"不破坏"而存在，就会长期留在代码里。**

#### F5：`CfirChecker` 缺少"实现完备性"分类维度

**现状**：第 4.4.5 A 表列了 12 个需要 early-return 的 checker。我当时的结论是
"分组注册粒度太粗，所以逐 checker early-return"——**这是回避问题**。
真正的缺陷是：**checker 体系只有一个分类维度（声明种类），而"实现完备性检查 vs 接口/类型检查"是另一个正交维度，框架没有表达它。**

**实证**：`DeclarationCheckers` 是 `abstract class`（`cfir/checkers/src/.../declaration/DeclarationCheckers.kt:13`），
槽位全是"按声明种类"分的（basic / callable / function / classLike / property / constructor / extend …），
注册是**集合粒度**（`useCheckers(CommonDeclarationCheckers)`，`CheckersContainers.kt`）。
⇒ 想按"实现完备性"关一批 checker，粒度对不上，只能下沉到每个 checker 内部。

**破坏性改动**：

```kotlin
interface CfirChecker {
    /**
     * 该 checker 是否检查"实现的完备性"（依赖函数体 / 初始化器 / 访问器实现的存在性）。
     * 声明模式（`.cj.d`）下这类检查整体跳过——对齐官方 Sema 的 6 类豁免。
     * 默认 false：新增 checker 默认参与全部模式。
     */
    val requiresImplementation: Boolean get() = false
}
```
分派处统一过滤：`if (configuration.compileCjd && checker.requiresImplementation) return`。

- **收益（J1 + J4）**：豁免点 12 → 1；**新增 checker 的作者不必知道 `.cj.d` 的存在**，
  只需在写"依赖实现的检查"时声明一次标记，默认行为就是正确的。
- **破坏半径**：1 个接口默认属性 + 12 个 checker 各 1 行 + 分派处 1 处。
- **对第 4 章的替换**：采纳后 4.4.5 的 A 表从"12 处逐点 early-return 清单"变为
  "12 个 checker 打标记"——施工量相近，但**语义层级不同**：前者把知识散在 12 个实现里，后者把它固化为框架能力。

#### F6：`common` 才是路径工具的归宿

`project-model` 引不到 `cfir-serialization`（4.9.4 已证），于是内联复制。
但 `.cjo → .cj.d` 的推导是**纯字符串运算**，不依赖任何 CFIR 类型 ⇒ 归宿应是 `common`（零依赖、双方都已依赖）。
把 `CjdSidecarLocator.deriveCjdPath` 拆成"纯路径部分（下沉 `common`）"+"可读性探测部分（留在 `cfir-serialization`）"，
两边各取所需（J5 满足）。

#### F7：两条解析路径的模式判定必须同源

`CangJieParser`（PSI）与 `CangJieLightParser`（LightTree）今天各自决定"用哪种模式"。
F2 落地后，两者都只需从**文件**取 `sourceKind` 并传给解析器 ⇒ 判定同源（J5 满足），
`CangJieLightParser` 的阻塞（V2-3）也随之消解。

### 10.4 破坏性改动的排序与风险控制

按"收益 / 风险"排序，建议按此顺序推进：

| 序 | 改动 | 是否阻断功能 | 外部可见行为变化 | 建议 |
|---|---|---|---|---|
| 1 | **F4**（`CjFile` 持有 `sourceKind`） | 是（F2 依赖它） | 无 | 先做，破坏半径最小（3 处） |
| 2 | **F2/F3**（模式上移到解析器实例） | 是 | 无 | 紧随 F4；顺带删 11 个死预设可**独立提交** |
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
  增量式新增能力，不存在"框架错配"，保持增量即可（见 10.5）。

### 10.5 明确**不**建议破坏的地方（防止过度重构）

| 项 | 为什么保持增量 |
|---|---|
| `CangJieDeclarationFileType` 新增 FileType（D1） | 这是**新增能力**，不是"在旧结构里塞新语义"。IDE 的 FileType 扩展点就是为这种场景设计的 |
| `CjoPackageWriter` / `.cjo` 格式 | 不动（D9，`.cjo` 由外部 `cjc` 产出），无框架问题 |
| `CfirDeclDeserializer` 的 sidecar 合并点 | 是**新增一条数据来源**，不改变既有语义；合并点位置由既有代码顺序决定，属正确适配 |
| sidecar 的 `DeclarationMatchKey` | 是对官方不确定匹配的**有意严格化**（DIFF-2），属新增实现，非框架问题 |
| `AbstractFrontendPipeline` | 既有骨架未接通，与本特性正交（OP4），不在此处补 |
| `deveco` / `intellij-ide` 的两份同名收集器 | 根因是"两个独立构建"（OP3），修它需要构建基础设施改造，**收益（12 行）远小于风险**；F6 的"下沉 common"不适用于它（那是源码级复用，受构建边界限制） |

### 10.6 采纳本章后的净效果

| 指标 | 第 3–4 章方案（纯增量） | 采纳本章后 |
|---|---|---|
| 需要逐点人工判断的解析抑制点 | 16（分 A/B/C 三组） | **1**（`reportMissingBody`） |
| 需要逐点 early-return 的 checker | 12 | **1**（分派处过滤） |
| "是否声明文件"的事实来源 | 4 处 | **1 处**（`CjSourceKind`） |
| 文件级解析模式的载体 | `ParsingContext` 字段（可被局部覆盖） | 解析器实例不可变属性 |
| 新增同类实体时的默认行为 | 需要人工枚举判断 | **默认正确**（J1 满足） |
| 两条解析路径的模式判定 | 各自实现 | 同源 |
| 死代码 | 保留 11 个未使用的 `ParsingContext` 预设 | 清理 |