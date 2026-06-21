

// 自增减的变量定义在全局，使用在局部

main(): Int64 {
    var b = foo()
    if (b != 2) {
        return 1
    }
    return 0
}

var res: Int32 = 1

func foo() {
    var a = res
    res++ // error: usage of a variable 'res' which is not defined yet
    res
}
