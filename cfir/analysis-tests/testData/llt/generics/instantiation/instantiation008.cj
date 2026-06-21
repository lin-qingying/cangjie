


class Tester<T> {
    init(x: T) {}
}

class Test {
    func f(): This {
        this
    }

    operator func !(): This {
        return f()
    }

    // Avoid duplicat instantiation for Tester<This> and Tester<Test>
    func test_instantiation() {
        let x = Tester(f())

        let y = Tester(this)

        let z = Tester(!this)

        let w = Tester(Test())
    }
}

main() {
}
