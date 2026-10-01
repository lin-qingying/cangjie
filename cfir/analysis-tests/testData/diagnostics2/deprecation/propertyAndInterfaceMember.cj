// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：
//   父类 `@Deprecated["old", strict: true] public open prop p` + 子类 `override prop p` 无注解
//     → error: overridden property 'p' should be marked with @Deprecated（锚整条 prop 声明）
//   接口成员 `@Deprecated["old", strict: true] func f()` + 实现类 `func f()` 无注解
//     → error: overridden function 'f' should be marked with @Deprecated（锚整条函数声明）
//   官方 kind：sema_deprecation_override_error（父成员非 strict 时同一形态给 warning kind sema_deprecation_override_warning）。
//   注意：接口成员若写显式 `public`，官方 cjc 1.0.5 / 1.1.3 都会另报 parse_redundant_modifier warning，故接口成员不写 `public`。
// FILE: propertyMember.cj
open class A {
    private var store: Int64 = 0

    public init() {
        store = 0
    }

    @Deprecated["old", strict: true]
    public open prop p: Int64 {
        get() {
            return store
        }
    }
}

class B <: A {
    private var store: Int64 = 0

    public init() {
        store = 0
    }

    <!DEPRECATION_OVERRIDE_ERROR!>public override prop p: Int64 {
        get() {
            return store
        }
    }<!>
}

// FILE: interfaceMember.cj
interface I {
    // 接口成员隐式 public：官方 cjc 对接口成员上的显式 `public` 报 parse_redundant_modifier warning，此处不写。
    @Deprecated["old", strict: true]
    func f(): Int64
}

class C <: I {
    <!DEPRECATION_OVERRIDE_ERROR!>public func f(): Int64 {
        return 1
    }<!>
}
