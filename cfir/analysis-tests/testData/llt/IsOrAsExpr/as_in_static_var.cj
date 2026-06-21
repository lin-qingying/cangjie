


class A {
    public static let opt: Option<Bool> = true as Bool
}

main(): Int64 {
    match (A.opt) {
        case Some(v) =>
            if (v) {
                return 0
            } else {
                return 1
            }
        case None => return 1
    }
}
