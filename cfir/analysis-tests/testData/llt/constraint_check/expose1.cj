// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class Bar1 {
    public let member: Int32 = 111
}

func Foo1<T>(a: T) where T <: Bar1 {
    var b: Int32 = a.member
    return b
}

open class Bar2<T> {
    public let member: Int32 = 222
}

func Foo2<U>(a: U) where U <: Bar2<U> {
    var b: Int32 = a.member // Filed access by expose to assumption.
    return b
}

class C1 <: Bar1 {}

class C2 <: Bar2<C2> {}

main() {
    var a: Int32 = Foo1<C1>(C1())
    var b: Int32 = Foo2<C2>(C2())
    var f = Foo2<C2>
    var c = f(C2())
    if (a == 111 && b == 222 && c == 222) {
        return 0
    }
    return 1
}
