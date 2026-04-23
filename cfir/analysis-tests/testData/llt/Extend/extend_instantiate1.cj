// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A<T> {
    func foo(a: T): Unit {
    }
}

extend<T> A<T> {
    func add(a: T): T {
        foo(a)
        return a
    }
}

main() {
    let a = A<Int32>()
    a.add(1)
    0
}
