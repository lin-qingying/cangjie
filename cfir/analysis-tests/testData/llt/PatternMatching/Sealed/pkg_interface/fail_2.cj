// FILE: pkgs.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkgs

sealed interface I {
    static func f() {
        1
    }
}

// FILE: fail_2.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


import pkgs.*

struct <!CANNOT_INHERIT_SEALED!>A<!> <: I {}

main(): Int64 {
    return A.f() - 1
}
