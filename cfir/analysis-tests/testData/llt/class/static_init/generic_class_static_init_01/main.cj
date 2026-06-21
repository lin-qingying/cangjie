// FILE: pkg.cj
package pkg

public var initCount = 0
public open class A<T> {
    static init() {
        initCount++
    }
}




// FILE: pkg1.cj
package pkg1
internal import pkg.*

let _ = A<Int64>()




// FILE: pkg2.cj
package pkg2

internal import pkg.*

let _ = A<Rune>()




// FILE: main.cj
internal import pkg.*
internal import pkg1.*
internal import pkg2.*

main() {
    let _ = (A<Int64>(), A<Rune>())
    if (initCount != 1) {
        return 1
    }
    return 0
}


