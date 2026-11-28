// LANGUAGE_VERSION: 1.1.0
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_ll_uncalled_member_first

public class Item {}

common extend Item {
    public common func used(): Int64
    public common func unused(): Int64
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// MAIN_FILE_NAME: caller.cj
// FILE: caller.cj
package cjmp_ll_uncalled_member_first

public func caller(item: Item): Int64 { item.used() }

// FILE: callee.cj
package cjmp_ll_uncalled_member_first

// 两个成员必须属于**同一个** specific extend：官方 `MergeCJMPExtensions` 以"被扩展类型 +
// 接口集 + 泛型约束"为键，同键的 specific extend 是重复声明（sema_specific_has_duplicate_extensions），
// 后一个不会与 common 建立配对。
specific extend Item {
    public specific func used(): Int64 { 1 }
    public specific func unused(): Int64 { 2 }
}
