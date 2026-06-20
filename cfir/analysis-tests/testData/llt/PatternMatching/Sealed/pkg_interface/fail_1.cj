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

// FILE: fail_1.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


internal import pkga.*

enum <!CANNOT_INHERIT_SEALED!>Color<!> <: I { // error
    RED | GREEN | BLUE

    public func f() {
        2
    }
}

main() {}
