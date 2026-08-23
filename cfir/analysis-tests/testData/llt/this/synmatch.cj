


class Test {
    func f(x: Int64): This <!TYPE_MISMATCH!>{
        (match (x) {
            case _ => this
        }).g()
    }<!>

    func g(): This <!TYPE_MISMATCH!>{
        (match {
            case _ => this
        }).g()
    }<!>
}

