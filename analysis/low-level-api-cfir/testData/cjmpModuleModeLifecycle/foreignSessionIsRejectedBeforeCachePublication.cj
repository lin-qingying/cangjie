// LANGUAGE_VERSION: 1.1.0
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_module_lifecycle

public common func value(): Int64

// MODULE: explicitNone()()(common)
// CJMP_MODE: NONE
// FILE: disabled.cj
package cjmp_module_lifecycle_disabled

public func plain(): Int64 { 1 }

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package cjmp_module_lifecycle

public specific func value(): Int64 { 42 }
