// LANGUAGE_VERSION: 1.1.0
// Official SDK 1.1.3: review-20261003/explicit-tuple-header/specific.cj compiled successfully.
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package pattern_matching

public common let a: Int64
public common let b: Int64

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// MAIN_FILE_NAME: specific.cj
// FILE: specific.cj
package pattern_matching

public specific let (a, b): (Int64, Int64) = (1, 2)

// FILE: unrelated.cj
package pattern_matching

public func unrelated(): Int64 { 41 }
