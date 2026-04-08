// FILE: patternBindingReference.cj
// TARGET_NAME: value
// EXPECTED_SYMBOL_CLASS: CaCfirPatternBindingSymbolImpl
// EXPECTED_SYMBOL_NAME: value
// EXPECTED_ORIGINAL_PSI_CLASS: CjBindingPattern
package sample.symbols.reference

func consume(): Int64 {
    let value = 1
    return value
}
