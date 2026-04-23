// Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

import std.reflect.TypeInfo

interface I<T> {
    func foo(): Unit {
        println(TypeInfo.of<T>())
    }
}

class A<T> {}

class B<T> {}

extend<X> A<B<X>> <: I<X> {}

main() {
    var a: I<Int64> = A<B<Int64>>()
    a.foo()
}
