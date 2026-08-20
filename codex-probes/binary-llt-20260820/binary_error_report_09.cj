class Test {
    operator func +(other: Test) {
        return this + other  // recursive call without return type
    }
}

main() {
    let a = Test()
    let b = Test()
    let c = a + b
}
