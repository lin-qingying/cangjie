

class A {}

func test(): Int64 {
    let a = A()
    var b = A()
    var res = match (b) {
        case _ => -1
    }
    if (res == 0) {
        return 0
    }
    return 1
}

main(): Int64 {
    return test()
}
