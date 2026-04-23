// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A <: B {
    public var a = <!USED_BEFORE_INITIALIZATION!>b<!>
}

open class B {
    public var b: Int64 = 1
}

main(): Int64 {
    return 0
}
