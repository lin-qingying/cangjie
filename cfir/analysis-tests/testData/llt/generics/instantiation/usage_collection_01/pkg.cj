// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

class Base {}

class A<T> {
    var v: ?T = None
}

public class B {
    var b: ?A<Base> = None
    func test() {
        if (let Some(a) <- b) {
            a.v
        }
    }
}

public open class B0 {
    var b: B
    init(a: B) {
        b = a
    }
}

public class B1<T> <: B0 {
    init(a: B) {
        super(a)
    }
    public func test(): Unit {
        b.b
    }
}

public func foo<T>() {
    return B1<T>(B())
}
