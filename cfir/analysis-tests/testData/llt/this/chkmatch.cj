


class Test {
    func f(x: Int64): This <!TYPE_MISMATCH!>{
        return match (x) {
            case _ => this
        }
    }<!>

    func g(): This <!TYPE_MISMATCH!>{
        return match {
            case _ => this
        }
    }<!>
}

