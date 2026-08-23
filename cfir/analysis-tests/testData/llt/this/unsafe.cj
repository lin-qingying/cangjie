


class Test {
    func f(x: Int64): This <!TYPE_MISMATCH!>{
        unsafe {
            this
        }
    }<!>
}

