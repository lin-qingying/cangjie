// RUN_PIPELINE_TILL: FRONTEND
// type alias 规则语义夹具：别名环（`TYPEALIAS_CYCLE`）、别名未使用的类型形参
// （`TYPEALIAS_UNUSED_TYPE_PARAMETERS`，官方是 **warning**）、别名暴露低访问级别类型
// （`TYPEALIAS_EXTERNAL_REFER_INTERNAL`），以及与后两者共现的 `ACCESSIBILITY_ERROR`。
//
// 取证方式：官方 cjc 1.0.5 与 1.1.3 两版实测，静态库模式
//   `cjc <段>.cj -o out.a --output-type=staticlib --diagnostic-format json`；每个 `// FILE:` 段单独编译
//   （官方整包编译时一处 parse 错误会吞掉其余段的语义诊断：CompilerDiagnosticHandler::CanBeEmitted 按
//   DiagCategory 只发第一类，src/Basic/DiagnosticEngine.cpp:764-770；同一 (起点, 文案) 的重复诊断由
//   DiagnosticHandler::HasPrevDiag 去重，:716-723 与 :778-780）。
// 版本门禁：版本指令是**文件级**指令、不能写在 `// FILE:` 段内（LLT 框架会直接报
//   "Directive LANGUAGE_VERSION has Global applicability but it declared in File"），因此带门禁的 7 个段
//   已按版本拆出：`typeAliasRulesV105.cj`（版本 1.0.5：形态 7、形态 8 的 1.0.5 段、形态 25–28）与
//   `typeAliasRulesV110.cj`（版本 1.1.0：形态 8 的 V113 段）。本文件 30 段两版逐段 kind / 文案 /
//   MainHint.Range 完全一致。
//   指令由 tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/config/BaseDiagnosticConfiguration.kt
//   经 LanguageSettingsDirectives.LANGUAGE_VERSION 消费（analysis tests 的配置基类继承该配置）。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**；三个 kind 都是非 refactor kind，
//   官方 `Diagnose(node, kind)`（include/cangjie/Basic/DiagnosticEngine.h:843-857）退化成 `GetBegin()` **1 个字符**。
//   `GetBegin()` = `TypeAliasDecl::begin`，parser 在**消费修饰符之前**记录它（src/Parse/ParseDecl.cpp:1180-1181
//   `ret->begin = lookahead.Begin()` / `ret->keywordPos = lookahead.Begin()`，随后才 `CheckDeclModifiers` 吃修饰符），
//   所以：**无修饰符别名锚 `type` 的 `t`（列 1）**；**带修饰符别名锚第一个修饰符的首字母**（`public` 的 `p`，仍是列 1，
//   `type` 在列 8）。探针实测：`type TARA = TARB` → (1,1)..(1,2)；`public type TARBj = TARBJ` → (3,1)..(3,2)。
//   `sema_accessibility` 是 **refactor** kind，锚 `MakeRangeForDeclIdentifier(outDecl)` = **别名名**（不含类型形参列表）。
// 内联标记写法：由"小于感叹号 + 诊断名 + 大于感叹号 + 锚点文本 + 结束标记"五段构成；头部注释一律用文字描述该序列，
//   不出现字面量（verify_one 会把首个 `// FILE:` 之前的内容当一段解析）。**同一行两个官方诊断同起点**时用嵌套：
//   外层先开、内层后开、**内层先闭合**（LIFO），本文件两处：形态 8 的 V113 段（`TYPEALIAS_CYCLE` + 未用形参）与
//   形态 24（`TYPEALIAS_EXTERNAL_REFER_INTERNAL` + 未用形参），写法是"外层标记、内层标记、首字符、结束、外层结束"。
// **未跑 CFIR**：本文件不依据本项目 CFIR 的实际输出编写，头部只按源码写出 CFIR 名、生产者位置与可预判的锚点差异。
//
// 官方实现（external/cangjie_compiler，工作树 checkout 在 tag v1.0.0）：
//   A. 别名环 → src/Sema/PreCheck.cpp:1140-1152 `TypeAliasCircleCheck`：**导入包直接跳过**
//      （`:1142 if (ctx.curPackage->TestAttr(Attribute::IMPORTED)) return`），对本包 `TYPE_ALIAS_DECL`
//      按 `Sort::posAsc`（声明位置升序）逐个以**空 path** 调 `CheckTypeAliasCycleForOneDecl`。
//      :1187-1202 `CheckTypeAliasCycleForOneDecl`：目标 `tad.checkFlag == VISITING` 即成环，
//      :1191-1192 `diag.Diagnose(tad, DiagKind::sema_typealias_cycle, ConstructDiagInfoForCycle(tad, path).c_str())`，
//      **立即 return**；否则置 VISITING、push 进 path、`GetTypeArgsOfType(tad.type)` 取 RHS 全部类型节点（前序，
//      src/Sema/Utils.cpp:390-427：REF_TYPE / QUALIFIED_TYPE / TUPLE_TYPE / FUNC_TYPE / OPTION_TYPE 各自递归），
//      再 :1199 逐个 `CheckTypeAliasCycleForOneType`（:1154-1172，只对 `REF_TYPE` / `QUALIFIED_TYPE`
//      且 `ref.target` / `target` 是 `TYPE_ALIAS_DECL` 递归），最后 pop、置 VISITED。
//      文案 DiagnosticSema.def:38 `"type cycle detected: '%s'"`，实参来自 PreCheck.cpp:35-52 `ConstructDiagInfoForCycle`：
//      从 path 里第一次出现 `root` 起拼接 `标识符->`，所以报在环上每个别名上的链条各自不同（探针实测：两别名互环
//      `'A->B->A'` 与 `'B->A->B'`；三别名环三条 `'X->Y->Z->X'`、`'Y->Z->X->Y'`、`'Z->X->Y->Z'`）。
//      **RHS 里非别名引用（含泛型类实例）不进 DFS**（`class C<T>{}` + `type D<T> = C<C<T>>` 两版零诊断，
//      官方无法把 C 的实例化展开回 D，见 `HandleAliasForRefType`），故官方没有"别名经泛型类参数成环"这一形态。
//   B. 未使用的类型形参 → src/Sema/TypeCheckDecl.cpp:679-701 `CheckTypeAlias`（调用点 src/Sema/TypeChecker.cpp:1066）：
//      `IS_CHECK_VISITED` 早退；:687 `!tad.type || !Ty::IsTyCorrect(tad.type->ty)` 早退；:688 调 `CheckTypeAliasAccess`；
//      :690-695 `if (tad.generic)` 时用 :660-677 `GetUnusedTysInTypeAlias` 按**声明顺序**列出
//      `tad.generic->typeParameters` 中不出现在 `GetTyArgsRecursive(*tad.type->ty)`（:648-658，对 Ty 的 `typeArgs`
//      **递归**收集）里的类型实参，非空则 :694
//      `diag.Diagnose(tad, DiagKind::typealias_unused_type_parameters, Ty::GetTypesToStr(diffs, ","))`。
//      文案 DiagnosticSema.def:304 `WARNING(typealias_unused_type_parameters, UNUSED, "type arg(s) %s are not used")`
//      —— **warning**（UNUSED 类，JSON note "this warning can be suppressed by setting the compiler option
//      `-Woff unused`"）；实参是类型名列表，形参类型 `GenericsTy::String()`（src/AST/Types.cpp:785-788）输出
//      `"Generics-" + name`，`Ty::GetTypesToStr`（include/cangjie/AST/Types.h:340-351）用**逗号无空格**连接：
//      两版实测单形参 `'type arg(s) Generics-T are not used'`、多形参一条 `'type arg(s) Generics-T,Generics-K are not used'`
//      （**不带尖括号**）。另有两个"偏推导"报告点：src/Sema/TypeCheckType.cpp:198（类型位置引用别名）与
//      src/Sema/TypeCheckGeneric.cpp:325（表达式位置引用别名）——两处只是**擦除**未用实参后继续推导，**不报**本诊断
//      （探针：`type A<K> = Array<K>` + `type B = A<Int64>` 零诊断；`type A<T,K> = Array<T>` + `func f(x: A<Int8,Bool>)`
//      两版都只有声明处一条）。
//   C. 别名暴露低访问级别类型 → src/Sema/TypeCheckDecl.cpp:619-642 `CheckTypeAliasAccess`：`PRIVATE` 别名跳过（:620）；
//      :623-624 `GetTypeArgsOfType(tad.type)` 取 RHS 全部类型节点；:629-630 只处理 `REF_TYPE`（元组 / 函数类型 /
//      `Option<?T>` / `Array<T>` 的**内层**类型节点照样进 vector，但它们自身不是 `REF_TYPE`，靠 :637 `if (!decl) continue`
//      或 :638 `decl->astKind != GENERIC_PARAM_DECL && …` 过滤）；:639 `!IsCompatibleAccessLevel(tadLevel, GetAccessLevel(*decl))`
//      即 `tadLevel > declLevel`（include/cangjie/AST/Utils.h:148-151；
//      `AccessLevel{PRIVATE=0, INTERNAL=1, PROTECTED=2, PUBLIC=3}`，Node.h:2782-2787）时 :640
//      `diag.Diagnose(tad, DiagKind::sema_typealias_external_refer_internal, tadLevelStr, tad.identifier.Val(), GetAccessLevelStr(*decl), decl->identifier.Val())`。
//      - **只比对语法位置上写下的那个声明**（:632 `decl = rt->ref.target`）：探针 p3 b7（`class C<T>{}` + `type B<T> = C<T>`
//        + `public type A = B<Int64>`）两版都报 `'internal' type 'B'`（不报展开后的 C）；探针 p2 a10（`private type P = Int64`
//        + `public type A = P`）报 `'private' type 'P'`。级别串来自 src/AST/Utils.cpp:465-479，文案 DiagnosticSema.def:39
//        `"'%s' type '%s' refers to '%s' type '%s'"`。
//      - 同一别名上**多个**低级别目标只报**一条**（`for` 无 break，靠 HasPrevDiag 同起点同文案去重），报的是
//        `GetTypeArgsOfType` 前序里**第一个**违规目标（探针 p5 e5：`(TAR44A, TAR44B)` 都 private 只报第一个）。
//      - `GENERIC_PARAM_DECL`（别名的类型形参）被 :639 显式排除，所以 `public type A<T> = Array<T>` 合法（探针零诊断）。
//   D. 伴随诊断 `sema_accessibility`（**refactor** kind，include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:23
//      `"'%s' declaration uses %s types"`；带 MainHint 的变体 :22 `sema_accessibility_with_main_hint` 在 `hintDecls`
//      非空时使用）：生产者 src/Sema/Diags.cpp:505-543 `DiagLowerAccessLevelTypesUse`，:512 锚
//      `MakeRangeForDeclIdentifier(outDecl)`（:109-137，普通 Decl → `MakeRange(decl.identifier)`，即**别名名**，不含类型形参
//      列表），带 MainHint `"inferred type"`，note `"type '<Ty>' contains <level> type"`（节点范围）+
//      `"the <level> type is '<Ty>'"`。**与 C 的差别（探针矩阵）**：
//        - C 出现的场景 D 基本都出现（public/internal 别名 → 类；别名 → 别名），**唯一例外**是 `public func f(x: PrivateType)`
//          两版都不报 D（探针 p3 b6），因为类型别名只在**自己的 RHS** 里查级别。
//        - `public type = Array<私有类>` / `= (私有类, Int64)` / `= (Int64) -> 私有类` / `= Option<私有类>`：
//          C 两版都有，**D 只在 1.0.5**（范围锚 RHS 整体，如 (1,13)..(1,27)；1.0.5 按展开类型树递归进所有类型实参，
//          1.1.3 只检查**直接**引用的 Decl 与**泛型**实参）——本文件形态 25–28 用 `// LANGUAGE_VERSION 指令 1.0.5`。
//        - `public type = 泛型别名`（`type B<T> = C<T>` + `public type A = B<Int64>`，C 是 internal 类）：C + D 两版都有，
//          D 的 note 是展开后的 `Class-C<Int64>` 与形参类型 `Class-C<Generics-T>`（形态 19 / 29）。
//        - `public type = 非泛型别名`（`private type P = Int64`）：只有 C，**没有 D**（形态 22 的对照：别名目标是
//          private **类**时 D 才出现，指向内置类型时不出现）。
//
// 两版分歧（3 类，合计 7 个门禁段）：
//   1) 形态 7 `typeAliasUnusedParamWithPropagatedConstraint.cj`（在 `typeAliasRulesV105.cj`，门禁 1.0.5）：RHS 类带 where 约束、
//      约束经 `IgnoreAssumptionForTypeAliasDecls`（PreCheck.cpp:1324-1334）/ `GetAllAssumptions`（TypeCheckDecl.cpp:560-617）
//      传播到别名形参（`GenericsTy::lowerBound`）时，1.0.5 报 `'type arg(s) Generics-K are not used'`、1.1.3 零诊断
//      （探针 p6 g1/g3 另两例同此）。
//   2) 形态 8 的两段：环上别名带**未被使用**的形参时，1.0.5（`typeAliasCycleUnusedParamGate.cj`，门禁 1.0.5）报两条
//      `sema_typealias_cycle` 且**不**报未用形参（`CheckTypeAlias` 在 `!Ty::IsTyCorrect` 处早退）；1.1.3
//      （`typeAliasCycleUnusedParamGateV113.cj`，门禁 1.1.0）在两条环诊断外**各加**一条未用形参 warning。
//      环上形参**被使用**时（形态 3）两版都只有两条环诊断。既有 LLT typealias7.cj 是"形参未被使用"的同一形态。
//   3) 伴随诊断 `ACCESSIBILITY_ERROR`（官方 `sema_accessibility`）在 RHS **内层**类型（`Array<私有类>` / 元组 / 函数类型 /
//      `Option<私有类>`，形态 25–28）**只在 1.0.5 产出**（锚 RHS 整体，如 (4,1)..(4,15)），1.1.3 只报
//      `sema_typealias_external_refer_internal`；四个段都在 `typeAliasRulesV105.cj`（门禁 1.0.5）。
//
// 本项目 CFIR 名（cfir/checkers/checkers-component-generator/src/org/cangnova/cangjie/cfir/checkers/generator/
// diagnostics/CfirDiagnosticsList.kt，生成物 cfir/checkers/gen/.../diagnostics/CfirErrors.kt）：
//   :2650 `TYPEALIAS_CYCLE` by error<PsiElement>(parameter<String>("typeAlias")) ← 官方 `sema_typealias_cycle`
//   :2645 `TYPEALIAS_UNUSED_TYPE_PARAMETERS` by **warning**<PsiElement>(parameter<String>("typeParameters"))
//     ← 官方 `typealias_unused_type_parameters`
//   :1174 `TYPEALIAS_EXTERNAL_REFER_INTERNAL` by error<PsiElement>(Visibility aliasAccessLevel, Name typeAlias,
//     Visibility typeAccessLevel, Name typeName) ← 官方 `sema_typealias_external_refer_internal`
//   :1167 `ACCESSIBILITY_ERROR` by error<PsiElement>(String declarationKind, Visibility visibility)
//     ← 官方 `sema_accessibility` / `sema_accessibility_with_main_hint`
//   （本项目诊断名表内**没有**官方 `sema_redefinition` 的对应名——只有 :1706 `REDEFINITION_ENTRY`；本文件不出现重定义形态。）
// 文案表 cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrorsDefaultMessages.kt：
//   :1365 TYPEALIAS_UNUSED_TYPE_PARAMETERS `"type arg(s) {0} are not used"`、:1366 TYPEALIAS_CYCLE
//   `"type cycle detected: ''{0}''"`、:1368-1371 TYPEALIAS_EXTERNAL_REFER_INTERNAL
//   `"''{0}'' type ''{1}'' refers to ''{2}'' type ''{3}''"` —— 三条与官方逐字一致；
//   :886 ACCESSIBILITY_ERROR `"''{0}'' declaration uses {1} types"`（官方 :23 同形，但**实参含义不同**，见下）。
// 生产者（cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/）：
//   CfirTypeAliasCycleChecker.kt:32（`check` :25-35：只在 `expandedTypeRef` 是带
//     `DiagnosticKind.RecursiveTypealiasExpansion` 的 `CfirErrorTypeRef` 时报，即只在别名展开结果为递归错误时才报）
//   CfirTypeAliasUnusedTypeParameterChecker.kt:46（`check` :26-49：遍历**展开后**类型的全部 `ConeTypeParameterType`，
//     与 `declaration.typeParameters` 求差；:29-33 对 error type / 未解析限定符跳过；:56-64 在循环别名上改读
//     `delegatedTypeRef` / `partiallyResolvedTypeRef` 以保留 RHS 类型实参）
//   CfirTypeAliasAccessChecker.kt:54（`check` :40-60：PRIVATE 跳过；:46 展开类型含 error type 跳过；:48
//     `firstExposureTarget`（:71-112）按前序找**第一个**违规目标、遇到即返回；:77-86 classId 优先取 `abbreviatedType`
//     即**别名自身**，与官方"不穿透别名"一致）
//   ACCESSIBILITY_ERROR 由可访问性检查器族产出（不在三个 typealias checker 内，本文件只记名与文案）。
//
// 可预判锚点差异（**未跑 CFIR**，按源码写出）：
//   1) 官方三个 kind 都 `Diagnose(tad, …)` → `GetBegin()` 一字符（无修饰符时 `t`，有修饰符时修饰符首字母，列均为 1）。
//      CFIR 三个 checker 统一用 `cfir/checkers/.../declaration/CfirDeclarationDiagnosticSources.kt:187-203
//      typeAliasDeclarationHeaderDiagnosticSource()`，范围是 `type` 关键字 → 类型形参列表末尾（无形参则到别名名末尾）：
//      `type A` / `type A<T>` 整段。故本文件按官方写 1 字符标记，CFIR 在这三个 kind 上会给出**更宽**的范围
//      （与 llt/typealias/typealias6.cj、7.cj 等既有夹具里 1 字符锚点标记并存）。
//   2) CFIR 的 TYPEALIAS_EXTERNAL_REFER_INTERNAL 参数 (别名可见级别, 别名名, 目标可见级别, 目标名) 渲染成
//      `'<级别>' type '<别名>' refers to '<级别>' type '<目标>'`，与官方逐字同形；但 ACCESSIBILITY_ERROR 的 CFIR 参数
//      (declarationKind, visibility) 渲染成 `'<别名名>' declaration uses '<级别>' types`，官方是
//      `'<级别>' declaration uses '<级别>' types`（第一个实参是**级别**而非名字）——文案实参层面不一致。
//   3) CFIR 的 TYPEALIAS_UNUSED_TYPE_PARAMETERS 实参是 `"Generics-<名>"` 逗号连接（:47，与官方 `GenericsTy::String()`
//      同形）；`CfirTypeAliasAccessChecker` 只保留第一个违规目标（:48-50 return），与官方 HasPrevDiag 去重后的效果一致。
//   4) 官方"未用形参"的 `usedTys` 来自 RHS **AST 类型树**的递归 `typeArgs`（嵌套任意深度都算使用，见形态 9b）；
//      CFIR 遍历的是**展开后的 Cone 类型树**。两者是否完全等价本文件不作断言（未跑 CFIR），按官方行为写期望。
//
// 同起点两条诊断的嵌套写法（两处，标记解析按 LIFO：先开先关、内层先闭合）：
//   形态 8 的 V113 段：外层标记名 = TYPEALIAS_UNUSED_TYPE_PARAMETERS、内层标记名 = TYPEALIAS_CYCLE，锚点文本 = t（内层标记只包住 t），
//     —— 外层未用形参、内层环，两条都锚 `t`（去标记后源码是合法仓颉 `type TARBF<T> = ...`）。
//   形态 24：外层标记名 = TYPEALIAS_EXTERNAL_REFER_INTERNAL、内层标记名 = TYPEALIAS_UNUSED_TYPE_PARAMETERS，锚点文本 = t，
//     —— 外层暴露内部类型、内层未用形参，两条都锚 `t`；本段**没有** ACCESSIBILITY_ERROR（别名目标两版都不产 D）。
//
// 伴随诊断登记（官方产出但本文件**不写内联标记**的诊断，避免与被测 kind 混淆）：
//   - `typeAliasAccessibilityAliasTarget.cj` 第 3 行 `type TARBv<T> = TARBwS18`（internal 别名 → private 类）为形态 29
//     的支撑声明，官方在此额外产出：`sema_typealias_external_refer_internal`(3,1)、`typealias_unused_type_parameters`(3,1,
//     warning)、`sema_accessibility`(3,6)。`exact_check.py` 的 `unmarked_official` 计数里前两条 error 会出现（=2）。
//   - 形态 25–28（1.0.5-only 段）在 1.1.3 下必须零诊断，由 `typeAliasRulesV105.cj` 的文件级门禁保证；
//     `exact_check.py` 不看版本指令，跑 V105 文件时仍会照 1.1.3 编一遍（形态 7 段因此在 1.1.3 下记 1 处 DIFF，属预期）。
//   - `exact_check.py` 只比对 **error**（`official_ranges` 跳过 warning），所以 6 个 `TYPEALIAS_UNUSED_TYPE_PARAMETERS`
//     标记恒为 DIFF；这是工具口径（既有 diagnostics2/try/finallyFlowPlaceholder.cj 同样 exact=0 diff=2），
//     其范围已由 verify_one 逐段确认与官方 warning 的 (行,1)..(行,2) 一致。
//
// 已有覆盖不重复：
//   diagnostics2/typealias/a.cj（`type A = Int64` 合法基线）、diagnostics2/typealias/crossPackageImportedAliasInFunctionSignature.cj
//     （跨包导入别名在函数签名；本文件不写包导入形态）、diagnostics2/interop/foreignFunctionTypeAliasExpansion.cj；
//   llt/typealias/typealias2.cj（单形参未用 + 使用点）、3.cj（单形参未用 + 伴随 INVALID_BINARY_OPERATOR）、4.cj（三形参未用）、
//     6.cj（两别名互环）、7.cj（两别名互环且各带未用形参）、17.cj（`A2<T,K> = A1<K>`、`B2<T,K> = B1<K>` 别名链未用）、
//     18.cj（经非泛型别名 `B<T> = A<Rune>` 未用 + `D<T> = C` 间接）、19.cj（两层链 `C1<x> = B1<Int64>`、`A2<T,K> = Type<Rune,T>`）、
//     26.cj（`str<T> = String` 未用 + `?Int32` 实参）、29.cj（约束型形参未用 + 约束不匹配伴随）、
//     34.cj（`TypeClass3<X,Y> = TypeClass2<X>` 经别名未用 + GENERIC_ARGUMENT_NO_MATCH）、
//     typealias_access01.cj（public/private/internal 别名 × Hidden/PrivateTarget/Option/元组/别名链两种，内联名是
//     `ACCESSIBILITY_ERROR` 且锚在别名名上）、typealias_partial_infer_02.cj（`Y<K> = E<Int32,Int8>` 未用 + 枚举构造器推导）、
//     typealias_partial_infer_05.cj（`Y0<K> = I1<Int32,Int8>`、`X1<K> = I2<Int64>` 未用）。
//   本文件补：① 三别名环（形态 2）；② 环上形参**被使用** vs **未使用**的版本分歧（形态 3 / 8）；③ 未用形参在别名链上的
//   传播与"实参被擦除"（形态 6，`A<T> = B<T>` + `B<K> = Int64` 两条都报）；④ 形参放进**别名实参列表**不算使用（形态 9）
//   vs 放进 RHS 类型**内部**（任意深度）算使用（形态 9b，两版零诊断）；⑤ RHS 类 where 约束传播到别名形参（形态 7，1.0.5-only）；
//   ⑥ public 别名 → 默认可见类（形态 11）、→ internal 别名（形态 15，只有 C 没有 D）、private 别名跳过（形态 16）、
//   public 别名 → 泛型别名（形态 19，note 指向展开后类型）、internal 别名 → private 类（形态 12）、public 别名 → 泛型形参的
//   别名（形态 23，带形参别名上 D 仍只锚名字）、别名目标两种（形态 22 非泛型 / 形态 29 泛型）、D 与 C 同起点的两条诊断
//   （形态 24）；⑦ public → private 出现在 Array/元组/函数类型/Option **内层**时 D 只在 1.0.5（形态 25–28）；
//   ⑧ 合法对照：public→public 类（30）、internal→internal 类（18）、private 别名用在 public 函数签名（20）、
//   泛型形参被跳过（21）、全部形参都用上（31）、internal→public 类（32）。
//
// 已删除形态（探针取证，未写进本文件）：
//   - **自引用别名** `type A<T> = A<T>` / `type A = A`：两版都报 `sema_undeclared_type_name`（锚实参 `A`），
//     1 字符；解析/名字解析阶段就认定未声明，别名自身不在作用域里——所以不是 `sema_typealias_cycle`。
//   - **别名经泛型类参数成环** `class C<T>{}` + `type D<T> = C<C<T>>`（探针 p1 s3）：两版零诊断，官方没有这一形态。
//   - **partial inference 报告点**（TypeCheckType.cpp:198 / TypeCheckGeneric.cpp:325）：只擦除未用实参，不报本族
//     （探针 p3 b1/b2/b3：`type A<K> = Array<K>` + `type B = A<Int64>` 零诊断；`type A<T,K> = Array<T>` + `func f(x: A<Int8,Bool>)`
//     只有声明处一条）。
//   - **别名声明带 where** `type A<T> where T <: B = B`：官方 parser 不支持——`ParseTypeAlias`（src/Parse/ParseDecl.cpp:1174-1211）
//     在泛型列表后只 `Skip(ASSIGN)`，没有 `Skip(WHERE)`（`ParseConstraints` 的调用点在 class/func/extend/enum 的 parse 函数里，
//     ParseDecl.cpp:852/1058/1143/1240），`ParseGeneric`（:1283-1325）也只吃 `<...>`；两版实测 `parse_expected_assignment` +
//     `parse_expected_type`。改用"RHS 类带 where、约束传播"形态（形态 7）。
//   - **1.0.5 段与 1.1.3 段同名声明**：同名会触发 `sema_redefinition`（DiagnosticSema.def:21，锚声明名）并让环诊断被
//     `CanBeEmitted` 的类别优先策略吞掉，所以两段必须用不同声明名（TARBD/TARBE 与 TARBF/TARBG）。
//   - std 容器 `List` / `HashMap` 在两版都不存在（`sema_undeclared_type_name`），改用 `Array`。

// FILE 说明：` 段单独编译
//   （官方整包编译时一处 parse 错误会吞掉其余段的语义诊断，见 DiagnosticEngine.cpp 的 CanBeEmitted）。
// 两版在下面列出的**分歧段**上行为不同，用 LANGUAGE_VERSION 门禁；其余段两版逐段一致。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**；三个 kind 都是非 refactor kind，
//   官方 `Diagnose(node, kind)`（DiagnosticEngine.h:843-857）退化成 `GetBegin()` **1 个字符**，即 `type` 关键字首字母 `t`，
//   报 Begin..Begin+1（探针实测：段 typeAliasCycleTwoWay (1,1)..(1,2)、(3,1)..(3,2)）。
// 内联标记写法：由"小于感叹号 + 诊断名 + 大于感叹号 + 锚点文本 + 结束标记"五段构成；全文只在 `// FILE:` 段内出现，
// 头部注释一律用文字描述，不出现该序列的字面量。
// **未跑 CFIR**：本文件不依据本项目 CFIR 的实际输出编写，头部只按源码写出 CFIR 名、生产者位置与可预判的锚点差异。
//
// 官方实现（external/cangjie_compiler，工作树 checkout 在 tag v1.0.0）：
//   A. 别名环 → src/Sema/PreCheck.cpp:1140-1152 `TypeAliasCircleCheck`：**导入包直接跳过**
//      （`:1142 if (ctx.curPackage->TestAttr(Attribute::IMPORTED)) return`），对本包 `TYPE_ALIAS_DECL`
//      按 `Sort::posAsc`（声明位置升序）逐个以**空 path** 调 `CheckTypeAliasCycleForOneDecl`。
//      :1187-1202 `CheckTypeAliasCycleForOneDecl`：目标 `tad.checkFlag == VISITING` 即成环，
//      :1191-1192 `diag.Diagnose(tad, DiagKind::sema_typealias_cycle, ConstructDiagInfoForCycle(tad, path).c_str())`，
//      **立即 return**（不在此处把 checkFlag 置 VISITED，由外层后续遍历再次进入该别名时从别处成环）；
//      否则置 VISITING、push 进 path、`GetTypeArgsOfType(tad.type)` 取 RHS 全部类型节点（前序，见 src/Sema/Utils.cpp:390-427）
//      后 :1199 逐个 `CheckTypeAliasCycleForOneType`（:1154-1172，只对 `REF_TYPE` / `QUALIFIED_TYPE`
//      且 `ref.target` / `target` 是 `TYPE_ALIAS_DECL` 递归），再 pop、置 VISITED。
//      文案 :38 `"type cycle detected: '%s'"`，实参来自 PreCheck.cpp:35-52 `ConstructDiagInfoForCycle`：
//      从 path 里第一次出现 `root` 起拼接 `标识符->`，所以报在环上每个别名上的链条各自不同
//      （探针实测两别名互环 `'A->B->A'` 与 `'B->A->B'`；三别名环三条 `'X->Y->Z->X'`、`'Y->Z->X->Y'`、`'Z->X->Y->Z'`）。
//      **RHS 里非别名引用（含泛型类实例）不进 DFS**：`class C<T> {}` + `type D<T> = C<C<T>>` 两版零诊断（官方无法把
//      C 的实例化展开回 `D`，见 `HandleAliasForRefType`），故官方没有"别名经泛型类参数成环"这一形态。
//   B. 未使用的类型形参 → src/Sema/TypeCheckDecl.cpp:679-701 `CheckTypeAlias`：`IS_CHECK_VISITED` 早退；
//      :687 `!tad.type || !Ty::IsTyCorrect(tad.type->ty)` 早退（RHS 环起来后类型无效 → 本族不报，见形态 8）；
//      :688 调 `CheckTypeAliasAccess`；:690-695 `if (tad.generic)` 时用 :660-677 `GetUnusedTysInTypeAlias`
//      按**声明顺序**列出 `tad.generic->typeParameters` 中不出现在 `GetTyArgsRecursive(*tad.type->ty)`（:648-658，
//      递归收集 RHS 类型的全部 `typeArgs`）里的类型实参，非空则 :694
//      `diag.Diagnose(tad, DiagKind::typealias_unused_type_parameters, Ty::GetTypesToStr(diffs, ","))`。
//      文案 include/cangjie/Basic/DiagnosticSema.def:304 `WARNING(typealias_unused_type_parameters, UNUSED,
//      "type arg(s) %s are not used")` —— **warning**（UNUSED 类，可用 `-Woff unused` 抑制；JSON note
//      "this warning can be suppressed by setting the compiler option `-Woff unused`"）；实参是类型名列表，
//      形参类型 `GenericsTy::String()`（src/AST/Types.cpp:785-788）输出 `"Generics-" + name`，
//      分隔符是**逗号无空格**（Types.h:340-351）。1.0.5/1.1.3 实测多形参只报**一条**、实参带**尖括号**
//      （`'type arg(s) <Generics-T,Generics-K> are not used'`）。
//      另有两个"偏推导"报告点：`src/Sema/TypeCheckType.cpp:198`（类型位置上引用别名，如 `var x: A<Int64>`）与
//      `src/Sema/TypeCheckGeneric.cpp:325`（表达式位置上引用别名，如 `A<Int64>(...)`）—— 两处都是**擦除**未用实参后
//      再推导，**不报**本诊断（探针：`type A<K> = Array<K>` + `type B = A<Int64>` 零诊断；
//      `type A<T,K> = Array<T>` + `func f(x: A<Int8,Bool>)` 在两版都只有声明处一条）。
//   C. 别名暴露低访问级别类型 → src/Sema/TypeCheckDecl.cpp:619-642 `CheckTypeAliasAccess`：`PRIVATE` 别名跳过（:620）；
//      :623-624 `GetTypeArgsOfType(tad.type)` 取 RHS 全部类型节点；:629-630 只处理 `REF_TYPE`（元组 / 函数类型 /
//      `Option<?T>` / `Array<T>` 的**内层**类型节点照样进入 vector，但它们自身不是 `REF_TYPE`，靠 :637 `if (!decl) continue`
//      或 :638 `decl->astKind != GENERIC_PARAM_DECL && …` 过滤）；:639
//      `!IsCompatibleAccessLevel(tadLevel, GetAccessLevel(*decl))` 即 `tadLevel > declLevel`
//      （include/cangjie/AST/Utils.h:148-151；`AccessLevel{PRIVATE=0, INTERNAL=1, PROTECTED=2, PUBLIC=3}`，Node.h:2782-2787）
//      时 :640 `diag.Diagnose(tad, DiagKind::sema_typealias_external_refer_internal, tadLevelStr, tad.identifier.Val(),
//      GetAccessLevelStr(*decl), decl->identifier.Val())`。
//      - **只比对语法位置上写下的那个声明，不穿透别名**（:632 `decl = rt->ref.target`）：`public type A = B` 在 `B` 为 public 时不报，
//        即使 `B` 展开后引用了 internal 类型（探针：`class C<T>{}` + `type B<T> = C<T>` + `public type A = B<Int64>` 报
//        `'B'` 是 internal；对照 `internal type C = Hidden` + `public type A = C` 只报 `'C'`，不报 `Hidden`）。
//      - 同一别名上**多个**低级别目标只报**一条**（`for` 循环内无 break，但 DiagEngine 按同类同范围去重），
//        报的是 `GetTypeArgsOfType` 前序里**第一个**违规目标（探针：`(TAR44A, TAR44B)` 都 private 只报第一个 `TAR44A`）。
//      - `GENERIC_PARAM_DECL`（别名的类型形参）被 :639 显式排除，所以 `public type A<T> = Array<T>` 合法（探针零诊断）。
//      文案 DiagnosticSema.def:39 `"'%s' type '%s' refers to '%s' type '%s'"`（级别串来自 src/AST/Utils.cpp:465-479）。
//   D. 伴随诊断 `sema_accessibility`（**refactor** kind，include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:23
//      `"'%s' declaration uses %s types"`）：与 C 同场景产出，生产者 src/Sema/Diags.cpp:505-543 `DiagLowerAccessLevelTypesUse`，
//      :512 锚 `MakeRangeForDeclIdentifier(outDecl)`（:109-137，普通 Decl → `MakeRange(decl.identifier)`，即**别名名**），
//      带 MainHint `"inferred type"`，note `"type '<Ty>' contains <level> type"`（节点范围）+ `"the <level> type is '<Ty>'"`。
//      **与 C 的差别（探针实测 1.0.5 vs 1.1.3 分歧）**：1.0.5 按**展开后的类型树**递归检查元素类型，1.1.3 只对
//      RHS **直接**引用的 `Decl` 判定——`public type A = Array<PrivateX>` / `= (PrivateX, Int64)` / `= (Int64) -> PrivateX`
//      / `= Option<PrivateX>` 在 1.0.5 出 `sema_accessibility`（锚 RHS 整体，范围 (1,13)..(1,27) 等）而 1.1.3 **不出**。
//      这种 1.0.5-only 段放在 `typeAliasRulesV105.cj`（文件级门禁 1.0.5），在 1.1.3 下必须零诊断。
//      另外 `public func f(x: PrivateType)`（类型别名不是别名 RHS，见形态 5 的 probe）两版都**不**报 `sema_accessibility`。
//
// 本项目 CFIR 名（cfir/checkers/checkers-component-generator/src/org/cangnova/cangjie/cfir/checkers/generator/
// diagnostics/CfirDiagnosticsList.kt，生成物 cfir/checkers/gen/.../diagnostics/CfirErrors.kt）：
//   :2650 `TYPEALIAS_CYCLE` by error<PsiElement>(parameter<String>("typeAlias"))
//     ← 官方 `sema_typealias_cycle`
//   :2645 `TYPEALIAS_UNUSED_TYPE_PARAMETERS` by **warning**<PsiElement>(parameter<String>("typeParameters"))
//     ← 官方 `typealias_unused_type_parameters`
//   :1174 `TYPEALIAS_EXTERNAL_REFER_INTERNAL` by error<PsiElement>(Visibility, Name, Visibility, Name)
//     ← 官方 `sema_typealias_external_refer_internal`
//   :1167 `ACCESSIBILITY_ERROR` by error<PsiElement>(declarationKind: String, visibility: Visibility)
//     ← 官方 `sema_accessibility` / `sema_accessibility_with_main_hint`
// 文案表 cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrorsDefaultMessages.kt：
//   :1365 TYPEALIAS_UNUSED_TYPE_PARAMETERS `"type arg(s) {0} are not used"`（与官方逐字一致）
//   :1366 TYPEALIAS_CYCLE `"type cycle detected: ''{0}''"`（与官方逐字一致）
//   :1368-1371 TYPEALIAS_EXTERNAL_REFER_INTERNAL `"''{0}'' type ''{1}'' refers to ''{2}'' type ''{3}''"`（与官方逐字一致）
//   :886 ACCESSIBILITY_ERROR `"''{0}'' declaration uses {1} types"`（与官方逐字一致；`declarationKind` 传别名名，
//     官方传 `GetAccessLevelStr(outDecl)` 级别串——**本项目参数含义与官方文案实参不同**，见"可预判差异"第 2 条）
// 生产者（全部在 cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/）：
//   CfirTypeAliasCycleChecker.kt:32（`check` :25-35：只在 `expandedTypeRef` 是带
//     `DiagnosticKind.RecursiveTypealiasExpansion` 的 `CfirErrorTypeRef` 时报，即只在别名展开结果为递归错误时才报）
//   CfirTypeAliasUnusedTypeParameterChecker.kt:46（`check` :26-49：遍历**展开后**类型的全部 `ConeTypeParameterType`，
//     与 `declaration.typeParameters` 求差；`:29-33` 对 error type / 未解析限定符跳过；`:56-64` 在循环别名上改读
//     `delegatedTypeRef` / `partiallyResolvedTypeRef` 保留 RHS 类型实参）
//   CfirTypeAliasAccessChecker.kt:54（`check` :40-60：`PRIVATE` 跳过；`:46` 展开类型含 error type 跳过；
//     `:48` `firstExposureTarget`（:71-112）按前序找**第一个**违规目标、遇到即返回；`:77-86` classId 优先取
//     `abbreviatedType` 即**别名自身**，与官方"不穿透别名"一致）
//   ACCESSIBILITY_ERROR 由可访问性检查器族产出（本文件只记录名与文案，不列具体类）。
//
// 可预判锚点差异（**未跑 CFIR**，按源码写出）：
//   1) 官方三个 kind 都 `Diagnose(tad, …)` → `GetBegin()` 一字符（`tad.begin` 是 `type` 关键字，见 TypeAliasDecl
//      Node.h:1132-1138 的 `begin` 注释 "Position of 'type'"）。CFIR 三个 checker 统一用
//      `CfirDeclarationDiagnosticSources.kt:187-203 typeAliasDeclarationHeaderDiagnosticSource()`，范围是
//      **`type` 关键字 → 类型形参列表末尾（无形参则到别名名末尾）**：形参别名为 `type A<T>` 整段，非形参别名为 `type A`，
//      官方为 `t`。故本文件按官方写 1 字符标记，CFIR 在这三个 kind 上会给出更宽的范围（与
//      llt/typealias/typealias6.cj、7.cj 等既有夹具里 1 字符锚点标记并存）。
//   2) CFIR 的 TYPEALIAS_EXTERNAL_REFER_INTERNAL 参数 a/b/c/d 为 (aliasVisibility, 别化名, targetVisibility, targetName)，
//      文案实参渲染成 `'<可见级别>' type '<别名>' refers to '<目标级别>' type '<目标名>'`，**与官方逐字同形**；
//      但 ACCESSIBILITY_ERROR 的 CFIR 参数 (declarationKind, visibility) 会渲染成 `'<别名名>' declaration uses '<级别>' types`，
//      官方是 `'<级别>' declaration uses '<级别>' types`（第一个实参是**级别**而非名字），两者不一致（文案实参层面）。
//   3) CFIR 的 TYPEALIAS_UNUSED_TYPE_PARAMETERS 实参为 `"Generics-<名>"` 逗号连接（:47），与官方 `GenericsTy::String()` 同形，
//      但**不含**官方 1.0.5/1.1.3 实测的尖括号。
//
// 已有覆盖不重复：
//   diagnostics2/typealias/a.cj（`type A = Int64` 合法基线）、diagnostics2/typealias/crossPackageImportedAliasInFunctionSignature.cj
//     （跨包导入别名在函数签名；本文件不写包导入形态）、diagnostics2/interop/foreignFunctionTypeAliasExpansion.cj；
//   llt/typealias/typealias2.cj（单形参未用 + 使用点）、3.cj（单形参未用 + 伴随 INVALID_BINARY_OPERATOR）、4.cj（三形参未用）、
//     6.cj（两别名互环）、7.cj（两别名互环且各带形参）、17.cj（别名链未用：`A2<T,K> = A1<K>`、`B2<T,K> = B1<K>`）、
//     18.cj（经非泛型别名 `A<K> = structA<K>` 的 `B<T> = A<Rune>` 未用 + `D<T> = C` 间接）、19.cj（两层链 `C1<x> = B1<Int64>`、
//     `A2<T,K> = Type<Rune,T>` 未用）、26.cj（`str<T> = String` 未用 + `?Int32` 实参）、29.cj（约束型形参未用）、
//     34.cj（经别名 `TypeClass3<X,Y> = TypeClass2<X>` 未用）、typealias_access01.cj（public/private/internal 别名 ×
//     Hidden/PrivateTarget/Option/元组/别名链两种）、typealias_partial_infer_02.cj（`Y<K> = E<Int32,Int8>` 未用 + 枚举构造器
//     推导）、typealias_partial_infer_05.cj（`Y0<K> = I1<Int32,Int8>`、`X1<K> = I2<Int64>` 未用）。
//   本文件补：① 环的**类型参数组合**（环上各别名形参**都被使用**时 1.0.5/1.1.3 均不报未用，1.0.5 缺 `typealias_unused_type_parameters`
//     而 1.1.3 报——形态 8 分歧）；② 三别名环；③ **别名间未用的传播**（`A<T> = B<T>` 且 `B<K> = Int64` 时未用报在 `A` 而非 `B`——形态 6）；
//   ④ 泛型形参在 Option 脱糖后仍算使用（形态 5）；⑤ 别名 RHS 是另一别名且该别名是泛型（形态 4）；⑥ 带 where 约束的未用形参（形态 7）；
//   ⑦ 访问级别：`internal` 别名 → private（形态 12）、public → internal **默认可见类**（形态 13）、别名链（形态 15/18）、
//     经泛型别名再包一层（形态 19/20）、private 别名跳过的函数签名用法（形态 21）、internal → public 合法（形态 22）、
//     public → internal 类型 + 两处 private 的元组只报首个（形态 16）；⑧ 删除形态的自引用 / 经泛型类参数 / partial inference
//     见文末"已删除形态"。

// ===== 形态 1：两别名互环（`TARA -> TARB -> TARA`），两版一致；与既有 typealias6.cj 的区别是无形参。
// FILE: typeAliasCycleTwoWay.cj
<!TYPEALIAS_CYCLE!>type TARA<!> = TARB

<!TYPEALIAS_CYCLE!>type TARB<!> = TARA

func tarMainCycleTwoWay() {}

// ===== 形态 2：三别名成环，报三条且链条各异；既有夹具未覆盖。
// FILE: typeAliasCycleThreeWay.cj
<!TYPEALIAS_CYCLE!>type TARB1<!> = TARB2

<!TYPEALIAS_CYCLE!>type TARB2<!> = TARB3

<!TYPEALIAS_CYCLE!>type TARB3<!> = TARB1

func tarMainCycleThreeWay() {}

// ===== 形态 3：环上别名**带形参且形参被使用** → 1.0.5 与 1.1.3 都**不**报未用形参（1.1.3 实测为两条 CYCLE + 两条
// UNUSED，见形态 8 的版本分歧说明；本段不加 UNUSED 标记，1.0.5 零附加诊断）。既有 typealias7.cj 的形参**未被使用**。
// FILE: typeAliasCycleWithUsedTypeParam.cj
<!TYPEALIAS_CYCLE!>type TARB4<T><!> = TARB5<T>

<!TYPEALIAS_CYCLE!>type TARB5<U><!> = TARB4<U>

func tarMainCycleUsedParam() {}

// ===== 形态 4：单形参未使用（`type A<T> = Int64`），1 字符锚 `t`；既有 typealias2.cj 的同一形态带使用点与伴随诊断。
// FILE: typeAliasUnusedSingleParam.cj
<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>type TARB6<T><!> = Int64

func tarMainUnusedSingle() {}

// ===== 形态 5：多个形参只用一个 → **一条**诊断、逗号无空格连接两个未用形参（`1.0.5` / `1.1.3` 实测同为
// `'type arg(s) Generics-T,Generics-K are not used'`，**不带尖括号**）；`Option<?Int64>` 脱糖成 `Enum-Option` 也不影响判定。
// FILE: typeAliasUnusedMultipleParams.cj
<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>type TARB7<T, K><!> = Option<?Int64>

func tarMainUnusedMultiple() {}

// ===== 形态 6：未用形参**在别名链上传播**：`TARB8<T> = TARB9<T>` 引用 TARB9 时，`CheckRefTypeWithRealTarget`
// （TypeCheckType.cpp:195-200）先算出 TARB9 的未用形参 {K} 并 `EraseIf` 掉对应实参（此处被擦掉的是 T），于是链头
// TARB8 的 RHS 类型树里不再有 T —— **T 也算未用**。两版都报两条 warning：`Generics-T` 报在 TARB8（链头）、
// `Generics-K` 报在 TARB9。对照：K 被用到的链（`type A<T> = B<T>`、`type B<K> = Array<K>`）两版零诊断（合法对照 31）。
// FILE: typeAliasUnusedPropagatedToNonGenericAlias.cj
<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>type TARB8<T><!> = TARB9<T>

<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>type TARB9<K><!> = Int64

func tarMainUnusedPropagated() {}

// ===== 形态 7：约束来自 RHS 类的 where（别名声明本身**不能**带 where，见文末"已删除形态"），**1.0.5-only**：
// `interface TARBA` + `class TARBB<X> where X <: TARBA` + `type TARBC<T, K> = TARBB<T>`：别名形参 T 变成 `BoundedTy`
// （`GenericsTy::lowerBound`，由 PreCheck.cpp:1324-1334 `IgnoreAssumptionForTypeAliasDecls` 与 TypeCheckDecl.cpp:560-617
// `GetAllAssumptions` 从 RHS 传播）。**版本分歧**：1.0.5 报 `'type arg(s) Generics-K are not used'`（warning，锚 `t`），
// 1.1.3 对同样的"约束传播型"别名**零诊断**（探针另测 `type A<T,K> = C<T>`、`type A<T1,T2,T3> = C<T1,T1>` 两例同此）；
// 1.1.3 下本段必须零诊断，故加 `// LANGUAGE_VERSION 指令 1.0.5`。
// FILE: typeAliasUnusedParamInTypeArgument.cj
<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>type TARBH<T><!> = TARBIS10<Int64>

type TARBIS10<K> = Array<K>

func tarMainUnusedInTypeArg() {}

// ===== 形态 9b：形参出现在 RHS 类型**内部**（`TARN<T> = Array<TARBIS10<T>>`）——两版都**零诊断**：`GetTyArgsRecursive`
// 对 `Array<TARBIS10<T>>` 递归收集时把**内层** `TARBIS10<T>` 的类型实参 `T` 也收了进来（Utils.cpp:390-427 对 REF_TYPE
// 递归 `typeArguments`），所以 T 算"被使用"。与形态 7/9 的对照说明：`type A<T> = C<T>`（类，C 也是别名）报 K 不报 T，
// 而 `type X<T> = Array<XAlias<T>>` 完全不报 —— **官方 `usedTys` 是对 RHS AST 类型树逐个类型实参收集的，嵌套深度不限**，
// 与 CFIR 侧遍历 `ConeType` 的 `ConeTypeParameterType` 是否等价需要单独核对（本文件只按官方行为写期望）。
// FILE: typeAliasUnusedParamInsideTypeArgument.cj
type TARN<T> = Array<TARBIS11<T>>

type TARBIS11<K> = Array<K>

func tarMainUnusedInsideTypeArg() {}

// ===== 形态 10：`public type TARBj = PrivateTarget` → C（锚声明首字符 `p`）+ D（锚别名名 `TARBj`）。
// FILE: typeAliasAccessPublicRefPrivate.cj
private class TARBJ {}

public <!TYPEALIAS_EXTERNAL_REFER_INTERNAL!>type <!ACCESSIBILITY_ERROR!>TARBjV9<!><!> = TARBJ

func tarMainAccessPublicRefPrivate() {}

// ===== 形态 11：默认可见（internal）类被 public 别名引用 → C 报 internal（锚声明首字符 `p`）；D 锚别名名 `TARBl`。两版一致
// （探针 p2 a13、p7 h3 实测 kind / 范围 / 文案完全相同）。
// FILE: typeAliasAccessPublicRefInternal.cj
class TARBK {}

public <!TYPEALIAS_EXTERNAL_REFER_INTERNAL!>type <!ACCESSIBILITY_ERROR!>TARBlV10<!><!> = TARBK

func tarMainAccessPublicRefInternal() {}

// ===== 形态 12：默认可见（internal）别名引用 private 类型 → C 报 private，D 锚别名名（`type X` 的 `X`，无类型形参）。
// FILE: typeAliasAccessInternalRefPrivate.cj
private class TARBmV11 {}

<!TYPEALIAS_EXTERNAL_REFER_INTERNAL!>type <!ACCESSIBILITY_ERROR!>TARBnV11<!><!> = TARBmV11

func tarMainAccessInternalRefPrivate() {}

// ===== 形态 13：内层类型（`Array<TARBo>` 的类型实参）也参与访问级别判定；既有 typealias_access01.cj 只覆盖了
// `Option<Hidden>`，本段是 `Array<…>` 形态。
// FILE: typeAliasAccessNestedTypeArgument.cj
private class TARBoV12 {}

<!TYPEALIAS_EXTERNAL_REFER_INTERNAL!>type <!ACCESSIBILITY_ERROR!>TARBpV12<!><!> = Array<TARBoV12>

func tarMainAccessNestedTypeArg() {}

// ===== 形态 14：元组里两个 private 目标 → C 只报**第一个**（`GetTypeArgsOfType` 前序 + DiagEngine 同类同范围去重），
// D 的 note 列出整个元组类型。
// FILE: typeAliasAccessTupleFirstOnly.cj
private class TARBqV13 {}

private class TARBrS16 {}

<!TYPEALIAS_EXTERNAL_REFER_INTERNAL!>type <!ACCESSIBILITY_ERROR!>TARBsV13<!><!> = (TARBqV13, TARBrS16)

func tarMainAccessTupleFirstOnly() {}

// ===== 形态 15：别名链——`public type = internal 别名` 只报语法位置上写下的 internal 别名 `TARBu`，**不穿透**到
// `Int64`，且此时 D **不出现**（D 判定的是 RHS **直接**引用的 Decl；别名目标没有 D，探针 p2 a11、p7 h4 两版一致）。
// 既有 typealias_access01.cj 的 InternalAliasUser 形态未标 D。
// FILE: typeAliasAccessChainToInternalAlias.cj
type TARBuV14 = Int64

public <!TYPEALIAS_EXTERNAL_REFER_INTERNAL!>type TARBvV14<!> = TARBuV14

func tarMainAccessChainToInternal() {}

// ===== 形态 16：别名链——`private type TARBx = private 类` **合法**（`CheckTypeAliasAccess` 首行 PRIVATE 早退；探针 p7 h5
// 实测零诊断，与既有 LLT `private type F = Hidden` 一致）；再 `type TARBy = TARBx`（internal 别名 → private 别名）
// → C（锚 `t`）+ D（锚别名名 `TARBy`）。两版一致（探针 p7 h5）。
// FILE: typeAliasAccessChainToPrivateAlias.cj
private class TARBwS18 {}

private type TARBxV15 = TARBwS18

<!TYPEALIAS_EXTERNAL_REFER_INTERNAL!>type <!ACCESSIBILITY_ERROR!>TARByV15<!><!> = TARBxV15

func tarMainAccessChainToPrivate() {}

// ===== 形态 17：`private 别名 = private 类型` 合法（CheckTypeAliasAccess 首行 PRIVATE 早退），既有 typealias_access01.cj 已覆盖。
// ===== 形态 18：`internal 别名 = internal 类型` 合法（`INTERNAL <= INTERNAL`）。既有 typealias_access01.cj 未覆盖 internal→internal。
// FILE: typeAliasAccessLegalInternalToInternal.cj
internal class TARBzS19 {}

internal type TARCaS19 = TARBzS19

func tarMainAccessLegalInternal() {}

// ===== 形态 19：经泛型别名再包一层——`type TARBb<T> = TARBc<T>`（internal）、`public type TARBd = TARBc<Int64>`
// 只报语法位置上的 `TARBc`（internal）；D 的 note 是**展开后**类型 `Class-TARBc<Int64>` 与形参类型
// `Class-TARBc<Generics-T>`。两版一致（探针 p3 b7、p5 e1、p7 h6）。
// FILE: typeAliasAccessThroughGenericAlias.cj
class TARBb<T> {}

type TARBc<T> = TARBb<T>

public <!TYPEALIAS_EXTERNAL_REFER_INTERNAL!>type <!ACCESSIBILITY_ERROR!>TARBd<!><!> = TARBc<Int64>

func tarMainAccessThroughGeneric() {}

// ===== 形态 20：`public func` 用 private 别名合法（类型别名只在自己 RHS 里查级别）。既有 typealias_access01.cj 未覆盖函数形态。
// FILE: typeAliasAccessLegalPrivateAliasInSignature.cj
private type TARBe = Int64

public func tarUsePrivateAlias(value: TARBe): TARBe {
    return value
}

// ===== 形态 21：内层类型在**泛型形参**位置上（`Array<TARBf>` 的 T 是 `TARBf` 自己的形参）→ `GENERIC_PARAM_DECL` 被跳过，
// 合法对照；既有 typealias_access01.cj 的 `E<T> = Array<T>` 未标 D，本段补 D 的阴性对照。
// FILE: typeAliasAccessLegalTypeParamNotChecked.cj
private class TARBf {}

public type TARBg<T> = Array<T>

func tarMainAccessTypeParamSkipped() {}

// ===== 形态 22：public 别名引用**非泛型** private 别名（`public type TARCd = TARBh`，`private type TARBh = TARBi`）→
// C 报语法位置上的**别名** `TARBh`（`'public' type 'TARCd' refers to 'private' type 'TARBh'`），**没有 D**
// （`sema_accessibility` 对非泛型别名目标两版都不产出，探针 p2 a10 实测）；与形态 29 的**泛型**别名目标
// （`TARBv<Int64>`，D 会穿透到展开后的类）对照。
// FILE: typeAliasAccessAliasTargetFirst.cj
private class TARBi {}

private type TARBh = TARBi

public <!TYPEALIAS_EXTERNAL_REFER_INTERNAL!>type <!ACCESSIBILITY_ERROR!>TARCd<!><!> = TARBh

func tarMainAccessAliasTargetFirst() {}

// ===== 形态 23：类型别名**带形参**且引用 private 类型 → C 锚 `type` 首字母 1 字符；D（`sema_accessibility`）锚别名名
// `TARBk`（`MakeRangeForDeclIdentifier`，Diags.cpp:512 + :109-137 —— 注意它锚的是**名字**，**不含**类型形参列表）。
// （本文件所有 ACCESSIBILITY_ERROR 锚点都是别名名，不是 `type 名字<T>` 整段。）
// FILE: typeAliasAccessWithTypeParam.cj
private class TARBjV21 {}

<!TYPEALIAS_EXTERNAL_REFER_INTERNAL, TYPEALIAS_UNUSED_TYPE_PARAMETERS!>type <!ACCESSIBILITY_ERROR!>TARBk<!><T><!> = TARBjV21

func tarMainAccessWithTypeParam() {}

// ===== 形态 24：默认可见（internal）别名引用 private 类型**别名** → C 报 `TARBl`（private 别名），并与形态 4 的未用形参
// **共现于同一别名**、同一起点（`CheckTypeAliasAccess` 在 `CheckTypeAlias` 早退之后、unused 之前报告，TypeCheckDecl.cpp:687-695；
// 官方同一别名上多个 RHS 违规目标只报一条是 `HasPrevDiag` 按（起点, 文案）去重，src/Basic/DiagnosticEngine.cpp:716-723 + :778-780）。
// A 锚 `type` 首字母 1 字符，B 锚 `type` 首字母 1 字符（与 A 同点，用嵌套标记），**本段没有 D**：RHS 直接引用的是
// `private type TARBl`，`sema_accessibility` 在 1.0.5 / 1.1.3 都不为别名目标产出（探针 p2 a11 与本段实测一致）。
// FILE: typeAliasAccessAndUnusedSameStart.cj
private type TARBlV22 = Int64

<!TYPEALIAS_EXTERNAL_REFER_INTERNAL, TYPEALIAS_UNUSED_TYPE_PARAMETERS!>type TARBmV22<T><!> = TARBlV22

func tarMainAccessAndUnused() {}

// ===== 形态 25（1.0.5-only）：`public type = Array<private 类>` —— C 报告（`Array` 的类型实参节点），D（`sema_accessibility`）
// 只在 **1.0.5** 出现（1.0.5 按展开类型树递归检查元素类型，1.1.3 只对 RHS 直接引用的 Decl 判定），范围是 RHS 整体；
// 1.1.3 下本段必须零诊断，故加 `// LANGUAGE_VERSION 指令 1.0.5`。
// FILE: typeAliasAccessibilityAliasTarget.cj
private class TARBwS30 {}

type TARBvV23<T> = TARBwS30

public <!TYPEALIAS_EXTERNAL_REFER_INTERNAL!>type <!ACCESSIBILITY_ERROR!>TARBxV23<!><!> = TARBvV23<Int64>

func tarMainAccessibilityAliasTarget() {}

// ===== 合法对照 30：public 类 + public 别名（`PUBLIC <= PUBLIC`）零诊断；既有 typealias_access01.cj 有 `public type C = Int64`
// 但没有 public 类 → public 别名的形态。
// FILE: typeAliasAccessLegalPublicToPublic.cj
public class TARByV24 {}

public type TARBzS31 = TARByV24

func tarMainAccessLegalPublic() {}

// ===== 合法对照 31：全部类型形参都被使用（`type TARCa<T> = HashMap` 不行——std 无 HashMap；用 `Array`）→ 零诊断。
// FILE: typeAliasLegalAllTypeParamsUsed.cj
type TARCaS32<T> = Array<T>

type TARCb<K> = TARCaS32<K>

func tarMainLegalAllParamsUsed() {}

// ===== 合法对照 32：internal 别名引用 public 类型零诊断。
// FILE: typeAliasAccessLegalInternalToPublic.cj
public class TARCC {}

internal type TARCD = TARCC

func tarMainAccessLegalInternalToPublic() {}
