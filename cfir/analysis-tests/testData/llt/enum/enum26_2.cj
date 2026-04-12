// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

enum A<T> {
    a(T)

    public func test(x: A<T>): A<T> {
        x
    }
    public func foo() {
        0
    }
    public func test2() {
        this.foo()
        foo()
    }
}

func test<T>() {
    var a: Option<Int64> = 1
}

enum B {
    b

    public func foo() {
        0
    }
    public func test() {
        this.foo()
        foo()
    }
}

main(): Int64 {
    var i = A<Int64>.a(1)
    var x: A<Int64> = i.test(i)
    test<Int64>()
    return b.test()
}
