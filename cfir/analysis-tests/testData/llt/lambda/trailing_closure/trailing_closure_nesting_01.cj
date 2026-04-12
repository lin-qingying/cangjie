// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

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
