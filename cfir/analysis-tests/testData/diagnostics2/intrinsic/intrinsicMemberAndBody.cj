// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 / 1.1.3 实测（kind parse_intrinsic_function_cannot_have_body / parse_intrinsic_function_must_be_toplevel
//   / parse_duplicated_intrinsic_function / sema_invalid_intrinsic_decl）：
//   成员函数上的 `@Intrinsic`（1.0.5 语法是 `@Intrinsic func`，1.1.3 才接受 `@Intrinsic getTypeForTypeParameter`）
//     → error: intrinsic function must be toplevel scope（锚整条成员声明，不含 `@Intrinsic` 注解行）
//     官方在这条之外还报 parse_intrinsic_function_cannot_have_body（锚函数体），与 bodyIntrinsic.cj 同因。
//   `@Intrinsic` 函数带函数体 → error: intrinsic function cannot have body（锚函数体 `{ ... }`）
//   同名 intrinsic 重复声明 → error: duplicated intrinsic function 'getTypeForTypeParameter'
//     （锚**第二个**声明的 `func` 关键字首字母 1 个字符；官方在两条之后各给一条 cannot_have_body，因为函数不能带体）
//   非 std 包的**无函数体**顶层 `@Intrinsic`
//     → error: intrinsic function 'helper' cannot be declared in 'default' package（锚 `func` 关键字首字母 1 个字符）
//   非 std 包的**带函数体**顶层 `@Intrinsic` → 官方只报 cannot_have_body（parse 阶段先失败，语义阶段不再跑），
//     拿不到 sema_invalid_intrinsic_decl，故 INVALID_INTRINSIC_DECL 用例必须无函数体。
// 本项目：官方 kind 分别对应 INTRINSIC_FUNCTION_MUST_BE_TOPLEVEL、INTRINSIC_FUNCTION_CANNOT_HAVE_BODY、
//   INTRINSIC_FUNCTION_DUPLICATED、INVALID_INTRINSIC_DECL。范围口径：官方对声明级 kind 的窄锚（GetBegin
//   1 字符 / 锚 `func` 关键字）按项目 Diagnostic Range Policy 不镜像，CFIR 锚"注解 + 签名"整段
//   （与 Kotlin FIR declaration.source 含注解的行为一致），INVALID_INTRINSIC_DECL 等标记按整段写。
//   已知 CFIR 与官方的语义差异：member / duplicated 段 CFIR 额外报 INTRINSIC_FUNCTION_CANNOT_HAVE_BODY
//   （官方 parse 错误互斥不级联），属 CFIR 待对齐项，不是本文件标记口径问题。
// 合法对照：同名的非 intrinsic 类成员函数零诊断；官方对 `extend` / 成员上的 `public` 另给 parse_redundant_modifier
//   warning，合法对照里不写 `public`。顶层函数不能沿用同名：本包内 duplicatedIntrinsic.cj 的两个
//   `@Intrinsic func getTypeForTypeParameter` 已是顶层函数，同包同签名顶层重名官方报
//   `sema_overload_conflicts` "function 'getTypeForTypeParameter' has overload conflicts"（锚第二个声明的函数名），
//   CFIR 对应 CONFLICTING_OVERLOADS，所以顶层合法对照改名。

// FILE: memberIntrinsic.cj
class C {
    @Intrinsic
    <!INTRINSIC_FUNCTION_MUST_BE_TOPLEVEL!>func getTypeForTypeParameter(): Int64 {
        return 1
    }<!>
}

// FILE: bodyIntrinsic.cj
@Intrinsic
func withBody(): Int64 <!INTRINSIC_FUNCTION_CANNOT_HAVE_BODY!>{
    return 1
}<!>

// FILE: duplicatedIntrinsic.cj
@Intrinsic
func getTypeForTypeParameter(): Int64 {
    return 1
}

@Intrinsic
<!INTRINSIC_FUNCTION_DUPLICATED!>f<!>unc getTypeForTypeParameter(): Int64 {
    return 2
}

// FILE: nonStdIntrinsic.cj
<!INVALID_INTRINSIC_DECL!>@Intrinsic
func helper(): Int64<!>

// FILE: legalControl.cj
class D {
    func getTypeForTypeParameter(): Int64 {
        return 1
    }
}

func legalGetTypeForTypeParameter(): Int64 {
    return 2
}
