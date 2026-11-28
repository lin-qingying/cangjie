// LANGUAGE_VERSION: 1.1.0
// Official evidence: review-20261002/var-pattern-merge/README.md
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

public specific let (a, _, b) = (1, 2, 3)

// FILE: unrelated.cj
package pattern_matching

public func unrelated(): Int64 { 41 }
