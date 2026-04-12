// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f
// TIMEOUT: 10

import std.collection.*

interface F1<T> {
    func foo(x: T): Bool
}

interface F2<T> {
    func foo(x: T): Bool
}

interface F3<T> {
    func foo(x: T): Bool
}

interface F4<T> {
    func foo(x: T): Bool
}

interface F5<T> {
    func foo(x: T): Bool
}

interface F6<T> {
    func foo(x: T): Bool
}

interface F7<T> {
    func foo(x: T): Bool
}

interface F8<T> {
    func foo(x: T): Bool
}

interface F9<T> {
    func foo(x: T): Bool
}

interface I1<T> <: F1<T> & F2<T> & F3<T> & F4<T> & F5<T> & F6<T> & F7<T> & F8<T> & F9<T> {}

interface I2<T> <: F1<T> & F2<T> & F3<T> & F4<T> & F5<T> & F6<T> & F7<T> & F8<T> & F9<T> {}

interface I3<T> <: F1<T> & F2<T> & F3<T> & F4<T> & F5<T> & F6<T> & F7<T> & F8<T> & F9<T> {}

interface J1<T> <: I1<Int> & I2<String> & I3<T> {}

interface J2<T> <: I1<Int> & I2<String> & I3<T> {}

interface J3<T> <: I1<Int> & I2<String> & I3<T> {}

interface J4<T> <: I1<Int> & I2<String> & I3<T> {}

interface J5<T> <: I1<Int> & I2<String> & I3<T> {}

interface J6<T> <: I1<Int> & I2<String> & I3<T> {}

interface J7<T> <: I1<Int> & I2<String> & I3<T> {}

interface J8<T> <: I1<Int> & I2<String> & I3<T> {}

interface J9<T> <: I1<Int> & I2<String> & I3<T> {}

class C <: J1<Rune> & J2<Rune> & J3<Rune> & J4<Rune> & J5<Rune> & J6<Rune> & J7<Rune> & J8<Rune> & J9<Rune> {
    public func foo(x: Int) {
        false
    }
    public func foo(x: String) {
        false
    }
    public func foo(x: Rune) {
        false
    }
}

main() {
    let v1 = {x => x.foo("123")}
    let v2 = [C()] |> any {x => x.foo(2)}
}
