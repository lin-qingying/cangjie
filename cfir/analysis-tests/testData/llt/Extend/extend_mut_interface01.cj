// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I {
    mut prop p: Int64 {
        get() {
            1
        }
        set(_) {}
    }
}

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend Int64 <: I {}<!>

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend Bool <: I {}<!>

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend Unit <: I {}<!>

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend Nothing <: I {}<!>

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend String <: I {}<!>

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend<T> Range<T> <: I {}<!>

enum A {
    | AAA
}

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend A <: I {}<!>

main() {}
