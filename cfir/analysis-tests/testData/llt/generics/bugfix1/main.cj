// FILE: pkg.cj
package pkg

public interface IA<T0> {
    static func foo(_: (Int8, Range<Int32>)) {
        false
    }
}

public open class CA<T0> <: IA<Float32> {}




// FILE: main.cj
import pkg.*

main() {
    let _ = CA<Float32>()
}
