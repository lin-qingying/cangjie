// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
<!PARSE_SPECIFIC_IN_NON_SPECIFIC_FILE!>package cjmp_gate<!>

public common func commonApi(): Int64 { 1 }
public specific func misplacedSpecific(): Int64 { 1 }
