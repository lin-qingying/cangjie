

let x = 4

func f1() {
    var a0 = x
    let x = 99
    func f1() {
        x
    }
    let f2 = {=> x}
    if (a0 != 4 || f1() != 99 || f2() != 99) {
        return 1
    }
    return 0
}

func f2() {
    func f1() {
        x
    }
    let f2 = {=> x}
    let x = 99
    if (f1() != 4 || f2() != 4) {
        return 1
    }
    return 0
}

main(): Int64 {
    var res = f1() + f2()
    if (res != 0) {
        return 1
    }
    return 0
}
