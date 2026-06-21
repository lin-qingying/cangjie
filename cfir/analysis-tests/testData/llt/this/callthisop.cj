


class Test {
    operator func +(rhs: Test): This {
        return this
    }
    operator func -(): This {
        this
    }

    func f9(rhs: Test): This {
        this + rhs
    }

    func f10(): This {
        return -this
    }
}

main() {
}
