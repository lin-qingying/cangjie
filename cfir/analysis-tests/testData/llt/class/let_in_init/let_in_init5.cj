

class A {
    public let a: Int32
    public A(aa: Int32) {
        this.a = aa
    }
}

main() {
    let x: Int32 = 10
    var a = A(x)
    if (a.a != 10) {
        return 1
    }
    return 0
}
