

func foo(): Int32 {
    let a = Some<Int32>(42)
    if (let Some(v) <- a) {
        print("Ok")
        return v
    }
    let ret: Int32 = 0
    return ret
}

main() {
    let a = foo()
    if (a != 42) {
        return 1
    }
    0
}
