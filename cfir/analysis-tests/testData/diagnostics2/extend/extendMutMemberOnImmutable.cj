// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：
//   `extend Int64 { public mut func bump(): Unit {} }` → error: 'mut' modifier is illegal in extend body of 'Int64'（锚 `mut` token）
//   `extend Int64 { public mut prop doubled: Int64 { get() {...} set(v) {...} } }` → 同一条错误（锚 `mut` token）；
//     官方在这条之外还报 sema_property_must_have_accessors（锚整条 prop），本项目对应 PROPERTY_MUST_HAVE_ACCESSORS，未在此用例覆盖。
//   合法对照：`struct S` 的 extend 中 `mut prop` 零诊断（官方只禁止 primitive 与非 struct 目标上的 mut 成员）。
// 官方实现 DeclAttributeChecker::CheckExtendAttribute：extendedDecl 不是 struct（或目标是 primitive）且成员带 mut 时报错。
// FILE: structExtend.cj
struct S {
    var x: Int64

    init() {
        x = 0
    }
}

extend S {
    public mut prop y: Int64 {
        get() {
            return x
        }

        set(v) {
            x = v
        }
    }
}

// FILE: primitiveExtend.cj
extend Int64 {
    public <!MUT_ONLY_ON_FUNCTION!>mut<!> func bump(): Unit {
    }
}
