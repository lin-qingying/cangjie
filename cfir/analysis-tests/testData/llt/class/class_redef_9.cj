// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class C1 {
    public static func foo(a: Int64): Int64 {
        return 2
    }
}

class C2 <: C1 {
    public static redef func <!OVERRIDING_RETURN_TYPE_MISMATCH!>foo<!>(a: Int64): Int32 {
        return 3
    }
}

main(): Int64 {
    return 1
}
