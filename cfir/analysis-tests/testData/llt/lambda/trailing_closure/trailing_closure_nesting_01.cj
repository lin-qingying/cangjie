


class A {
    init(a: () -> Unit) {}
    func test() {}
}

class B {
    init(b: () -> Unit) {}
    func test() {}
}

class C {
    init(c: () -> Unit) {}
    func test() {}
}

main(): Unit {
    A() {
        B() {
            C() {
                A() {
                    B() {
                        C() {
                            A() {
                                B() {
                                    C() {
                                        A() {
                                            B() {
                                                C() {
                                                    A() {
                                                        B() {
                                                            C() {
                                                                A() {
                                                                    B() {
                                                                        C() {
                                                                            
                                                                        }.test()
                                                                    }.test()
                                                                }.test()
                                                            }.test()
                                                        }.test()
                                                    }.test()
                                                }.test()
                                            }.test()
                                        }.test()
                                    }.test()
                                }.test()
                            }.test()
                        }.test()
                    }.test()
                }.test()
            }.test()
        }.test()
    }.test()
}
