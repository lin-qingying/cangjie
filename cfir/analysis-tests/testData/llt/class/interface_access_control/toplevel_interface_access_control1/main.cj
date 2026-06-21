// FILE: pkg/pkg.cj


package pkg

public interface I {
    static func x(): Int64 {
        return 1
    }
}

public class Test <: I {}

// FILE: main.cj



internal import pkg.*

main(): Int64 {
    var x: Int64 = Test.x()
    if (x == 1) {
        return 0
    }
    return 1
}
