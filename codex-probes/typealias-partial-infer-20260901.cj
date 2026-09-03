enum E<T, K> {
    EE(K) | EEE
}

type X<K> = E<Int32, K>
type Y<K> = E<Int32, Int8>

main() {
    let a = X.EE(11)
    var a1: E<Int32, Int8> = X.EE(11)
    let b = Y.EE(12)
    a1 = b
    match (a) {
        case EE(v) =>
            if (v != 11) {
                return 1
            }
        case _ => ()
    }
    match (b) {
        case EE(v) =>
            if (v != 12) {
                return 2
            }
        case _ => ()
    }
    return 0
}
