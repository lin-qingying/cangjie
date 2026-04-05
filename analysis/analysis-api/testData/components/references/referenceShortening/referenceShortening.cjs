// FILE: referenceShortening.cjs
// EXPECTED_REFERENCE_SHORTENING_OPERATION: sample.buildValue()|buildValue|DIRECT|-

package sample

func buildValue(): Int64 {
    return 1
}

func consume(): Int64 {
    return sample.buildValue()
}
