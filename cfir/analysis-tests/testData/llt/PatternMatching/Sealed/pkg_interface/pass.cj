// FILE: pkga.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkga

sealed interface I {
    func f(): Int64
}

abstract sealed class A {}

extend A <: I {
    public func f() {
        0
    }
}

abstract sealed class B <: A {}

public class C <: B {}

// FILE: pass.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


internal import pkga.*

interface J {
    func g(): Int64
}

extend B <: J { // ok, sealed class can implement new interface
    public func g() {
        9
    }
}

func f(x: I) {
    <!NON_EXHAUSTIVE_MATCH!>match (x) {
        case c: B => c.g()
        case a: A => a.f()
    }<!>
}

main() {
    f(C())
}
