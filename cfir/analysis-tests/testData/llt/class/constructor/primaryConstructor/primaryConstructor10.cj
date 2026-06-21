


class Data {
    public Data(let a!: Bool = <!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER, CAPTURE_HAS_SHADOW_VARIABLE!>x<!>, var x!: Bool = a) {}
}

func run(): Int32 {
    var data: Data = Data()
    print(data.a.toString())
    print(data.x.toString())

    if (data.a && data.x) {
        return 0
    }
    return 1
}

main(): Int64 {
    return Int64(run())
}
