// FILE: functionReference.cj
// TARGET_NAME: greet
// EXPECTED_SYMBOL_CLASS: CaCfirNamedFunctionSymbol
// EXPECTED_SYMBOL_NAME: greet
// EXPECTED_ORIGINAL_PSI_CLASS: CjNamedFunction
package sample.symbols.reference

func greet(value: Int64): Int64 {
    return value
}

func consume(): Int64 {
    return greet(1)
}
