// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 / 1.1.3 实测（kind parse_conflict_modifier）：
//   `public static open func f()` → error: 'open' and 'static' modifiers conflict on function declaration
//     （主锚 `open` 1 个字符，OtherHint 指向 `static` 与 `func`；只有这一条 error）
//   `public OverloadedCtor()` + `public init()` 同名构造器 → error: constructor 'init' has overload conflicts
//     （kind sema_overload_conflicts，锚 `init`）
// 本项目：官方第一条对应 INCOMPATIBLE_MODIFIERS；第二条对应 CONFLICTING_OVERLOADS。
// 分文件的原因：官方第一条是 parser 阶段诊断，parse 失败后不再进入语义阶段，
// 两条放同一文件时 `OverloadedCtor` 拿不到 overload 诊断（cjc 实测）。
// 合法对照：单个 static 成员、单个构造器零诊断。

// FILE: conflictModifier.cj
class StaticOpen {
    public static <!INCOMPATIBLE_MODIFIERS!>open<!> func f(): Int64 {
        return 1
    }
}

// FILE: overloadConflict.cj
class OverloadedCtor {
    public OverloadedCtor() {
    }

    public <!CONFLICTING_OVERLOADS!>init<!>() {
    }
}

// FILE: legalControl.cj
class StaticOnly {
    public static func f(): Int64 {
        return 1
    }
}

class NormalCtor {
    public NormalCtor() {
    }
}
