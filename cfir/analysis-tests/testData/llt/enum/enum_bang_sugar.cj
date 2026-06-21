

class C {
    public var item = 100
}

func f2() {
    let err = Exception()
    let c2 = Option<C>.None
    let r2 = c2.getOrThrow().item // return Result<Int64>.Err(err)
    Some(r2)
}

main() {
    0
}
