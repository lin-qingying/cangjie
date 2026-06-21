

// Test for compilation of instantiation
open class I1<F, K> {
    static var a: Int64 = 1
}

extend<F, K> I1<F, K> {}

class I2<K> <: I1<Int64, K> {}

<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>t<!>ype Y0<K> = I1<Int32, Int8>

type Y1<K> = I1<Int32, K>

type X0<K> = I2<K>

<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>t<!>ype X1<K> = I2<Int64>

main() {
    Y0.a
    let y0: Int64 = <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>Y1<!>.a
    let y1 = Y0<Int8>.a
    let x0 = <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>X0<!>.a
    let x1 = X0<Int8>.a
    return 0
}
