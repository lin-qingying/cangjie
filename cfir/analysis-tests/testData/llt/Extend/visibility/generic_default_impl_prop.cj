// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: UIntNative {
    prop b1: T {
        get() {
            <!CANNOT_CONVERT_LITERAL!>1<!>
        }
    }
}

extend<T> Option<T> <: I<UIntNative> {}

main() {
    var u1: I<UIntNative> = Some(1)
    return u1.b1 - 1
}
