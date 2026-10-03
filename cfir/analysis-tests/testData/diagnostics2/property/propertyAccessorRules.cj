// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 / 1.1.3 实测（kind sema_immutable_property_with_setter "immutable property cannot have setter"）：
//   非 mut `prop` 同时写 get/set → 锚**整条 prop 声明**（从 `public` 到 `}`）
//   非 mut `prop` 只写 set → 同一 kind、同样锚整条 prop 声明
//   合法对照：只写 get 的 `prop`、无访问器的 `prop` 均零诊断。
// 官方实现 DeclAttributeChecker：非 `var` 属性带 setter 时报 sema_immutable_property_with_setter。
// 注意：接口成员隐式 public（官方对显式 `public` 给 parse_redundant_modifier warning），故 interface F 里的 prop 不写 public。

// FILE: immutableSetter.cj
class C {
    private var store: Int64 = 0

    public init() {}

    <!IMMUTABLE_PROPERTY_WITH_SETTER!>public prop p: Int64 {
        get() {
            return store
        }

        set(v) {
            store = v
        }
    }<!>
}

// FILE: setterOnly.cj
class D {
    private var store: Int64 = 0

    <!IMMUTABLE_PROPERTY_WITH_SETTER!>public prop q: Int64 {
        set(v) {
            store = v
        }
    }<!>
}

// FILE: getterOnly.cj
class E {
    public prop r: Int64 {
        get() {
            return 1
        }
    }
}

// FILE: abstractProp.cj
abstract class F {
    public prop s: Int64
}

interface G {
    prop t: Int64
}
