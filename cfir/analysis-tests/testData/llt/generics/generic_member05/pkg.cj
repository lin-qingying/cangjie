// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

enum B {
    B1

    public func test<T>(a: T) {
        a
    }
}

public enum A<T> {
    A1

    public func test(a: T) {
        return B1.test(a)
    }
    public func foo<X>(b: X) {
        b
    }
}

public enum C {
    C1

    public func foo<X>(b: X) {
        b
    }
}

public func test() {
    if (A1<Int64>.test(123) != 123) {
        return 1
    }
    if (A1<Int64>.foo(234) != 234) {
        return 2
    }
    if (C1.foo("23") != "23") {
        return 3
    }
    return 0
}
