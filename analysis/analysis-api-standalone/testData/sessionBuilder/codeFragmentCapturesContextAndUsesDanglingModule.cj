// MODULE: main
// MAIN_MODULE
// FILE: codeFragmentCapturesContextAndUsesDanglingModule.cj
package sample.standalone.fragment

func context(x: Int64): Int64 {
    let y = x + 1
    return y
}
