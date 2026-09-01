


enum E<T, K> {
    EE(K) | EEE
}

type X<K> = E<Int32, K>

<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>type Y<K><!> = E<Int32, Int8>

main() {
    let a = X.EE(11)
    var a1: E<Int32, Int8> = X.EE(11)
    let b = Y.EE(12)
    a1 = b // Confirm type equal
    match (a) {
        case EE(v) =>
            if (v != 11) {
                return 1
            }
        case <!UNREACHABLE_PATTERN!>_<!> => ()
    }
    match (b) {
        case EE(v) =>
            if (v != 12) {
                return 2
            }
        case <!UNREACHABLE_PATTERN!>_<!> => ()
    }
    let c = Y.EEE
    let d: E<Int32, Int8> = X.EEE
    var e = X<Int8>.EEE
    e = d // Confirm type equal
    e = c
    return 0
}
