// FILE: pkg.cj


package pkg

public class CA<T> {
    public init() {}
    init(x: Range<Int8>) {}
}

// FILE: main.cj



internal import pkg.*

main() {
    var a = CA<Range<UInt16>>()
    0
}
