// MODULE: dependency
// FILE: dependency.cj
package insertion.dependency

public func insertionTarget(): Int64 { 1 }

// MODULE: main(dependency)
// MAIN_MODULE
// FILE: main.cj
package completion.sections

func insertionSite(): Int64 {
    completionTarget
}
