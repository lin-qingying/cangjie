// LANGUAGE_VERSION: 1.1.0
// MODULE: main
// MAIN_MODULE
// MAIN_FILE_NAME: anchor.cj
// FILE: anchor.cj
package cjmp_standalone_anchor
public func anchor(): Int64 { 0 }

// FILE: common.cj
package cjmp_standalone
public common func value(): Int64

// FILE: specific.cj
package cjmp_standalone
public specific func value(): Int64 { 42 }
public func use(): Int64 { value() }
