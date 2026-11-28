// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// common/specific property：本体与 getter 都参与配对（官方 MatchCJMPProp 对本体与访问器分别 TrySetSpecificImpl）。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_prop

public common class Item {
    public common init()

    public common prop value: Int64 {
        get() { 1 }
    }
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_prop

public specific class Item {
    public specific init() {}

    public specific prop value: Int64 {
        get() { 2 }
    }
}
