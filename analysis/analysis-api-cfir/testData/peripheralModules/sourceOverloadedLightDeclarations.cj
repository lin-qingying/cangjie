// MODULE: sourceOverloadedLightDeclarations
// MAIN_MODULE
// MAIN_FILE_NAME: sourceOverloadedLightDeclarations.cj
// FILE: sourceOverloadedLightDeclarations.cj
package sample.peripheral

func overload(): Int64 {
    return 42
}

func overload(value: Int64): Int64 {
    return value
}
