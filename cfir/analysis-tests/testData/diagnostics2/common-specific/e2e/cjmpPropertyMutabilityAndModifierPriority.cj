// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 两阶段验证：property MUT 缺失和多出分别锚 specific/common；MUT 优先于可见性差异。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_property_mutability_priority

public common struct MutCases {
    public common init()

    public common mut prop missingMut: Int64 {
        get() { 1 }
        set(value) { let _ = value }
    }

    <!PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT!>public common prop extraMut: Int64 {
        get() { 2 }
    }<!>

    public common mut prop missingMutBeforeVisibility: Int64 {
        get() { 3 }
        set(value) { let _ = value }
    }

    <!PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT!>private common prop extraMutBeforeVisibility: Int64 {
        get() { 4 }
    }<!>
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_property_mutability_priority

public specific struct MutCases {
    public specific init() {}

    <!PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT!>public specific prop missingMut: Int64 {
        get() { 1 }
    }<!>

    public specific mut prop extraMut: Int64 {
        get() { 2 }
        set(value) { let _ = value }
    }

    <!PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT!>private specific prop missingMutBeforeVisibility: Int64 {
        get() { 3 }
    }<!>

    public specific mut prop extraMutBeforeVisibility: Int64 {
        get() { 4 }
        set(value) { let _ = value }
    }
}
