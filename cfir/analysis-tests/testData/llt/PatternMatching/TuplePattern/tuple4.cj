

let a: (Int32, Unit) = (0, ())
var b: ((Int8, UInt32), Int64, Unit, Int32) = ((0, 0), 0, (), 0)

main() {
    match ((b, a, 4, 8)) {
        case <!UNREACHABLE_PATTERN!>(_, _, _: Int32, _)<!> => 0
        case _ => 1
        case <!UNREACHABLE_PATTERN!>(((0, 1), 2, (), 3), _, 4, 5)<!> => 2
    }
}
