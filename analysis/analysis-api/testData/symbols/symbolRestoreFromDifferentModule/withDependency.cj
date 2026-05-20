// MODULE: main
// MAIN_MODULE
// FILE: main.cj
package sample.symbols.restore

func marked<caret>(): Int64 {
    return 0
}

// MODULE: restore(main)
// FILE: restore.cj
package sample.symbols.restore.consumer

func restoreInContextOf() {
    <caret_restoreAt>
}
