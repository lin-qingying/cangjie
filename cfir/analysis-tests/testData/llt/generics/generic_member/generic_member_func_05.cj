// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A<X> {
    public static func test<T>(x: X): X {
        println("a");
        x
    }
    public func goo<T>(x: T): T {
        println("a");
        x
    }
}

class B<X> <: A<X> {
    public static redef func <!CANNOT_OVERRIDE!>test<!><T>(x: T): T {
        println("b");
        x
    }
    public func <!CANNOT_OVERRIDE!>goo<!><Y>(x: Y): Y {
        println("b");
        x
    }
}

main() {
    0
}
