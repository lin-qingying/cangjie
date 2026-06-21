

class A {
    func foo(): This <!TYPE_MISMATCH!>{
        if (1 > 2) {
            this
        } else {
            this
        }
    }<!>

    func foo2(): This {
        <!TYPE_MISMATCH!>(if (1 > 2) {
            this
        } else {
            this
        }).foo()<!>
    }
}
