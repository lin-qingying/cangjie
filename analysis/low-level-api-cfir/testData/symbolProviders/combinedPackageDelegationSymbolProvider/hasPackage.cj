// MODULE: library1
// MODULE_KIND: LibraryBinary
// FILE: A1.cj
package library1.foo.bar

public class A1 {}

// FILE: B1.cj
package library1.foo.bar.baz

public let b1: Int64 = 0

// FILE: C1.cj
package library.other1

public func c1() {}

// MODULE: library2
// MODULE_KIND: LibraryBinary
// FILE: A2.cj
package library2.foo.bar

public class A2 {}

// FILE: B2.cj
package library2.foo.bar.baz

public let b2: Int64 = 0

// FILE: C2.cj
package library.other2

public func c2() {}

// MODULE: library3
// MODULE_KIND: LibraryBinary
// FILE: A3.cj
package library3

public class A3 {}

// FILE: B3.cj
package library3.foo.bar.baz

public let b3: Int64 = 0

// FILE: C3.cj
package library.other3

public func c3() {}

// MODULE: main(library1, library2, library3)
// HAS_PACKAGE: <root>
// HAS_PACKAGE: library1
// HAS_PACKAGE: library1.foo
// HAS_PACKAGE: library1.foo.bar
// HAS_PACKAGE: library1.foo.bar.baz
// HAS_PACKAGE: library1.foo.bar.baz.fake
// HAS_PACKAGE: library2
// HAS_PACKAGE: library2.foo
// HAS_PACKAGE: library2.foo.bar
// HAS_PACKAGE: library2.foo.bar.baz
// HAS_PACKAGE: library2.foo.bar.baz.fake
// HAS_PACKAGE: library3
// HAS_PACKAGE: library3.foo
// HAS_PACKAGE: library3.foo.bar
// HAS_PACKAGE: library3.foo.bar.baz
// HAS_PACKAGE: library3.foo.bar.baz.fake
// HAS_PACKAGE: library
// HAS_PACKAGE: library.other1
// HAS_PACKAGE: library.other2
// HAS_PACKAGE: library.other3
// HAS_PACKAGE: library.foo
// HAS_PACKAGE: library.foo.bar
// HAS_PACKAGE: library.foo.bar.baz
// HAS_PACKAGE: library.foo.bar.baz.fake
// HAS_PACKAGE: library1.other1
// HAS_PACKAGE: library2.other2
// HAS_PACKAGE: library3.other3
// HAS_PACKAGE: mainpkg
// FILE: hasPackage.cj
package mainpkg

class Main {}

func main(): Int64 {
    return 0
}
