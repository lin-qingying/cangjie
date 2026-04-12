// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public interface I {
    func f(): Unit
}

class A <: I {
    public func f() {}
}

func test1<T>(a: T) where T <: I {
    a.f()
}

public func test2<T>(a: T) where T <: I {
    test1<T>(a)
}

public func test3<T>() {
    test2<A>(A())
}
