// MODULE: library
// MODULE_KIND: Source
// FILE: library.cj
package sample

public func buildValue(): Int64 {
    return 1
}

// MODULE: main(library)
// MODULE_KIND: Source
// MAIN_MODULE
// FILE: referenceShortening.cj
// EXPECTED_REFERENCE_SHORTENING_OPERATION: sample.buildValue()|buildValue|DIRECT|-

package client

import sample
import sample.*

func consume(): Int64 {
    return sample.buildValue()
}
