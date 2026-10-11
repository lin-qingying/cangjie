// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 / 1.1.3 实测（弃用信息沿 override / redef 继承，锚点是子成员整条声明，**不含** `@Deprecated` 注解行）：
//   父成员 `@Deprecated["old", strict: true]` + 子类 `override func f()` 无注解
//     → error: overridden function 'f' should be marked with @Deprecated（kind sema_deprecation_override_error）
//   同上但父成员非 strict → warning: overridden function 'f' should be marked with @Deprecated（kind sema_deprecation_override_warning）
//   父 strict + 子 `@Deprecated["old"]`（放宽） → error: strictness of @Deprecated can not be weaken on inheritors
//     （kind sema_deprecation_weakening，锚整条成员声明，不含注解行）
//   父 static 成员 strict + 子类同名 static 函数 → error: redefined function 's' should be marked with @Deprecated
//     （kind sema_deprecation_redef_error，锚整条子函数声明）
// 官方实现 DeclAttributeChecker::CheckDeprecationOfOverride / CheckDeprecationWeakening（sema_deprecation_* 家族）。
// 注意：子类成员若比父类可见性更弱（不写 `public`），官方另给 sema_weak_visibility，故子类成员写 `public`。

// FILE: strictParent.cj
open class A {
    @Deprecated["old", strict: true]
    public open func f(): Int64 {
        return 1
    }
}

class B <: A {
    <!DEPRECATION_OVERRIDE_ERROR!>public override func f(): Int64 {
        return 2
    }<!>
}

// FILE: weakening.cj
open class C {
    @Deprecated["old", strict: true]
    public open func g(): Int64 {
        return 1
    }
}

class D <: C {
    <!DEPRECATION_WEAKENING!>public override func g(): Int64 {
        return 2
    }<!>
}

// FILE: redefStatic.cj
open class E {
    @Deprecated["old", strict: true]
    public static func s(): Int64 {
        return 1
    }
}

class F <: E {
    <!DEPRECATION_REDEF_ERROR!>public static func s(): Int64 {
        return 2
    }<!>
}
