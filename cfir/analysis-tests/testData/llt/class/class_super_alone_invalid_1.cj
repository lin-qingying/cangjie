// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class C0 {}

open class C1 <: C0 {
    public var x = <!ILLEGAL_THIS_OR_SUPER_CALL!>super<!>
}

main(): Int64 {
    return 1
}
