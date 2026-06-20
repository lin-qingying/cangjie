// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I1<T> {
    func foo1(x: T) {
        return x
    }
}

struct R1<T> <: I1<T> {
    <!INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE!>p<!>ublic mut func foo1(x: T) {
        return x
    }
}

interface I2<T> {
    mut func foo2(x: T) {
        return x
    }
}

struct R2<T> <: I2<T> {
    <!INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE!>p<!>ublic func foo2(x: T) {
        return x
    }
}

interface I3<T> {
    func foo3(x: T): Unit
}

struct R3<T> <: I3<T> {
    <!INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE!>p<!>ublic mut func foo3(x: T): Unit {
    }
}

interface I4<T> {
    mut func foo4(x: T): Unit
}

struct R4<T> <: I4<T> {
    <!INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE!>p<!>ublic func foo4(x: T): Unit {
    }
}

main() {
    return 1
}
