

func f(x: String) {
    match (x) {
        case <!INTERPOLATION_IN_CONST_PATTERN!>"${x.size}"<!> => 0
        case _ => 1
    }
}

main() {
    f("1")
}
