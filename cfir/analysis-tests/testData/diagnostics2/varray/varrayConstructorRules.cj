// RUN_PIPELINE_TILL: FRONTEND
// VArray 构造器实参个数 / VArray 元素类型限制 / VArray 下标索引个数 / VArray 作 CFunc 返回 四族规则语义夹具。
//
// 取证方式：官方 cjc 1.0.5 与 1.1.3 两版实测，静态库模式
//   `cjc <段>.cj -o out.a --output-type=staticlib --diagnostic-format json`；每个 `// FILE:` 段单独编译
//   （官方整包编译时一处 parse 错误会吞掉其余段的语义诊断）。两版对本文件全部段的 kind、文案、MainHint.Range
//   逐段一致，**无版本分歧**，因此不拆 LANGUAGE_VERSION 门禁段。
// 范围口径：cjc JSON 的 MainHint.Range 结束列是**开区间**（单字符锚点报 Begin..Begin+1），本文件标记逐字符对齐官方范围；
//   正文里的 (行,列) 均为段内行号（`// FILE:` 行被剥离后的干净文本坐标）。
// 内联标记写法：由"小于感叹号 + 诊断名 + 大于感叹号 + 锚点文本 + 结束标记"五段构成；全文只在 `// FILE:` 段内出现，
//   头部注释一律用文字描述，不出现该序列的字面量。
//
// 本机两版 cjc 实测事实：
//   - `VArray` 是**内建**类型：不写任何 import 即可使用；反过来加 `import std.collection.*` 只会多出一条
//     `sema_unused_import` 警告（实测见下方"已删除形态"）。故本文件所有段都不写 import。
//   - `VArray` 构造器语法为 `VArray<T, $n>(repeat: v)`，也可写成尾随 lambda 的 `VArray<T, $n> {i => ...}`
//     （连括号都不写，实测合法，见 varrayCtorTrailingLambda.cj）。
//   - 1.1.3 本机对含 VArray 的段常在**语义检查之后**的 codegen / Reflection 阶段失败（"Reflection format:
//     modifier type is invalid" / "LLVM ERROR: Broken global value"），stdout 无 JSON；cjc 在语义有错时不会进入
//     codegen，所以带诊断的段两版都能拿到完整语义 JSON；合法对照段在 1.1.3 上可能只崩不报，等价于零语义错误。
//
// 官方实现（external/cangjie_compiler，工作树 checkout 在 tag v1.0.0）：
//
// 1) `sema_varray_args_number_mismatch`（VArray 构造器只接受一个实参）
//   - src/Sema/TypeCheckBuiltinExpr.cpp:370-376 `TypeCheckerImpl::ChkVArrayArg(ASTContext&, ArrayExpr&)`：
//       `if (ve.args.size() != 1) { diag.DiagnoseRefactor(kind, ve, MakeRange(ve.leftParenPos, ve.rightParenPos + 1)); return false; }`
//       要点：
//       (1) 锚点是 `MakeRange(ve.leftParenPos, ve.rightParenPos + 1)`，即**只覆盖圆括号内的实参列表**（从 `(` 到 `)`），
//           **不是**整个 `VArray<T, $n>(...)` 调用表达式——`VArray` 标识符与类型实参不在范围内。实测锚为 `()` / `(1, 2)`。
//       (2) 该 overload 是 `DiagnoseRefactor(kind, const AST::Node&, Args...)`（include/cangjie/Basic/DiagnosticEngine.h:908-915），
//           内部 `MakeRange(node.GetBegin(), node.GetEnd())` 后被显式 range 覆盖，第三个实参其实是 `ve.args[0]->name.Val()`。
//       (3) 判定只看**实参个数**，与 `$n` 的取值无关：`$n` 只在数组字面量路径参与检查
//           （TypeCheckBuiltinExpr.cpp:115-116 `sema_varray_size_match`，已由 varraySizeMismatch.cj /
//           varrayArrayLiteralSizeMismatch.cj 覆盖，本文件不重复）。所以"实参个数 != 1"就是唯一触发条件。
//       (4) 调用点两处：:410 `ChkVArrayExpr` 与 :465 `SynArrayExpr`（无目标类型推导时），两条路径同一判定。
//       (5) **尾随 lambda 计作一个实参**：`ve.args[0]->name.Empty()` 分支（:378-382）说明不带名字的实参按
//           `(Int64) -> T` 函数类型检查，所以 `VArray<Int64,$3> {i => i}` 与 `VArray<Int64,$3>({i => i})` 都不报本条；
//           实测 varray09.cj 的 TYPE_MISMATCH 来自 `Array<Int64>` 这个**目标类型注解**，不是实参个数。
//
// 2) `sema_varray_arg_type_with_reftype`（VArray 元素类型不能直接或间接是引用类型）
//   - src/Sema/TypeCheckType.cpp:110-129 `TypeCheckerImpl::CheckVArrayType(ASTContext&, const VArrayType&)`：
//       先 `Synthesize` 元素类型与 `$n` 常量类型，再
//       `auto [isReferenceType, type] = CheckVArrayWithRefType(*typeArgTy, traversedTy);`
//       `if (isReferenceType) { diag.DiagnoseRefactor(kind, *vt.typeArgument, Ty::ToString(typeArgTy)); builder.AddMainHintArguments(type); }`
//       要点：
//       (1) 锚点是 `*vt.typeArgument`，即**整个类型实参**（本文件实测均为元素类型那 7 个字符），不是整个 `VArray<…>`，
//           也不是 1 个字符。
//       (2) 主文案实参是 `Ty::ToString(typeArgTy)`（实测渲染成 `Class-VCRClass` 这种带前缀的形式），
//           MainHint 附加实参是 `CheckVArrayWithRefType` 返回的 `type`（"contain unsupported instance member
//           variable with type '%s'"）。
//       (3) `CheckVArrayWithRefType`（TypeCheckType.cpp:75-108）直接判为"是引用类型"的第一批：
//           `ty.IsStructArray()`（= 标准库 `Array`，src/AST/Types.cpp:394-400 按 kind == TYPE_STRUCT 且
//           fullPackageName == core 且 identifier == "Array" 判定）、`IsClassLike()`（class/interface）、
//           `IsArray()`、`IsEnum()`、`IsGeneric()`（类型形参）、`(IsFunc() && !IsCFunc())`（普通函数类型）。
//           允许的一批：primitive、CPointer、CString、CFunc、以及 `String`（Types.cpp 里 `String` 是 struct，
//           不是 core 的 `Array`，故不进第一批——注意 `String` 会在**递归进入 struct 实例字段**时被下一条命中，
//           见 varray_with_reftype02/03/04 的既有覆盖）。
//           间接一批：struct 递归其**非 static 的 VAR_DECL 实例字段**（:85-97，`decl->TestAttr(STATIC)` 跳过、
//           `astKind != VAR_DECL` 跳过，所以 prop / static 字段都不参与），tuple 递归每个分量（:99-105），
//           `traversedTy` 集合做环检测。
//   - 入口覆盖面：`CheckVArrayType` 只由 `CheckReferenceTypeLegality` 的 `case ASTKind::VARRAY_TYPE`
//     （TypeCheckType.cpp:53-56）调用，而 `CheckReferenceTypeLegality` 在 :32-35 **先递归全部类型实参**，再按
//     astKind 分派。因此下面这些类型位置都会命中本族（本文件按位置逐段覆盖）：
//       - 任意类型语法的通用 post-visit（src/Sema/TypeChecker.cpp:1089，对 `ASTKind::TYPE` 等）；
//       - REF_TYPE / QUALIFIED_TYPE / PAREN_TYPE（:38-40 / :42-44 / :48-50）/ OPTION_TYPE 脱糖类型（:68-73）；
//       - FUNC_TYPE 的**形参与返回**（:313-319）；
//       - 泛型实参（:32-35 逐 typeArg 递归）；
//       - 类型别名定义（src/Sema/TypeCheckDecl.cpp:698-700，`IS_CHECK_VISITED` 保证同一别名只查一次）；
//       - 泛型上界与约束（src/Sema/TypeCheckGeneric.cpp:28 / :34 `CheckUpperBound`）。
//       另有**独立**的 `sema_varray_in_cfunc` 路径（见第 4 条）与 `sema_invalid_cfunc_return_type`
//       （DiagnosticSema.def:221，已由 interop/foreignFunctionReturnType.cj 覆盖）。
//   - 文案 include/cangjie/Basic/DiagRefactor/DiagnosticSema.def:218（refactor kind，锚整段类型实参）。
//
// 3) `sema_varray_subscript_num`（VArray 只接受一个下标索引）
//   - src/Sema/TypeCheckExpr/SubscriptExpr.cpp:140-146 `TypeCheckerImpl::ChkVArrayAccess(...)`：
//       `if (se.indexExprs.size() != 1) { diag.DiagnoseRefactor(kind, se); return false; }`
//       要点：
//       (1) 锚点是**整个 SubscriptExpr**（`DiagnoseRefactor(kind, const AST::Node&)`，DiagnosticEngine.h:908-915
//           → `MakeRange(GetBegin(), GetEnd())`），实测 `a[1, 2]` / `b[0][1, 2]` / `box.arr[1, 2]` 全覆盖整段，
//           接收者与方括号都在范围内。
//       (2) 本条**只管索引个数**，不管索引类型。索引类型必须是 `Int64`：不合时走 :149-160 的
//           `DiagMismatchedTypesWithFoundTy(diag, *idxExpr, ...)`（锚索引表达式本身），**不是**本条 kind；
//           `a["x"]` 实测报 `sema_mismatched_types`。这一点 varray_index01.cj 已有单点覆盖，本文件不重复。
//       (3) 入口：`ChkSubscriptExpr` 在 src/Sema/TypeCheckExpr/SubscriptExpr.cpp:41-47 先判 tuple（且索引个数 == 1）、
//           再判 `DynamicCast<VArrayTy*>(baseTy)`，故只对**接收者静态类型是 VArray**的下标表达式生效；
//           非 VArray 接收者的多索引走运算符重载脱糖路径（:48 起），不报本条。
//   - 文案 DiagnosticSema.def:216。
//
// 4) `sema_varray_in_cfunc`（CFunc 返回类型不能是 VArray）
//   - 两条**独立**入口，锚点相同（都是返回类型整段文本），触发面不同：
//       (1) src/Sema/TypeCheckType.cpp:331-339 `CheckCFuncReturnType(const Type& type)`：先判
//           `!Ty::IsMetCType(*type.ty)` → 报 `sema_invalid_cfunc_return_type`；`else if (Is<VArrayTy>(type.ty))` → 报本条，
//           `DiagnoseRefactor(kind, type)` 锚整个返回类型节点。调用点：
//             - TypeCheckType.cpp:328 `CheckFuncType`，即 **`CFunc<…>` / `@C` 函数类型语法**（`ft.isC` 才继续）；
//             - TypeCheckType.cpp:256 `CheckCFuncType`，即显式写出的 `CFunc<…>` 类型注解。
//           所以 `type X = CFunc<(Int32) -> VArray<…>>` 走这条（varrayCfuncReturnTypeAlias.cj）。
//       (2) src/Sema/FFI/CFFICheck.cpp:281-301 `UnsafeCheck(const FuncBody& fb)`：`fb.funcDecl` 带 `FOREIGN` 或 `C`
//           属性时，先判 `!Ty::IsTyCorrect(fb.retType->ty)` 直接 return（:292-294），否则 `IsMetCType` 假 → 报
//           `sema_invalid_cfunc_return_type`，真且是 `VArrayTy` → :299 报本条，
//           第三个实参 `GetFuncBodyRange(fb, *fb.retType)`（CFFICheck.cpp:148-155）在返回类型有源码位置时
//           等价于 `MakeRange(type.GetBegin(), type.GetEnd())`，**无源码位置时**才退化成函数名一个 token
//           （推导出来的返回类型）。调用点 src/Sema/TypeChecker.cpp:267。
//           `foreign func` 与 `@C func`（有函数体）都走这条（varrayCfuncReturnForeign.cj /
//           varrayCfuncReturnCFunc.cj）。注意 :292 的早退：函数体返回语句类型不符时 `fb.retType->ty` 会被判为
//           不正确，本条**不报**（实测证据见下方"已删除形态"）。
//   - 文案 DiagnosticSema.def:217。
//
// 本项目 CFIR 名（cfir/checkers/checkers-component-generator/.../diagnostics/CfirDiagnosticsList.kt）：
//   VARRAY_EXTRA 组（`DiagnosticGroup("VArrayExtra")`，:1875-1889，注释写明"对齐 C++ sema_varray_args_number_mismatch
//   等原有 TYPE_CHECK 未覆盖的条目"）：
//     :1877 VARRAY_ARGS_NUMBER_MISMATCH  ← 官方 sema_varray_args_number_mismatch，无参数
//     :1880 VARRAY_SUBSCRIPT_NUM         ← 官方 sema_varray_subscript_num，无参数
//     :1883 VARRAY_IN_CFUNC              ← 官方 sema_varray_in_cfunc，无参数
//     :1886 VARRAY_ARG_TYPE_WITH_REFTYPE ← 官方 sema_varray_arg_type_with_reftype，parameter<ConeCangJieType>("type")
//   另有 :940 VARRAY_SIZE_MISMATCH（官方 sema_varray_size_match）属另一组，已由 diagnostics2/varray/varraySizeMismatch.cj
//   与 varrayArrayLiteralSizeMismatch.cj 覆盖，本文件不重复。
//   官方↔CFIR 名映射另有测试侧确认：cfir/analysis-tests/testFixtures/.../golden/DiagnosticNameMapper.kt:269-271
//   （VARRAY_IN_CFUNC→sema_varray_in_cfunc、VARRAY_SUBSCRIPT_NUM→sema_varray_subscript_num、
//   VARRAY_ARGS_NUMBER_MISMATCH→sema_varray_args_number_mismatch）。
// 文案表：cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrorsDefaultMessages.kt:1126-1129，
//   四条与官方 DiagRefactor/DiagnosticSema.def:215-218 逐字一致（VARRAY_ARG_TYPE_WITH_REFTYPE 的
//   "''{0}'' directly or indirectly contains an unsupported type" 与官方 "'%s' directly or indirectly contains an
//   unsupported type" 只差渲染占位符写法）。
//
// 生产者：
//   VARRAY_ARGS_NUMBER_MISMATCH 与 VARRAY_ARG_TYPE_WITH_REFTYPE（构造器表达式入口）
//     → cfir/checkers/src/.../checkers/expression/CfirVArrayConstructorArgChecker.kt:34-45（ARGSMISMATCH：
//       `val argCount = expression.argumentList.arguments.size; if (argCount == 1) return` →
//       `reportOn(source = expression.argumentList.source ?: expression.source, ...)`）与 :62-71（REFTYPE：
//       `expression.typeArguments.firstOrNull() as? CfirResolvedTypeRef` → `findUnsupportedVArrayElementType` →
//       `reportOn(source = elementTypeRef.originalSource() ?: source, ...)`）。
//       类注释写明"直接构造语法 `VArray<T, $N>(...)` 在 CFIR 中被建模为 synthetic function call，其元素类型 `T`
//       不再处于完整的 `CfirVArrayTypeRef` 内，因此这里补上与 `CheckVArrayType` 相同的元素类型限制入口"。
//   VARRAY_ARG_TYPE_WITH_REFTYPE（类型语法入口）
//     → cfir/checkers/src/.../checkers/type/CfirVArrayElementTypeChecker.kt:29-45（注册为
//       `CfirResolvedTypeRefChecker`，source 取 `sourceTypeRef.elementTypeRef.source ?: ...`）。
//     → 共享递归判定：cfir/checkers/src/.../checkers/CfirVArrayElementTypeUtils.kt:36-68
//       `findUnsupportedVArrayElementType`（对应官方 `CheckVArrayWithRefType`）。
//   VARRAY_SUBSCRIPT_NUM
//     → cfir/checkers/src/.../checkers/expression/CfirExpressionSemanticsChecker.kt:794-801
//       （`if (expression.indices.size != 1) reportOn(source = expression.source, ...)`）。
//   VARRAY_IN_CFUNC
//     → cfir/checkers/src/.../checkers/declaration/CfirVArrayExtraChecker.kt:37-48（注册为
//       `CfirCallableDeclarationChecker`，要求 `declaration.status.isC` 且返回类型 `is ConeVArrayType`）。
//     → 另见 cfir/checkers/src/.../checkers/declaration/CfirForeignFunctionReturnTypeChecker.kt:83。
//
// 按源码可预判的锚点差异（**未跑 CFIR**，只写源码能直接看出的；CFIR 与官方不一致的缺口由主会话记入 README，
// 本文件一律按官方范围写标记）：
//   (1) VARRAY_ARGS_NUMBER_MISMATCH 锚点粒度：官方锚**圆括号内的实参列表**（`MakeRange(ve.leftParenPos,
//       ve.rightParenPos + 1)`，不含 `VArray` 与类型实参）；CFIR 锚 `expression.argumentList.source ??
//       expression.source`，即实参列表或整个调用表达式。形态 1 / 2 的实参列表与调用表达式同起点同终点，
//       故预计一致；但若 `argumentList.source` 缺失而回退到 `expression.source`，则会**多覆盖** `VArray<Int64, $3>` 前缀而红。
//   (2) VARRAY_ARG_TYPE_WITH_REFTYPE 锚点粒度：官方锚 `*vt.typeArgument`（元素类型整段）；CFIR 两个生产者都取
//       `elementTypeRef.source`，粒度应当一致。差异点在**递归报告的类型**：官方 MainHint 附加实参是
//       `CheckVArrayWithRefType` 返回的**成员字段类型**（形如 "contain unsupported instance member variable with
//       type '…'"），而 CFIR 的 `findUnsupportedVArrayElementType` 返回的是命中的**类型本身**；官方主文案用
//       `Ty::ToString(typeArgTy)` 渲染成 `Class-VCRClass` 这类前缀形式。两者文案渲染器不同，属已知偏差，
//       本文件只对锚点与 kind 负责。
//   (3) VARRAY_ARG_TYPE_WITH_REFTYPE 覆盖面：官方 `CheckVArrayWithRefType` 把标准库 `Array` 按
//       `IsStructArray()` 直接判为引用类型；CFIR 的 `findUnsupportedVArrayElementType` 用
//       `ConeStructType` + `StdlibClassIds.Array / StdlibClassIds.String` 显式列举（并额外把 `String` 也判为不可用，
//       官方则只在递归进入 `String` 的实例字段时才命中）——`String` 这一格属可预判的规则差异，本文件不写 `String` 形态。
//   (4) VARRAY_ARG_TYPE_WITH_REFTYPE 报告次数：官方 `CheckVArrayWithRefType` 命中即 return（一个 VArray 类型节点最多一条），
//       CFIR 同样返回首个命中类型；但形态"同一类型在两个位置各出现一次"时官方会报两条（两个独立的 VArrayType 节点），
//       CFIR 逐个 `CfirResolvedTypeRef` 各报一条，条数应一致。
//   (5) VARRAY_SUBSCRIPT_NUM 锚点粒度：官方锚**整个下标表达式**；CFIR `reportOn(expression.source)` 同样是整个
//       下标表达式，形态 9 / 10 / 11 预计一致。
//   (6) VARRAY_SUBSCRIPT_NUM 覆盖面：官方要求接收者静态类型是 `VArrayTy` 才进 `ChkVArrayAccess`；CFIR
//       `checkVArraySubscript` 由 `expression.receiver.coneTypeOrNull` 分派到 `ConeVArrayType` 分支，
//       分派口径一致，但 receiver 类型不可得（推导失败）时官方会因 `!Ty::IsTyCorrect(baseTy)` 提前返回、CFIR 会静默跳过。
//   (7) VARRAY_IN_CFUNC 覆盖面：官方 `UnsafeCheck`（CFFICheck.cpp:281）在 `FOREIGN` / `C` 两种属性下触发，
//       且 :292 对返回类型不正确的函数体**早退**；`CheckCFuncReturnType`（TypeCheckType.cpp:331）只在
//       `CFunc<…>` 类型语法处触发。CFIR `CfirVArrayExtraChecker` 用 `declaration.status.isC` 统一判定，
//       `CfirForeignFunctionReturnTypeChecker.kt:83` 另有一处。因此形态 18 / 19 / 20 三条路径
//       （`foreign func`、`@C func`、`CFunc<…>` 类型别名）CFIR 是否各报一条，属可预判的覆盖面差异，
//       特别是 `foreign func` 是否被 `status.isC` 覆盖需实跑确认。
//
// 已有覆盖不重复清单（下列都是 grep 过 cfir/analysis-tests/testData 后确认已存在的，本文件不重写）：
//   - `sema_varray_size_match`（CFIR VARRAY_SIZE_MISMATCH）：diagnostics2/varray/varraySizeMismatch.cj（VArray 类型
//     之间赋值/传参/返回的长度不符）与 diagnostics2/varray/varrayArrayLiteralSizeMismatch.cj（数组字面量形态）已覆盖，
//     另有旧目录 diagnostics/varray/varraySizeMismatchRich.cj / varrayArrayLiteralSizeMismatchRich.cj。
//   - `VARRAY_ARG_TYPE_WITH_REFTYPE` 的**元素类型种类**（interface / enum / 泛型形参 / 普通函数类型 / `String` /
//     struct 实例字段间接 / tuple 分量间接 / struct 递归 / 递归环）已由
//     macro/llt/varray/varray_with_reftype/varray_with_reftype01-06.cj 覆盖；llt/varray/varray06.cj / varray09.cj 也覆盖了
//     `let` 局部变量注解与嵌套位置。本文件只补这些文件**没有**的**类型位置**矩阵（形态 5-14）。
//   - `VARRAY_ARGS_NUMBER_MISMATCH`：llt/varray/varray10.cj 已有 `VArray<Int64,$3>(repeat: 1, {i => i})`（2 个实参）与
//     `VArray<Int64,$3>()`（0 个实参）。本文件形态 1 / 2 与之形态相同但锚点更短、且作为语义矩阵文件的独立段保留，
//     并额外补 `varray10` 未写的具名实参形式与"尾随 lambda 不算多实参"边界（形态 3）。
//   - `VARRAY_SUBSCRIPT_NUM`：llt/varray/varray_index01.cj 已有单点形态 `a[1, 2]`。本文件形态 9 与之同形，
//     但额外补嵌套下标（形态 10）与成员访问接收者（形态 11）两个位置。
//   - `VARRAY_IN_CFUNC`：llt/ffi/semantic_contract/cfuncSignaturesNegative.cj（`type VArrayReturn = CFunc<() -> VArray<Int64,$2>>`）、
//     llt/ffi/semantic_contract/ctypeParametersNegative.cj（`foreign func returnArray(): VArray<Int64,$2>`）与
//     macro/llt/varray/varray_cffi/varray_ctype02.cj（`foreign func` / `@C func` / `CFunc<…>` 变量 / `CPointer<VArray<…>>`
//     的正反两面）已覆盖。本文件形态 16 / 17 / 18 与它们同源，作为 diagnostics2 语义矩阵的独立段保留。
//   - `sema_invalid_subscript_expr`（在 VArray 上做下标但元素类型无下标能力）不在本文件范围，见下方"已删除形态"。
//   - 本族不涉及的数组字面量具名实参规则（`sema_array_first_arg_cannot_be_named` /
//     `sema_array_second_arg_cannot_be_named` / `sema_array_second_wrong_named_arg`，
//     src/Sema/TypeCheckBuiltinExpr.cpp:778-803 `ChkArrayArgs`）是**另一套**规则：它只作用于 `ArrayExpr` 的具名实参
//     （`ChkArrayExpr` 末尾调用），而 `ChkVArrayArg` 对 VArray 只认 `repeat`、认错名报 `sema_unknown_named_argument`。
//     两条入口互不相交，本文件按第 4 条形态（无 CFIR 名）记录 `sema_unknown_named_argument`。
//
// ── 形态 1：VArray 构造器**零个实参**（`VArray<Int64, $3>()`）
// kind：sema_varray_args_number_mismatch "'VArray' constructor accepts only one argument"
// 锚点：圆括号内的实参列表 `()`，2 个字符。Range (2,30)..(2,32)。探针：形态 1。
// FILE: varrayCtorZeroArgs.cj
func VCRZeroArgs(): Int64 {
    let a = VArray<Int64, $3><!VARRAY_ARGS_NUMBER_MISMATCH!>()<!>
    a[0]
}

func vcrMainS1() { 0 }

// ── 形态 2：VArray 构造器**两个位置实参**（`VArray<Int64, $3>(1, 2)`）——注意锚点不含 `VArray<Int64, $3>` 前缀
// kind：sema_varray_args_number_mismatch；锚点：实参列表 `(1, 2)`，6 个字符。Range (2,30)..(2,36)。探针：形态 2。
// FILE: varrayCtorTwoArgs.cj
func VCRTwoArgs(): Int64 {
    let a = VArray<Int64, $3><!VARRAY_ARGS_NUMBER_MISMATCH!>(1, 2)<!>
    a[0]
}

func vcrMainS2() { 0 }

// ── 形态 3（合法对照）：VArray 构造器写**尾随 lambda、连括号都不写**（`VArray<Int64, $3> {i => i}`）
// 依据 TypeCheckBuiltinExpr.cpp:378-382：尾随 lambda 进 `ve.args[0]->name.Empty()` 分支，按 `(Int64) -> T` 检查，
// 实参个数仍是 1，所以**不报** sema_varray_args_number_mismatch。实测 1.0.5 零 error、零 warning（各段末尾的收尾函数已按主会话要求从 `main()` 改名为唯一的 `func vcrMainS<n>()`，否则同包多段同名 `main` 会触发 CFIR 的 `REDEFINITION_ENTRY`；改名后 1.0.5 不再有 `chir_dce_unused_function_main` 警告）。
// 探针：形态 3。llt/varray/varray09.cj 里同形的 `VArray<Int64, $3> {i => i}` 配的是 TYPE_MISMATCH，
// 那是 `Array<Int64>` 目标类型注解导致、且加括号的两种写法，不是本条 kind 的形态。
// FILE: varrayCtorTrailingLambda.cj
func VCRTrailingLambda(): Int64 {
    let a = VArray<Int64, $3> {i => i}
    let b = VArray<Int64, $3> {i => i + 1}
    a[0] + b[1]
}

func vcrMainS3() { 0 }

// ── 形态 4（SUGGESTED，无内联标记）：VArray 构造器实参写成 `item:`（错的具名实参名）
// kind：sema_unknown_named_argument（DiagnosticSema.def:100 "unknown named argument prefix '%s:'"）+ Note
//   "expect the name of the named parameter is 'item'"。src/Sema/TypeCheckBuiltinExpr.cpp:383-389。
// 锚点：实测 Range (2,31)..(2,32)，**只有 `item` 一个字符**。注意这是 `diag.Diagnose(*ve.args[0], kind, ...)`
//   走 include/cangjie/Basic/DiagnosticEngine.h:843-857 的 node overload，末尾 `return Diagnose(node.GetBegin(), kind)`
//   退化成 GetBegin 一字符（**不是** refactor kind 的整段锚点；本族四个 kind 全在 DiagRefactor 里，故它们的锚点
//   都是整段）。
// 本项目 CFIR 名：CfirDiagnosticsList.kt 里**没有** `sema_unknown_named_argument` 的条目（最接近的
//   :401 NAMED_PARAMETER_NOT_FOUND 在 DiagnosticNameMapper.kt:153 对应的是官方 `sema_named_parameter_not_found`，
//   另一个 kind），故按规则记为 SUGGESTED 段：段内不写内联标记。
// 探针：形态 4。llt/varray/varray06.cj:13 已有 `VArray<Int64,$3>(itxx: 0)` 的 NAMED_PARAMETER_NOT_FOUND 单点形态，
//   但那是 CFIR 侧名字、锚点策略是 NAME_OF_NAMED_ARGUMENT，与本段官方 kind 不同，故本段作为边界取证单独保留。
// FILE: varrayCtorWrongNamedArg.cj
func VCRWrongNamedArg(): Int64 {
    let a = VArray<Int64, $3>(item: 1)
    a[0]
}

func vcrMainS4() { 0 }

// ── 形态 5：VArray 元素类型是 class，且出现在**类字段声明**的类型注解上
// kind：sema_varray_arg_type_with_reftype "'Class-VCRClassS4' directly or indirectly contains an unsupported type"
// 锚点：整个类型实参 `VCRClassS4`，7 个字符。Range (4,22)..(4,30)。探针：形态 5。
// FILE: varrayElemClassField.cj
class VCRClassS5 {}

class VCRHolder {
    let slot: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>VCRClassS5<!>, $2> = [VCRClassS5(), VCRClassS5()]
}

func vcrMainS5() { 0 }

// ── 形态 6：同一规则出现在**函数返回类型**上（SynFunc 的返回类型走通用 TYPE post-visit，TypeChecker.cpp:1089）
// 锚点：`VCRClassS5`，Range (3,23)..(3,31)。探针：形态 6。
// FILE: varrayElemReturn.cj
class VCRClassS6 {}

func VCRRet(): VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>VCRClassS6<!>, $2> {
    [VCRClassS6(), VCRClassS6()]
}

func vcrMainS6() { 0 }

// ── 形态 7：同一规则出现在**函数形参类型**上（函数体故意不解引用元素，避免叠加 sema_invalid_subscript_expr）
// 锚点：`VCRClassS6`，Range (3,29)..(3,37)。探针：形态 7。
// FILE: varrayElemParam.cj
class VCRClassS7 {}

func VCRParam(items: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>VCRClassS7<!>, $2>): Int64 {
    let _ = items
    0
}

func vcrMainS7() { 0 }

// ── 形态 8：同一规则出现在**元组类型的分量**上（TUPLE_TYPE 分支 → CheckTupleType，TypeCheckType.cpp:300-309；
//   本条本身来自 VARRAY_TYPE 分支，元组只是提供外层类型位置的可达性）
// 锚点：`VCRClassS7`，Range (3,41)..(3,49)。探针：形态 8。
// 与 interop/tupleFieldCTypeRules.cj 形态 18 不重复：那里是 `VArray<(…)>`（VArray 的元素是元组）触发
//   INVALID_TUPLE_FIELD_CTYPE；这里是元组分量里嵌 VArray、触发 VARRAY_ARG_TYPE_WITH_REFTYPE，kind 与嵌套方向都不同。
// FILE: varrayElemTupleComponent.cj
class VCRClassS8 {}

func VCRTupleParam(pair: (Int64, VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>VCRClassS8<!>, $2>)): Int64 {
    let _ = pair
    0
}

func vcrMainS8() { 0 }

// ── 形态 9：同一规则出现在 **`Array<VArray<…>>` 的元素类型**上（REF_TYPE → GetTypeArgs 递归，TypeCheckType.cpp:32-35）
// 锚点：`VCRClassS8`，Range (3,39)..(3,47)。探针：形态 9。
// FILE: varrayElemArrayElement.cj
class VCRClassS9 {}

func VCRArrayElem(items: Array<VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>VCRClassS9<!>, $2>>): Int64 {
    let _ = items
    0
}

func vcrMainS9() { 0 }

// ── 形态 10：同一规则出现在 **prop（属性访问器）类型**上
// 锚点：`VCRClassS9`，Range (4,24)..(4,32)。探针：形态 10。
// 注意官方 `CheckVArrayWithRefType` 只看 struct 的 VAR_DECL 实例字段（TypeCheckType.cpp:87 跳过
// `astKind != VAR_DECL`），所以 prop 自身不会被当成"间接含引用类型"的来源；这里 prop 的**类型注解**仍会独立触发本条。
// FILE: varrayElemProp.cj
class VCRClassS10 {}

class VCRPropHost {
    prop items: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>VCRClassS10<!>, $2> {
        get() {
            [VCRClassS10(), VCRClassS10()]
        }
    }
}

func vcrMainS10() { 0 }

// ── 形态 11：同一规则出现在**类型别名定义**上（CheckTypeAlias，TypeCheckDecl.cpp:698-700）
// 锚点：`VCRClassS10`，Range (3,24)..(3,32)。探针：形态 11。官方用 `IS_CHECK_VISITED` 保证同一别名只查一次，
// 故别名定义点与使用点合计只有一条；本段只定义不使用，避免与使用者混淆。
// FILE: varrayElemTypeAlias.cj
class VCRClassS11 {}

type VCRAlias = VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>VCRClassS11<!>, $2>

func vcrMainS11() { 0 }

// ── 形态 12：同一规则出现在**泛型实参**里（`VCRBox<VArray<VCRClassS11, $2>>`，逐 typeArg 递归）
// 锚点：`VCRClassS11`，Range (10,39)..(10,47)。探针：形态 12。
// FILE: varrayElemGenericArg.cj
class VCRClassS12 {}

class VCRBox<T> {
    let item: T
    init(value: T) {
        item = value
    }
}

func VCRGenericArg(box: VCRBox<VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>VCRClassS12<!>, $2>>): Int64 {
    let _ = box
    0
}

func vcrMainS12() { 0 }

// ── 形态 13：同一规则出现在**函数类型的形参**上（FUNC_TYPE 分支 → CheckFuncType，TypeCheckType.cpp:311-319）
// 锚点：`VCRClassS12`，Range (3,34)..(3,42)。探针：形态 13。
// FILE: varrayElemFuncTypeParam.cj
class VCRClassS13 {}

func VCRFuncTypeParam(f: (VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>VCRClassS13<!>, $2>) -> Unit): Int64 {
    f([VCRClassS13(), VCRClassS13()])
    0
}

func vcrMainS13() { 0 }

// ── 形态 14：同一规则出现在**泛型上界**上（CheckUpperBound，TypeCheckGeneric.cpp:28 / :34）
// 锚点：`VCRClassS13`，Range (3,53)..(3,61)。探针：形态 14。
// 伴随诊断（本段按 SUGGESTED 记录、段内不写标记）：`sema_upper_bound_must_be_class_or_interface`
//   "the upper bound 'VArray<Class-VCRClassS13, $2>' of generic parameter 'T' …"，锚在类型形参名 `T` 一字符
//   （Range (3,15)..(3,16)）。`CheckReferenceTypeLegality` 在 `CheckUpperBound` 里先跑，所以本族与上界合法性诊断同段共存；
//   UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE 已由 diagnostics2/generic/genericBoundRules.cj、
//   repeatedBoundTypeParameter.cj、conflictingUpperBounds.cj、onlyOneClassBoundAllowed.cj 覆盖，此处只作伴随登记。
// FILE: varrayElemUpperBound.cj
class VCRClassS14 {}

func VCRBound<T>(value: T): Int64 where T <: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>VCRClassS14<!>, $2> {
    let _ = value
    0
}

func vcrMainS14() { 0 }

// ── 形态 15：VArray 下标**两个索引** `a[1, 2]`——锚整个下标表达式（含接收者）
// kind：sema_varray_subscript_num "'VArray' accepts exactly one subscript index with type of 'Int64'"
// 锚点：`a[1, 2]`，7 个字符。Range (3,5)..(3,12)。探针：形态 15。llt/varray/varray_index01.cj:19 有同形单点。
// FILE: varraySubscriptTwoIndices.cj
func VCRSubTwo(): Int64 {
    let a = VArray<Int64, $3>(repeat: 0)
    <!VARRAY_SUBSCRIPT_NUM!>a[1, 2]<!>
}

func vcrMainS15() { 0 }

// ── 形态 16：**嵌套** VArray 的内层下标多索引 `b[0][1, 2]`——本条只报内层这一个表达式
// 锚点：`b[0][1, 2]`，10 个字符。Range (3,5)..(3,15)。探针：形态 16。
// 依据：外层 `b[0]` 索引个数是 1、类型合法，不报；内层接收者 `b[0]` 静态类型是 `VArray<Int64,$2>` 才进 ChkVArrayAccess。
// FILE: varraySubscriptNested.cj
func VCRSubNested(): Int64 {
    let b = VArray<VArray<Int64, $2>, $2>(repeat: [1, 2])
    <!VARRAY_SUBSCRIPT_NUM!>b[0][1, 2]<!>
}

func vcrMainS16() { 0 }

// ── 形态 17：**成员访问**作为接收者 `box.arr[1, 2]`——锚点从接收者首字符起，不从 `[` 起
// 锚点：`box.arr[1, 2]`，13 个字符。Range (6,5)..(6,18)。探针：形态 17。
// FILE: varraySubscriptMemberAccess.cj
class VCRMemberBox {
    let arr: VArray<Int64, $3> = [1, 2, 3]
}

func VCRSubMember(box: VCRMemberBox): Int64 {
    <!VARRAY_SUBSCRIPT_NUM!>box.arr[1, 2]<!>
}

func vcrMainS17() { 0 }

// ── 形态 18：`foreign func` 的返回类型是 VArray（CFFICheck.cpp:281 UnsafeCheck 的 FOREIGN 分支）
// kind：sema_varray_in_cfunc "return type of CFunc cannot be 'VArray' type"
// 锚点：返回类型整段 `VArray<Int64, $2>`，17 个字符。Range (1,43)..(1,60)。探针：形态 18。
// 与 macro/llt/varray/varray_cffi/varray_ctype02.cj 的 `foreign func c_f1` 同源；llt/ffi/semantic_contract/
//   ctypeParametersNegative.cj 的 `foreign func returnArray()` 同源。
// FILE: varrayCfuncReturnForeign.cj
foreign func VCRForeignRet(count: Int32): <!VARRAY_IN_CFUNC!>VArray<Int64, $2><!>

func vcrMainS18() { 0 }

// ── 形态 19：`@C func`（**有函数体**）的返回类型是 VArray（同 UnsafeCheck 的 C 属性分支）
// 锚点：`VArray<Int64, $2>`，Range (2,33)..(2,49)。探针：形态 19。
// 要点：函数体必须真的返回合法 VArray，否则 CFFICheck.cpp:292 的 `!Ty::IsTyCorrect(fb.retType->ty)` 会早退、
// 本条不报（探针取证见"已删除形态"）。
// FILE: varrayCfuncReturnCFunc.cj
@C
func VCRCFuncRet(count: Int32): <!VARRAY_IN_CFUNC!>VArray<Int64, $2><!> {
    return [0, 0]
}

func vcrMainS19() { 0 }

// ── 形态 20：**`CFunc<…>` 类型语法**的返回类型是 VArray（CheckFuncType → CheckCFuncReturnType，TypeCheckType.cpp:328/:337）
// 锚点：`VArray<Int64, $2>`，Range (1,39)..(1,56)。探针：形态 20。
// 与形态 18 / 19 是**不同入口**：这里走类型检查路径而不是 UnsafeCheck，所以不要求声明带 FOREIGN / C 属性。
// llt/ffi/semantic_contract/cfuncSignaturesNegative.cj:5 的 `type VArrayReturn = CFunc<() -> VArray<Int64, $2>>` 同源。
// FILE: varrayCfuncReturnTypeAlias.cj
type VRCCFuncAlias = CFunc<(Int32) -> <!VARRAY_IN_CFUNC!>VArray<Int64, $2><!>>

func vcrMainS20() { 0 }

// ── 形态 21（合法对照）：VArray 构造器与下标的三种合法写法
// `VArray<Int64,$3>(repeat: 1)`（具名 repeat 实参）、`VArray<Int64,$3>({i => i + 1})`（括号内 lambda）、
//   `VArray<Int64, $3> = [1, 2, 3]`（数组字面量，长度与 `$3` 一致，故不触发 sema_varray_size_match）；
//   下标 `a[0]` / `b[1]` / `c[2]` 索引个数为 1 且类型 Int64，不触发本族任何 kind。
// 探针：形态 21。实测 1.0.5 零 error、零 warning（收尾函数已改名为唯一的 `func vcrMainS<n>()`，理由同形态 3 的注释）。
// FILE: varrayLegalForms.cj
func VCRLegalCtor(): Int64 {
    let a = VArray<Int64, $3>(repeat: 1)
    let b = VArray<Int64, $3>({i => i + 1})
    let c: VArray<Int64, $3> = [1, 2, 3]
    a[0] + b[1] + c[2]
}

func vcrMainS21() { 0 }

// ── 形态 22（合法对照）：`CFunc` 返回非 VArray、且**形参**是 VArray（VArray 允许做 CFunc 形参，只禁返回）
// 依据 TypeCheckType.cpp:333-337：`CheckCFuncReturnType` 只在返回类型是 VArray 时报错；形参侧的
//   `CheckCFuncParamType`（CFFICheck.cpp:309 起）判的是 CType 合法性，`VArrayTy` 的 `IsMetCType` 为真
//   （Types.cpp:865-872），故 `VArray` 形参合法。macro/llt/varray/varray_cffi/varray_ctype02.cj 的
//   `foreign func c_f4(a: CPointer<VArray<...>>)` 与本段是同一正反两面。
// 探针：形态 22。实测 1.0.5 / 1.1.3 均零 error。本段只做**声明**不做调用、不取函数引用（原因见"已删除形态"第 6 条）。
// FILE: varrayLegalCfuncParam.cj
type VCRLegalCfuncAlias = CFunc<(Int32, VArray<Int32, $3>) -> Int32>

foreign func VCRLegalCfuncForeign(data: VArray<Int32, $3>): Int32

func VCRLegalCfuncUse(data: VArray<Int32, $3>): Int32 {
    data[0]
}

func vcrMainS22() { 0 }

// ── 已删除形态（探针取证，未保留独立段）：
//   1) `a[]`（VArray 零个下标索引）：**解析期**就失败，`parse_expected_expression` "expected expression after '[',
//      found ']'"，Range (3,7)..(3,8)，语义检查根本不执行，所以拿不到 sema_varray_subscript_num。
//      源码上 `ChkVArrayAccess` 的 `se.indexExprs.size() != 1` 确实能覆盖 size == 0，但 Cangjie 语法不允许空方括号，
//      该分支在本语言里不可达。探针：初稿 varraySubscriptNoIndex.cj。
//   2) `a["x"]`（单个索引但类型不是 Int64）：报的是 `sema_mismatched_types`（锚 `"x"`），**不是**
//      sema_varray_subscript_num —— 印证 SubscriptExpr.cpp:149-160 只管索引个数。llt/varray/varray_index01.cj:20
//      已有同形单点，本文件不重复。探针：初稿 varraySubscriptWrongIndexType.cj。
//   3) 在 VArray 上解引用元素（`items[0][0]`、`items[0][0][0]`、`box.item[0][0]`）：元素类型是 class 时额外叠加
//      `sema_invalid_subscript_expr` "invalid subscript operator [] on type 'Class-VCRClassS22' …"（锚整个解引用表达式）。
//      该 kind 不在本文件范围（属于下标运算符族），故对应段改为不触碰元素（`let _ = …`）。探针：初稿形态 7 / 9 / 12。
//   4) `@C func` 返回 VArray 但函数体返回语句类型不符（`{ count }`）：只报 `sema_mismatched_types`，
//      **不报** sema_varray_in_cfunc —— 印证 CFFICheck.cpp:292 对 `!Ty::IsTyCorrect(fb.retType->ty)` 的早退。
//      故形态 19 的函数体改为 `return [0, 0]`。探针：初稿 varrayCfuncReturnCFunc.cj。
//   5) `import std.collection.*`：不需要（`VArray` 是内建类型），加上只多一条 `sema_unused_import` 警告，
//      Range (1,1)..(1,1)。本机两版实测：无 import 可编译通过。探针：D:/tmp/cjprobe/importcheck。
//   6) `CFunc<…>` 变量**调用**（`f(1, [0, 0, 0])` where `f: CFunc<(Int32, VArray<Int32,$3>) -> Int32>`）：
//      叠三条无关诊断 `sema_no_match_function_declaration_for_ref` / `sema_inout_mismatch` /
//      `sema_unsafe_function_invoke_failed`（CFunc 的形参按 `inout` 传递，且需在 unsafe 块里调用），
//      与本族无关，故形态 22 只做声明不做调用。探针：初稿 varrayLegalCfuncParam.cj。
//
// 本文件的 SUGGESTED 段共 2 个：形态 4（sema_unknown_named_argument，本项目无对应 CFIR 名）与形态 14
//   （sema_upper_bound_must_be_class_or_interface，已有覆盖）；段内不写内联标记。
// 另有形态 19 这段需要注意：官方对 `VArray` 形参 + `CFunc` 形参调用会叠加 inout / unsafe 诊断，见"已删除形态"第 6 条。
// 除此之外 18 个非法形态（形态 1 / 2 / 5-20）全部有内联标记，另有 3 个合法对照段（形态 3 / 21 / 22）。