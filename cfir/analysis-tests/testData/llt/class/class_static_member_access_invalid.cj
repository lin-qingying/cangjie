// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A {
    public static var a: Int64 = 1
}

class B <: A {}

main(): Int64 {
    var b: Int64 = <!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>B<!>().a
    B.a
    return 0
}
