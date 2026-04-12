// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

open class B {
    public func test<T>(a: T) {
        a
    }
}

public class A<T> {
    public func test(a: T) {
        return B().test(a)
    }
    public func foo<X>(b: X) {
        b
    }
    public func goo<T>(x: T, y!: T = x): T {
        y
    }
}

public class C {
    public func foo<X>(b: X) {
        b
    }
}
