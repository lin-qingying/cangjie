// MODULE_KIND: Source
// FILE: unresolvedReference.cj
package sample.diagnostics

func main() {
    <caret>known()
}

func known(): Int64 {
    return 1
}

// FILE: unresolvedReference.fragment.cj
missingFromFragment()
