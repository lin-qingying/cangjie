// MODULE: main
// MAIN_MODULE
// MAIN_FILE_NAME: source.cj
// FILE: source.cj
package cjmp_ll_dangling_import

import cjmp_ll_dangling_target.target

public func caller(): Int64 { target() }

// FILE: target.cj
package cjmp_ll_dangling_target

public func target(): Int64 { 42 }
