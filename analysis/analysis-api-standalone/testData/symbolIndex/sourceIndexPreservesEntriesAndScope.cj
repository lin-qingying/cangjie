// MODULE: unrelated
// FILE: unrelated.cj
package index.unrelated

public func indexedOutside(): Int64 { 0 }

// MODULE: dependency
// FILE: first.cj
package index.first

public class IndexedWidget {}
public func indexedLoad(): Int64 { 1 }
public func indexedLoad(value: Int64): Int64 { value }
public type IndexedAlias = IndexedWidget
public enum IndexedShape {
    | IndexedDot
    | IndexedDot(Int64)
}

// FILE: second.cj
package index.second

public class IndexedWidget {}
public func indexedLoad(): Int64 { 2 }

// MODULE: main(dependency)
// MAIN_MODULE
// FILE: main.cj
package index.main

func entry(): Int64 { 0 }
