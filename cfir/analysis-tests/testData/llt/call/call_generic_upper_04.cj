// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

var str = ""

interface V {}

interface S {
    static func name(): String {
        return "S"
    }
}

interface I<T> where T <: S {
    func foo(a: T): (T) -> Unit {
        str = T.name()
        return {v: T => ()}
    }
}

open class A <: S & V {
    public static func name(): String {
        return "A"
    }
}

class B <: A {
    public static redef func name(): String {
        return "B"
    }
}

class C <: I<A> & I<B> {
    func test(): Int64 {
        let v1 = foo(A())
        if (str != "A") {
            return 1
        }
        let v2 = foo(B())
        if (str != "B") {
            return 2
        }
        return 0
    }
}

main() {
    let c = C()
    return c.test()
}
