// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I {
    static func foo(): Int64
    static func foo(a: Int64): Int64
    mut static prop a: Int64 {
        get() {
            foo()
        }
        set(x) {
            foo(x)
        }
    }
}

interface I2 <: I {}

main() {
    <!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>I2.a = 10<!>
    <!INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL!>I2.a<!>
    0
}
