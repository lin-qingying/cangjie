// LANGUAGE_VERSION: 1.1.0
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_ll_enum

public common enum E<T> {
    | Empty | Value(T) | Pair(T, Int64)
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// MAIN_FILE_NAME: specific.cj
// FILE: specific.cj
package cjmp_ll_enum

public specific enum E<U> {
    | Empty | Value(U) | Pair(U, Int64)
}

// FILE: untouched.cj
package cjmp_ll_enum

public func untouched(): Int64 { 42 }
