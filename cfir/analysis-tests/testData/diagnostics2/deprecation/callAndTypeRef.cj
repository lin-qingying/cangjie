// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 / 1.1.3 实测（`@Deprecated` 合法参数集为 message / since / strict）：
//   调用 `strict: true` 的函数 → error: function 'legacy' is deprecated since 1.0.0. old.
//     （kind sema_deprecated_error，锚被调用者 `legacy`）
//   返回类型引用同级别名 → error: class 'ReturnRef' is deprecated since 1.0.0. old.（锚类型名 `ReturnRef`；用前置 Option 形式 `?ReturnRef`
//     避免再报隐式构造调用的弃用诊断；postfix `T?` 在 cjc 1.0.5 / 1.1.3 上是 parse 错误）
//   构造调用同级别名 → 同上（锚 `CtorRef`，不含 `()`）
// 三个使用点必须拆到不同文件（各自用不同的类名）：声明了 `init()` 的类在 cjc 1.0.5 上是 parse 错误
//   （unexpected constructor in 'top-level' scope），放同一文件里取不到诊断；同名类放同一包也会 redefinition。
// 本项目：CFIR CfirDeprecatedCallChecker / CfirDeprecatedTypeRefChecker 读 STATUS 阶段发布的 `deprecationsProvider`，
//   `strict: true` 映射为 CfirErrors.DEPRECATED_ERROR。

// FILE: call.cj
@Deprecated["old", since: "1.0.0", strict: true]
func legacy(): Int64 {
    return 1
}

func callDeprecated(): Int64 {
    return <!DEPRECATED_ERROR!>legacy<!>()
}

// FILE: typeRef.cj
@Deprecated["old", since: "1.0.0", strict: true]
class ReturnRef {}

func useDeprecatedType(): ?<!DEPRECATED_ERROR!>ReturnRef<!> {
    return None
}

// FILE: ctorCall.cj
@Deprecated["old", since: "1.0.0", strict: true]
class CtorRef {}

func useDeprecatedCtor(): ?<!DEPRECATED_ERROR!>CtorRef<!> {
    return <!DEPRECATED_ERROR!>CtorRef<!>()
}
