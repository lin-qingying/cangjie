// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I {
    static func f2(a!: Int64, b!: Int64): Int64
}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>A<!> <: I {}

interface I2 <: I {}

class B {
    static func <!STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE!>a<!>(): Unit
}
