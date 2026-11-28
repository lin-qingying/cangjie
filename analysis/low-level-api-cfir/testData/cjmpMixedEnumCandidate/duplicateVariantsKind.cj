// LANGUAGE_VERSION: 1.1.0
// Evidence: review-20261002/mixed-boundaries/duplicatePayload.
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package mixed_duplicate_payload

public common enum E { A(Int64) }

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// MAIN_FILE_NAME: specific.cj
// FILE: specific.cj
package mixed_duplicate_payload

public specific enum E { A(Int64) | A(Int64) }

// FILE: unrelated.cj
package mixed_duplicate_payload

public func unrelated(): Int64 { 41 }
