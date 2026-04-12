// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public interface I<M> {
    static func test<T>(a: T): M
}

public class Base <: I<Base> {
    public var a = 111
    public static func test<T>(a: T): Base {
        let b = Base()
        b.a = 0
        return b
    }
}

public class A {
    public static func foo<T>(a: T): T where T <: I<T> {
        var b: T = T.test(a)
        return b
    }
}
