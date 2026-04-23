// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class A {}

class B <: A {}

class C {}

extend C {
    static func <!EXTEND_MEMBER_CANNOT_SHADOW!>f<!><A>(a: A) {}
}

extend C {
    static func <!EXTEND_MEMBER_CANNOT_SHADOW!>f<!><X>(a: X) {}
}
