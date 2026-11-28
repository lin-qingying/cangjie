// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：给构造器加 `@Deprecated["old", strict: true]` 后 `A()`
//   → error: constructor 'init' is deprecated. old.（锚构造名 `A` 的首字母 1 个字符）
// 锚点/措辞差异：cjc 用 "constructor 'init'"，CFIR CfirDeprecatedCallChecker 走构造调用分支，
// kind 固定为 "class"，declName 取被弃用符号名（构造器自身有弃用信息时为 `init`）。
// 合法对照：未弃用类的构造调用零诊断。
class A {
    @Deprecated["old", since: "1.0.0", strict: true]
    public init() {}
}

class B {
    public init() {}
}

func useDeprecatedCtor(): A {
    return <!DEPRECATED_ERROR!>A<!>()
}

func useNormalCtor(): B {
    return B()
}
