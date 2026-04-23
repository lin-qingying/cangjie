// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface IA<T> {
    func goo(): Unit
}

abstract class A<T> <: IA<T> {
    public func foo() {
        println("foo")
    }

    public open func goo() {
        println("goo")
    }

    public func bar() {
        println("bar")
    }
}

class B <: A<B> {}

func test<T>(t: T) where T <: IA<T> {
    t.goo()
}

main() {
    var b = B()
    test(b)
}
