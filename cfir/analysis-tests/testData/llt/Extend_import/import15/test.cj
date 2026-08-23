
// FILE: a.cj


package test

import std.collection.ArrayList

public class Foo <: Equatable<Foo> {
    public let id: String

    init(id: String) {
        this.id = id
    }

    public operator func ==(that: Foo): Bool {
        return id == that.id
    }
}

public class Foos {
    public let foos: ArrayList<Foo> = ArrayList()
}

// FILE: test.cj



package test

main(): Int64 {
    let a: Foos = Foos()

    a.foos.toArray()
    a.foos.contains(Foo("1"))
    return 0
}

