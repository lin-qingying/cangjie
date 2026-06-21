

//特别地，指定返回类型为 Unit 时（如 func f(x:Bool):Unit { x } ），则编译器会在函数体中所有 可能返回的地方自动插入表达式 return () ，使得函数的返回类型总是为 Unit
main(): Int64 {
    var a = 0
    if (case01() == ()) {
        a++
    }

    if (case04(1, 2) == ()) {
        a++
    }
    return 0
}

func case01(): Unit {
    2
}

func case04(a: Int32, b: Int32): Unit {
    if (a > b) {
        a
    } else {
        b
    }
}
