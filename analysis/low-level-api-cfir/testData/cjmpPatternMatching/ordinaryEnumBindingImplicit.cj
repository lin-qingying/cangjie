// LANGUAGE_VERSION: 1.1.0
// Official SDK 1.1.3: review-20261003/enum-binding-header/probe.cj compiled successfully.
// MODULE: main
// MAIN_MODULE
// MAIN_FILE_NAME: main.cj
// FILE: main.cj
package enum_binding_header

public enum Single { Value(Int64) }
public let Single.Value(value): Single = Single.Value(1)

public func unrelated(): Int64 { 41 }
