

struct A {
    let b: B = B()
    let d: Option<B> = B()
}

struct B {
    let c: Int64 = 0
}

func test1() {
    var a = Some([0, 0])
    (a?[0], a?[1]) = (1, 2)
    match (a) {
        case Some(arr) => arr[0] + arr[1] - 3
        case None => -1
    }
}

func test2() {
    let struct_a_opt = Some(A())
    let struct_b_opt = Some(A())
    let struct_a = A()
    let x: Option<Int64>
    let y: Option<Int64>
    let z: Option<Int64>
    (x, y, z) = (struct_a_opt?.b.c, struct_a_opt?.b.c, struct_a.d?.c)

    var res = match (x) {
        case Some(t) => t
        case None => 1
    } + match (y) {
        case Some(t) => t
        case None => 1
    } + match (z) {
        case Some(t) => t
        case None => 1
    }
    return res
}

main() {
    test1() + test2()
}
