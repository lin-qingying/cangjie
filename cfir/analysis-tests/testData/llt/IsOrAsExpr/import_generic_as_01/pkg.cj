// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

class A {
    public func test<T>(): Option<T> {
        return 1 as T
    }
}

class B<T> {
    public func test(): Option<T> {
        return 2 as T
    }
}

public func test<T>() {
    let a = A()
    var v = a.test<Int64>()
    if ((v ?? 0) != 1) {
        return 1
    }
    let b = B<Int64>()
    v = b.test()
    if ((v ?? 0) != 2) {
        return 2
    }
    return 0
}
