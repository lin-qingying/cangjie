// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3: missing binding 与 pattern root 各报 NOT_MATCHED；后续 b 成功匹配 common b。
// 证据：review-20261002/var-pattern-merge/patternFailureContinues。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_pattern_failure

public common let b: Int64

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_pattern_failure

<!NOT_MATCHED!>public specific let (<!NOT_MATCHED!>missing<!>, b) = (1, 2)<!>
