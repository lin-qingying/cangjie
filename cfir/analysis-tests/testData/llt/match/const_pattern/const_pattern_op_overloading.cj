


class A {
    public operator func ==(right: A): Bool {
        return true;
    }
}

main(): Int64 {
    let a = A()
    let b = A()

    let x: Int64 = match (a) {
        case b => 0
        case <!UNREACHABLE_PATTERN!>_<!> => 1
    }

    return x;
}
