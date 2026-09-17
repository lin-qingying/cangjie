# `.cj.d` 声明文件支持设计

> 状态：设计稿（待评审）
> 适用范围：主仓库（`common` / `psi` / `compiler` / `cfir` / `analysis` / `lsp`）+ `intellij-ide`
> 证据基线：`external/cangjie_compiler`（官方实现镜像，只读） + 本仓当前工作树

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
| **隐式 abstract 推断** | `isImplicitAbstractClassLikeMember`：`is CjNamedFunction -> !declaration.hasBody()` — **声明模式会被误判** | `cfir/raw-cfir/psi2cfir/src/.../PsiRawCfirBuilder.kt:3887`；LightTree 对应 `LightTreeRawCfirDeclarationBuilder.kt:3286` |
| 源收集（LightTree） | `when (virtualFile.extension) { "java" -> false; CangJieFileType.EXTENSION -> true; else -> fileType == CangJieFileType.INSTANCE }` | `compiler/frontend/src/.../sources/GroupedCjSources.kt:114-138` |
| 源收集（PSI） | `virtualFile.extension == CangJieFileType.EXTENSION \|\| fileType == CangJieFileType.INSTANCE` | `compiler/frontend/src/.../environment/coreEnvironmentUtils.kt:57-74` |
| 配置项 | Key + 扩展属性两段式（`CompilerConfigurationKey` + `CommonConfigurationKeys`） | `compiler/config/src/.../CommonConfigurationKeys.kt:58,180-184` |
| CLI 参数 | DSL 表驱动（`compilerArguments.kt`），生成 `CommonCompilerArguments` | `compiler/arguments/src/.../description/compilerArguments.kt:10-46` |
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
> 复用现成的 `psi/src/org/cangnova/cangjie/lang/declarations/` 包——该包当前只有 `.cjo` 的 `CangJieBuiltInFileType`（文件名为历史遗留的 `CangJieDeclarationsFileType.kt`）与空壳的 `CjDeclarationsFile.kt`。建议本次一并把 `CangJieBuiltInFileType` 迁到 `CangJieBuiltInFileType.kt`，让包内命名回归自洽（可选，不阻塞）。

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
> **建议**：本次**不改** `getFileType()`，而是在 4.2.4 的判定入口处统一使用 `isDeclarationFile` / `sourceKind`，把 `getFileType()` 的重构列为独立的后续项（缩小本次改动面）。

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
            cjParsing.parseFile(ParsingContext.DECLARATION_FILE)

        else ->
            cjParsing.parseFile()
    }

    if (psiFile is CjFile) psiFile.parsedLanguageModuleName = cjParsing.languageModuleName
    return psiBuilder.treeBuilt
}
```

> **关键点**：这里能直接拿到 `psiFile`，是因为 `CjFileElementType.doParseContents` 传的是 `psi.containingFile`（`CjFileElementType.kt:97`）。因此**不需要**为 `.cj.d` 新建 Language 或 ParserDefinition（D1）。

#### 4.2.5 `CangJieParsing.parseFile` 接收 context

文件：`psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt`（修改，约 1310 行）

```kotlin
@JvmOverloads
fun parseFile(parseContext: ParsingContext = ParsingContext.DEFAULT) {
    with(parseContext) {
        val fileMarker = mark()
        parsePreamble()
        while (!eof()) { parseTopLevelDeclaration() }
        checkUnclosedBlockComment()
        fileMarker.done(CJ_FILE)
    }
}
```

#### 4.2.6 六个抑制点的本仓落位

| 官方点 | 本仓对应位置 | 改法 |
|---|---|---|
| P1 `DiagMissingBody` | `CangJieParsing.kt:3876`（`parseFunction`）、`:4145`（`parseInitFunctionBody`）、`:4157`（`parseFunctionBody`）、`:3555`/`:3595`/`:3642`（main / primary init / init rest）、`:2821`/`:2860`（属性访问器） | 上报前判断 `parseContext.isDeclarationFile`，为真则跳过 `error(...)` |
| P2 const 缺初始化 | `psi` 中对应的 const 变量校验点 | 同上 |
| P3 顶层变量缺初始化 | `psi` 中对应的顶层变量校验点 | 同上 |
| P4 缺属性体 | `CangJieParsing.kt:2778-2782` | **只抑制 `error(...)` 两行**，保留 `classdetector` 分支结构（R4） |
| P5 `HAS_BROKEN` | `psi` 中无 `HAS_BROKEN` 概念，**不适用** | — |
| P6 `CheckFuncBody` | `CangJieParsing.kt:3867-3877` | 豁免条件追加 `!parseContext.isDeclarationFile` |

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
> **评审关注点**：若 `.cj.d` 被放入源码根，Stub 索引会与 `.cjo` 产生同名符号。缓解措施见 4.9.2——`.cj.d` 不进入声明提供者聚合，索引仅服务自身文件。

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
    description = "Compile declaration file(s) (.cj.d)".asReleaseDependent()
    argumentType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
    valueType = BooleanType(defaultValue = ReleaseDependent(false), isNullable = ReleaseDependent(false))
    lifecycle(CangJieReleaseVersion.V_1_0_5)
}
```

生成产物 `compiler/frontend/gen/org/cangnova/cangjie/frontend/arguments/CommonCompilerArguments.kt` 会得到 `var d: Boolean`。**该属性名与官方 `-d` 对齐，但可读性差**，建议在 DSL 里补 `compilerName = "compileCjd"`（以 `CangJieCompilerArgumentBuilder` 的 `compilerName` 支持为准，见 `CangJieCompilerArgument.kt:98`）。

> **必须同步处理的历史债**：`compilerArguments.kt` 与生成的 `CommonCompilerArguments.kt` **已存在漂移**——`gen` 里有 `useFir`，DSL 里没有对应声明（`compilerArguments.kt` 仅 4 个参数）。本次改动前必须先跑一次生成任务，确认漂移被消除或明确记录，否则新 flag 的生成结果不可信。
> 生成任务接线见 `compiler/frontend/build.gradle.kts:20-31`（`:compiler:frontend-arguments-generator`）。

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

#### 4.4.5 声明模式下的 Sema 豁免

对齐 R6。本仓的 Sema 豁免落点在 `cfir/checkers` 的分组注册上：

```kotlin
// cfir/entrypoint/src/.../checkers/CheckersContainers.kt 概念示意
fun CfirSessionConfigurator.registerDeclarationModeCheckers() {
    useCheckers(CommonDeclarationCheckers)      // 保留：类型、可见性、继承、override、注解参数
    useCheckers(CommonTypeCheckers)
    useCheckers(CommonExpressionCheckers)
    // 不注册：实现性检查（函数体缺失、构造初始化、getter/setter 实现、main 入口、未实现接口成员）
}
```

需要逐项核对 `CommonDeclarationCheckers`（`cfir/checkers/src/.../CommonDeclarationCheckers.kt:30`）各槽位，标出：

| 槽位 | 声明模式 |
|---|---|
| `basicDeclarationCheckers` | 保留 |
| `callableDeclarationCheckers` / `functionCheckers` | 保留（签名/ABI 相关） |
| `mainFunctionCheckers` | **移除**（对齐 R6：跳过 main 入口检查） |
| `propertyCheckers` / `propertyAccessorCheckers` | 部分移除（对齐 `DeclAttributeChecker.cpp:334`：不要求 getter/setter 同时存在） |
| `constructorCheckers` | 部分移除（对齐 `InitializationChecker`：不做初始化完整性检查） |
| `classLikeCheckers` / `extendCheckers` | 部分移除（对齐 `StructInheritanceChecker.cpp:839`：不报未实现接口成员） |
| `typeParameterCheckers` | 保留（泛型约束检查） |
| `valueParameterCheckers` / `fieldVariableCheckers` | 保留 |
| `fileCheckers`（含 `CfirImportsChecker`） | 保留（import 检查） |

**APILevel / SysCap 检查**：保留（`CfirApiLevelRefHigherChecker` 在 `CommonExpressionCheckers.qualifiedAccessCheckers` 中）。

> **实施要求**：上表的每一项都要在实现时有明确的代码依据（本仓的哪个 checker）。
> 官方是按 `opts.compileCjd` 在**单个 checker 内部**做 early-return；本仓若按"分组注册"裁剪，粒度更粗。**建议采用与官方同构的细粒度方案**：在需要的 checker 内部读 `configuration.compileCjd` 做 early-return。这样与官方一一对应，可审计性更强。

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

**护栏 1 —— 隐式 abstract 推断**

文件：
- `cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt:3887`（`isImplicitAbstractClassLikeMember`）
- `cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt:3286`（`isImplicitAbstractClassLikeFunction`）

```kotlin
// 现状
is CjNamedFunction -> !declaration.hasBody()

// 改为
is CjNamedFunction -> !isDeclarationFile && !declaration.hasBody()
```

其中 `isDeclarationFile` 的取值路径：
- PSI 路径：`builder 所在会话的当前文件` → `CjFile.isDeclarationFile`。具体做法是在 builder 构造/`buildCfirFile(file: CjFile)` 时记录 `file.sourceKind`，并在 `isImplicitAbstractClassLikeMember` 里读取。
- LightTree 路径：`LightTree2Cfir.buildCfirFileWithSurfaces(lightTree, sourceFile, linesMapping)` 已接收 `CjSourceFile`，直接读 `sourceFile.sourceKind`（4.1.2 的默认实现会正确处理）。

**护栏 2 —— interface 默认成员判定**

`isDefaultInterfaceMember`（PSI）与 `isDefaultInterfaceFunction`（LightTree）依赖 `hasBody()`。
在声明模式下，`.cj.d` 里的 interface 成员无 body **也是合法的**（对齐 P6），因此**判定逻辑保持原样**（无 body 即非 default），不需要 `!isDeclarationFile` 护栏。

> 这一条需要复核：`.cj` 的 interface 无体成员通过 `isInterfaceMethod` 豁免；`.cj.d` 通过 `parseDeclFile` 豁免。两者都无 body，判定结果都是"非 default"，所以行为一致。**不需要改。**

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

3. 支持两种输入源（与 4.4 的源收集一致）：
   - 磁盘上的 `.cj.d` → 用 `psi` 的轻量解析（`CangJieLightParser` / `LightTree2Cfir` 的上游 parse 能力）或全 PSI 解析。
   - 内存中的 `.cj.d`（IDE 未保存的编辑器内容）→ 走 PSI。

   > **建议**：优先复用 **LightTree**（`CangJieLightParser.parse(builder)`），因为 sidecar 只需要声明骨架 + 注解，不需要 body。

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

现有关键序列（`:131-167`）：

```kotlin
fun deserializeDecl(declIndex: Int): CfirDeclaration {
    // ... declCache 命中直接返回
    // convertDecl(decl) 产出 result
    result.serializedInteropFacts = serializedInteropFacts(decl)
    result.annotationInfo = serializedAnnotationInfo(decl)
    appendSerializedAnnotationMarker(result)
    if (result is CfirMemberDeclaration) { ... result.publishAnnotationInfo() ... }   // ← :154-165
    // ...
}
```

**改动位置**：在 `appendSerializedAnnotationMarker(result)` **之前**插入 sidecar 合并：

```kotlin
// 1) 读取 .cjo 侧注解（既有）
result.serializedInteropFacts = serializedInteropFacts(decl)
result.annotationInfo = serializedAnnotationInfo(decl)

// 2) sidecar 合并（新增）
cjdSidecarIndex?.mergeInto(result)          // 追加 .cj.d 侧注解，见下

// 3) 发布注解快照（既有，必须在合并之后）
appendSerializedAnnotationMarker(result)
if (result is CfirMemberDeclaration) { ... result.publishAnnotationInfo() ... }
```

> **顺序是关键**：`publishAnnotationInfo()` 会发布注解快照供 `CfirDeclarationAvailabilityProvider` 读取（`:157-165`）。合并必须早于发布，否则 APILevel/SysCap 检查看不到 sidecar 注解。

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

> **必须验证**：确认本仓不存在"跨 provider 复用的 `.cjo` 声明对象"。若存在（例如共享的 `declCache`），则需要显式清理——在 `PackageDeserializers` 销毁时把 sidecar 注解从声明上移除。**这一条列为 P4 的验收项。**

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

> **实施建议**：把 sidecar 索引与匹配谓词放在 **`cfir/cfir-serialization`**（已有 `.cjo` 读写基础设施），由 `analysis/decompiled` 依赖引用，而不是复制实现。

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

> **历史旁证**：过期构建产物 `intellij-ide/build/resources/main/META-INF/cangjie-filetypes.xml:49-51` 留有被注释掉的 `cjd` / `CangJieDeclarationsFileType` 注册草稿，说明该方向此前已有规划。本次实现应当**清掉那段注释**，避免继续误导。

#### 4.9.2 解析与文件创建

文件：`psi/src/org/cangnova/cangjie/parsing/CangJieParserDefinition.kt`（修改，`:106-115`）

```kotlin
override fun createFile(viewProvider: FileViewProvider): PsiFile {
    return when (viewProvider.fileType) {
        is CangJieDeclarationFileType -> CjFile(viewProvider, isCompiled = false, isDeclarationFile = true)
        is CangJieFileType -> CjFile(viewProvider, false)
        else -> PsiPlainTextFileImpl(viewProvider)
    }
}
```

> **顺序很关键**：`CangJieDeclarationFileType` 继承自 `CangJieFileType`，`when` 分支必须**把声明类型放在前面**，否则会被 `is CangJieFileType` 提前命中。
> 若不改这里，`.cj.d` 会 fallthrough 到 `PsiPlainTextFileImpl` —— 即"只显示文件类型，得到纯文本 PSI"，正是用户原文指出的风险。

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

`foo.cj.d` 的 `nameSequence` 以 `.cj.d` 结尾，**不以 `.cj` 结尾**（`endsWith(".cj")` 为 false），且 `isFileOfType` 判定的是新类型 → 返回 false。**行为正确，无需改动**。但需要补充一个显式的 `isCangJieDeclarationFileType()` 供需要区分的调用方使用。

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

**注意**：为避免 `project-model` 依赖 `cfir-serialization`，`derivedCjdPath` 的推导逻辑可以放在 `project-model` 内联实现（5 行），或抽到一个零依赖的小工具。**推荐内联**，因为它是纯路径运算。

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

## 5. 实施计划

按"可独立验证、可独立回滚"切分。每阶段完成后立即提交（项目约定：每个功能立即 commit）。

| 阶段 | 内容 | 依赖 | 验收标准 |
|---|---|---|---|
| **P0** | 公共契约：`CjSourceKind`、`CjSourceKindCarrier`、`CompilerConfiguration.compileCjd`、`compilerArguments` 的 `-d` | — | 单元测试覆盖 `CjSourceKind.fromFileName` 的全部边界（`.cj.d`、`a.cj.d`、`.cjd`、`a.CJ.D`、`a.cj.d.txt`）；生成任务跑通且漂移已记录 |
| **P1** | `psi` 声明解析模式：`CangJieDeclarationFileType`、`CjFile.isDeclarationFile`、`ParsingContext.isDeclarationFile`、`CangJieParser.parse` 分支、6 个抑制点 | P0 | 解析测试：`.cj.d` 中无体函数/构造函数/属性访问器无 error 节点；**同一文本在 `.cj` 下必须报错**（双向验证） |
| **P2** | CFIR declaration 护栏：`isImplicitAbstractClassLikeMember` / `isImplicitAbstractClassLikeFunction` 加 `!isDeclarationFile` | P1 | 测试：`.cj.d` 中无体类成员函数**不**被标记 implicit abstract；`.cj` 中同样文本**被**标记（双向验证） |
| **P3** | 源收集与声明模式：`GroupedCjSources`、`coreEnvironmentUtils` 的两级互斥过滤；检查器豁免 | P0 | 收集测试：`compileCjd=false` 只收 `.cj`；`compileCjd=true` 只收 `.cj.d`；两者都**不**收对方 |
| **P4** | **sidecar 合并**：`CjdSidecarLocator`、`CjdSidecarIndex`、`DeclarationMatchKey`、`CfirDeclDeserializer` 合并点、provider 失效 | P0 | 见第 6 章 sidecar 测试矩阵；**必须验证 4.6.6 的"声明对象不被跨 provider 复用"** |
| **P5** | IDE 侧：`cangjie-filetypes.xml`、`CangJieParserDefinition.createFile`、收集器注释与 `deveco` 副本对齐、`CjDependency.Binary.derivedCjdPath` | P1, P4 | 手工验收：打开 `.cj.d` 得到 Cangjie 高亮 + 可导航 + 无"缺函数体"波浪线；`.cj.d` 不出现在声明提供者聚合中 |
| **P6** | `analysis/decompiled` 侧注解注入、`lsp` 接入、`deveco` SDK 布局 | P4, P5 | light declaration / 文档中能看到 `.cj.d` 提供的 `@APILevel` |

**阶段解释**

- **P4 是本设计的核心**，也是风险最高的部分。它独立于 P0-P3 之外，可以并行推进；P1/P2/P3 不依赖 P4。
- **P2 必须紧接 P1**：若 `.cj.d` 能解析出无体成员、但 CFIR 仍把它们当隐式抽象，会得到"能打开但语义全错"的更糟状态。两者必须同批交付。
- **P3 的检查器豁免与官方的粒度对齐**（4.4.5 结尾）：建议在单个 checker 内部按 `configuration.compileCjd` early-return，而不是靠分组注册开关，以便与官方逐项对照审计。

---

## 6. 测试策略

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
| **同一 `.cjo` 被两个 provider 加载** | 验证声明对象不被共享（4.6.6 验收项）；若共享则需显式清理 |

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
| `.cj.d` 中的无体函数 | **无** "Function body expected" 波浪线 |
| `.cj.d` 中的 prop | 高亮/折叠/括号匹配正常 |
| `.cj.d` 内部符号导航 | 可用 |
| `.cj.d` 是否出现在声明提供者聚合 | **否**（D7） |
| `.cj.d` 是否加入 `classesRoots` | **否** |
| `x.cj.d` 与 `x.cjo` 并存时 | 无重复符号告警 |
| 普通项目构建 | 流程不变，不感知 `.cj.d` |

---

## 7. 风险与开放问题

### 7.1 已识别的有意差异（必须评审确认）

| 编号 | 差异 | 理由 | 影响 |
|---|---|---|---|
| **DIFF-1** | 泛型约束 `upperBounds` 比较用 `upperBounds.size()` 作为上界，官方用外层约束数 | 官方写法在 `upperBounds.size() < genericConstraints.size()` 时越界 | 本仓更严格正确；不改变可匹配集合 |
| **DIFF-2** | 用确定性签名键匹配，官方用 `unordered_map + find_if` | 官方匹配顺序不确定；类型别名场景下会漏配 | 本仓确定性更强；可能出现"官方漏配、本仓配上"的情况 |
| **DIFF-3** | `.cj.d` 建 Stub 索引，官方参考实现对声明文件用非 Stub 类型 | 本仓 `.cj.d` 需要完整导航/补全能力 | 需确认不会与 `.cjo` 产生索引级冲突（4.9.3 已通过"不进声明聚合"缓解） |
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

### 7.3 开放问题

1. **`CjFile.getFileType()` 是否应改为返回 `viewProvider.fileType`？**
   - 现状硬编码 `CangJieFileType.INSTANCE`（`CjFile.kt:66`）。
   - 不改的代价：`.cj.d` 的 `CjFile` 自称 `.cj` 类型，文件类型推断/图标可能有偏差。
   - 改的代价：爆炸半径未知，`FileTypeRegistry` 相关判断可能受影响。
   - **建议**：本次不改，列入独立技术债。

2. **`.cj.d` 是否应进入 `CjWorkspaceModelSync` 的 source roots？**
   - 若进入，Stub 索引会把它当普通源码，`.cj.d` 中声明的符号可能与 `.cjo` 同名。
   - 若进入，则 `isCangJieScopeCandidate()` 的"排除"变成了双保险；若不进入，`.cj.d` 在 IDE 中的 PSI 仍可用（用户手工打开）。
   - **建议**：**不进入**，与 D7 一致。

3. **`deveco` 与 `intellij-ide` 中的 `CaIdeScopeCangJieFileCollector` 重复代码如何收敛？**
   - 当前有两份同名实现。
   - **建议**：本次先把注释与排除策略在两处对齐，收敛为共享模块作为后续项。

4. **`compiler/frontend` 的 `AbstractFrontendPipeline` 是否要顺带补齐？**
   - 当前无子类、无调用方。
   - **建议**：**不补**。`.cj.d` 支持不依赖它（D9）。补齐泛用流水线是独立课题，混入本特性会显著扩大改动面与评审成本。

5. **`.cjo` 的 `DerivedCjdPath` 是否要在 IDE UI 上可见？**
   - 例如依赖树中展示"该 cjo 附带声明文件"。
   - **建议**：后续项，不影响本次功能正确性。

### 7.4 交付前必须复核的清单

- [ ] 每一条 R1-R14 都有对应的可执行测试用例
- [ ] 每一个 `DIFF-*` 都在评审记录中确认
- [ ] 每个抑制点都有 `.cj` 反向用例
- [ ] `.cj.d` 未出现在任何源码集/声明集/classesRoots 中（除声明模式编译）
- [ ] `deveco` 与 `intellij-ide` 的收集器策略一致
- [ ] `compilerArguments.kt` 与生成产物无漂移
- [ ] `external/` 下无任何改动

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
| `parseFile` / `parseOnlyAnnotationFile` | `psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt:1280-1327` |
| `parseFunction` 缺体豁免 | `psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt:3801-3880` |
| `parseProperty` | `psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt:2759-2786` |
| `parseFunctionBody` | `psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt:4149-4159` |
| `CjFile` | `psi/src/org/cangnova/cangjie/psi/CjFile.kt:47-393` |
| `CjDeclarationWithBody` | `psi/src/org/cangnova/cangjie/psi/CjDeclarationWithBody.kt:32-80` |
| 后缀工具 | `psi/src/org/cangnova/cangjie/utils/virtualFileUtil.kt:34-40` |
| 配置 Key 与扩展属性 | `compiler/config/src/org/cangnova/cangjie/config/CommonConfigurationKeys.kt:57-63, 180-184` |
| CLI 参数 DSL | `compiler/arguments/src/org/cangnova/cangjie/arguments/description/compilerArguments.kt:10-46` |
| 参数生成接线 | `compiler/frontend/build.gradle.kts:20-31` |
| 生成的参数类 | `compiler/frontend/gen/org/cangnova/cangjie/frontend/arguments/CommonCompilerArguments.kt:7-30` |
| 源收集（LightTree） | `compiler/frontend/src/org/cangnova/cangjie/frontend/sources/GroupedCjSources.kt:85-166` |
| 源收集（PSI） | `compiler/frontend/src/org/cangnova/cangjie/frontend/environment/coreEnvironmentUtils.kt:57-126` |
| 前端流水线 | `compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/AbstractFrontendPipeline.kt:12-41` |
| Phase 裁剪机制 | `compiler/phaser/src/org/cangnova/cangjie/phaser/PhaseConfig.kt:10, 51`；`PipelinePhase.kt:68-75` |
| `CfirFunction.body` 可空 | `cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/declarations/CfirFunction.kt:40` |
| `CfirFile` | `cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/declarations/CfirFile.kt:31-60` |
| `CfirDeclaration.replaceAnnotations` | `cfir/cfir-tree/gen/org/cangnova/cangjie/cfir/declarations/CfirDeclaration.kt:25, 37` |
| `BodyBuildingMode` | `cfir/raw-cfir/raw-cfir-common/src/org/cangnova/cangjie/cfir/builder/BodyBuildingMode.kt:9-14` |
| PSI→CFIR 无体 | `cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt:3555-3562` |
| 隐式 abstract（PSI） | `cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt:3887-3902` |
| LightTree 无体 | `cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt:3082` |
| 隐式 abstract（LightTree） | `cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt:3286-3322` |
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
| 过期的 `cjd` 注册草稿 | `intellij-ide/build/resources/main/META-INF/cangjie-filetypes.xml:49-51` |
| 参考实现 `.cj.d` 注册 | `external/cangjie_deveco_plugins/lsp-client/src/main/resources/META-INF/cangjie-lsp.xml:27-32, 43, 48, 64` |
| 参考实现 FileType | `external/cangjie_deveco_plugins/lsp-client/src/main/java/com/huawei/ideacj/filetypes/CangjieDeclarationFile.java:26-61` |
| 参考实现 ParserDefinition | `external/cangjie_deveco_plugins/lsp-client/src/main/java/com/huawei/ideacj/language/CangjieDeclarationParserDefinition.java:67-189` |
| SDK 布局 KDoc 提及 `.cj.d` | `deveco/modules/domain/toolchain/src/main/kotlin/org/cangnova/cangjie/toolchain/api/CangjieSdkLayout.kt:55-68, 492, 535-550` |
| `deveco` 收集器副本 | `deveco/modules/ide/base/src/main/kotlin/org/cangnova/cangjie/ide/base/analysisApiPlatform/CaIdeScopeCangJieFileCollector.kt` |
| **P2** | CFIR declaration 护栏：`isImplicitAbstractClassLikeMember` / `isImplicitAbstractClassLikeFunction` 加 `!isDeclarationFile` | P1 | 测试：`.cj.d` 中无体类成员函数**不**被标记 implicit abstract；`.cj` 中同样文本**被