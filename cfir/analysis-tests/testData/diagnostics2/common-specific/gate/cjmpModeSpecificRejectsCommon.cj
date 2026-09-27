// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_gate

public common func commonApi(): Int64 { 1 }
public common func specificApi(): Int64 { 1 }

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
<!PARSE_COMMON_IN_NON_COMMON_FILE!>package cjmp_gate<!>

public specific func specificApi(): Int64 { 1 }
public common func misplacedCommon(): Int64 { 1 }
