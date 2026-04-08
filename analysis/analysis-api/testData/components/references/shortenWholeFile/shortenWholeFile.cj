// FILE: shortenWholeFile.cj
// EXPECTED_REFERENCE_SHORTENING_OPERATION: sample.buildOther()|buildOther|DIRECT|-
// EXPECTED_REFERENCE_SHORTENING_OPERATION: sample.buildValue()|buildValue|DIRECT|-

package sample

func buildValue(): Int64 {
    return 1
}

func buildOther(): Int64 {
    return 2
}

func consume(): Int64 {
    let left = sample.buildValue()
    let right = sample.buildOther()
    return left + right
}
