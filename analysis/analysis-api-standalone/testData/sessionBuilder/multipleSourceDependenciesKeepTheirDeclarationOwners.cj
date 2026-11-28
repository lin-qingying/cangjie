// MODULE: left
// FILE: left.cj
package source_dependency_left
public func fromLeft(): Int64 { 1 }

// MODULE: right
// FILE: right.cj
package source_dependency_right
public func fromRight(): Int64 { 2 }

// MODULE: main(left, right)
// MAIN_MODULE
// FILE: main.cj
package source_dependency_main
import source_dependency_left.fromLeft
import source_dependency_right.fromRight
public func consume(): Int64 { fromLeft() + fromRight() }
