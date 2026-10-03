// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 / 1.1.3 实测（`interface MutInterface { public mut prop p: Int64 }`，kind parse_redundant_modifier：
//   `public` 修饰接口成员隐式 public 的写法给 redundant modifier warning，故接口成员不写 `public`）：
//   `extend Int64 <: MutInterface { public mut prop q: Int64 { get() { return 1 } } }` 的诊断：
//     sema_invalid_mut_modifier_extend_of_struct "'mut' modifier is illegal in extend body of 'Int64'"（锚 `mut` token，3 个字符）
//     sema_property_must_have_accessors（锚 `mut` token 首字母 1 个字符；与上一条同起点、不同范围，内联标记的栈式嵌套表达不了，
//       本用例不覆盖，对应本项目 PROPERTY_MUST_HAVE_ACCESSORS）
//     sema_interface_is_not_extendable "interface 'MutInterface' is not able to be extended"（锚 `extend` 首字母 1 个字符）
//     sema_need_member_implementation "implementation of function or property is needed in 'extend Int64'"（锚 `extend` 首字母 1 个字符）
//   官方 1.1.3 对 `public` 另给 parse_redundant_modifier warning @ `public`（extend 体的成员隐式 public），故 `public` 加标记。
// 本项目：官方 kind 分别对应 MUT_ONLY_ON_FUNCTION、PROPERTY_MUST_HAVE_ACCESSORS、EXTEND_INTERFACE_NOT_EXTENDABLE、
//   NEED_MEMBER_IMPLEMENTATION；两条都锚 `extend` 首字母，故合并成一个标记。CFIR 对 `mut` 统一报
//   EXTEND_IMMUTABLE_MUT_PROPERTY + MUT_ONLY_ON_FUNCTION（官方是 sema_invalid_mut_modifier_extend_of_struct 单条），
//   并把 EXTEND_INTERFACE_NOT_EXTENDABLE / NEED_MEMBER_IMPLEMENTATION 标在 `extend` 关键字整体上（实现缺口）。标记按官方范围写。
// 同文件里 struct 目标上的 mut prop 零诊断（见 extendMutMemberOnImmutable.cj）。

// FILE: primitiveExtend.cj
interface MutInterface {
    <!REDUNDANT_MODIFIER!>public<!> mut prop p: Int64
}

<!EXTEND_INTERFACE_NOT_EXTENDABLE, NEED_MEMBER_IMPLEMENTATION!>e<!>xtend Int64 <: MutInterface {
    <!EXTEND_IMMUTABLE_MUT_PROPERTY, MUT_ONLY_ON_FUNCTION!>mut<!> prop q: Int64 {
        get() {
            return 1
        }
    }
}

// FILE: needMemberOnly.cj
// 对照：官方 cjc 1.0.5 / 1.1.3 实测 `interface PlainInterface { func i(): Int64 }` +
//   `extend Int64 <: PlainInterface {}`（未实现成员）→ 只有 sema_need_member_implementation（锚 `extend` 首字母）。
interface PlainInterface {
    func i(): Int64
}

<!NEED_MEMBER_IMPLEMENTATION!>e<!>xtend Int64 <: PlainInterface {
}

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
