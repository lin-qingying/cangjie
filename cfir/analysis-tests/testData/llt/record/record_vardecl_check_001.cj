// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct R {
    public var a: C = C()
}

struct C {
    public var m: Int64 = 1
}

main(): Int64 {
    let r = R()
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>r.a.m<!> = 2
    return 0
}
