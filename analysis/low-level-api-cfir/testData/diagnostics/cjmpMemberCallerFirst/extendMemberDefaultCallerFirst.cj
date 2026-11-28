// LANGUAGE_VERSION: 1.1.0
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_ll_member_default_first

public class Item {}

common extend Item {
    public common func value(x!: Int64 = 1): Int64
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// MAIN_FILE_NAME: caller.cj
// FILE: caller.cj
package cjmp_ll_member_default_first

public func caller(item: Item): Int64 { item.value() }

// FILE: callee.cj
package cjmp_ll_member_default_first

specific extend Item {
    public specific func value(x!: Int64): Int64 { x }
}
