// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

interface I<<!sema_upper_bound_must_be_class_or_interface!>T<!>> where T <: UIntNative {
    prop b1: T {
        get() {
            <!sema_cannot_convert_literal!>1<!>
        }
    }
}

extend<T> Option<T> <: I<UIntNative> {}

main() {
    var u1: I<UIntNative> = Some(1)
    return u1.b1 - 1
}
