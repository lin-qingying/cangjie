// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I {
    static func f(a!: Int32): Int32 {
        a
    }
}

class A <: I {
    public static func <!PARAM_NAMED_MISMATCHED!>f<!>(a: Int32): Int32 {
        a + 1
    }
}

main() {
    A.f(0)
}
