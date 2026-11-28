// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 / 1.1.3 实测（`interface MutInterface { public mut prop p: Int64 }`，kind parse_redundant_modifier：
//   `public` 修饰接口成员隐式 public 的写法给 redundant modifier warning，故接口成员不写 `public`）：
//   `extend Int64 <: MutInterface { public mut prop q: Int64 { get() { return 1 } } }` 的诊断：
//     sema_invalid_mut_modifier_extend_of_struct（锚 `mut` token；本项目对属性使用 EXTEND_IMMUTABLE_MUT_PROPERTY）
//     sema_property_must_have_accessors（本项目对应 PROPERTY_MUST_HAVE_ACCESSORS，锚完整属性声明）
//     sema_interface_is_not_extendable "interface 'MutInterface' is not able to be extended"（锚 `extend` 首字母 1 个字符）
//     sema_need_member_implementation "implementation of function or property is needed in 'extend Int64'"（锚 `extend` 首字母 1 个字符）
//   接口成员上的显式 `public` 对应 REDUNDANT_MODIFIER_FOR_TARGET；extend 成员不使用该目标规则。
// 本项目：属性的非法 mut 对应 EXTEND_IMMUTABLE_MUT_PROPERTY，缺少访问器对应 PROPERTY_MUST_HAVE_ACCESSORS；
//   EXTEND_INTERFACE_NOT_EXTENDABLE 锚定完整 `extend` token，NEED_MEMBER_IMPLEMENTATION 锚定完整 extend 声明。
//   函数专属的 MUT_ONLY_ON_FUNCTION 仅用于非法 mut 函数，不应用于属性。
// 同文件里 struct 目标上的 mut prop 零诊断（见 extendMutMemberOnImmutable.cj）。

// FILE: primitiveExtend.cj
interface MutInterface {
    <!REDUNDANT_MODIFIER_FOR_TARGET!>public<!> mut prop p: Int64
}

<!NEED_MEMBER_IMPLEMENTATION!><!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend<!> Int64 <: MutInterface {
    <!PROPERTY_MUST_HAVE_ACCESSORS!><!EXTEND_IMMUTABLE_MUT_PROPERTY!>mut<!> prop q: Int64 {
        get() {
            return 1
        }
    }<!>
}<!>

// FILE: needMemberOnly.cj
// 对照：官方 cjc 1.0.5 / 1.1.3 实测 `interface PlainInterface { func i(): Int64 }` +
//   `extend Int64 <: PlainInterface {}`（未实现成员）→ 只有 sema_need_member_implementation（锚 `extend` 首字母）。
interface PlainInterface {
    func i(): Int64
}

<!NEED_MEMBER_IMPLEMENTATION!>extend Int64 <: PlainInterface {}<!>

// FILE: structExtend.cj
struct SBox {
    var x: Int64 = 0
}

extend SBox {
    public mut prop y: Int64 {
        get() {
            return x
        }

        set(v) {
            x = v
        }
    }
}
