// MODULE: library
// MODULE_KIND: Source
// FILE: library.cj
package sample

public func buildValue(): Int64 {
    return 1
}

public func buildOther(): Int64 {
    return 2
}

// MODULE: main(library)
// MODULE_KIND: Source
// MAIN_MODULE
// FILE: shortenRange.cj
// TARGET_EXPRESSION: sample.buildValue()
// EXPECTED_REFERENCE_SHORTENING_OPERATION: sample.buildValue()|buildValue|DIRECT|-

package client

import sample
import sample.*

func consume(): Int64 {
    let left = sample.buildValue()
    let right = sample.buildOther()
    return left + right
}
