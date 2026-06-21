

struct R<T> {
    public static func funca(a: Int32): Int32 {
        return a
    }
}

let a = <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>R<!>.funca
var b = <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>R<!>.funca

main(): Int64 {
    return Int64(a(1) + b(1) - 2)
}
