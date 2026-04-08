// MODULE: sourceViews
// MAIN_MODULE
// MAIN_FILE_NAME: sourceViews.cj
// FILE: sourceViews.cj
package sample.peripheral

class Greeter {
    func member(): Int64 {
        return topLevel()
    }
}

func topLevel(): Int64 {
    return 42
}
