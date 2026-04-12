// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

interface I {
    func h1(): Int64
    func h2() {
        h1()
    }
}

struct A<T> <: I {
    public func h1() {
        2
    }
}

public func foo<T>(v: T) {
    let a = A<T>()
    a.h2()
}
