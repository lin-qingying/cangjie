// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class A {
    public var s1: UInt32 = 1
}

class B <: A {
    public var b1: UInt32 = 2
    public var obj: A = <!RECURSIVE_CONSTRUCTOR_CALL!>B()<!>
}

main() {
    var c: B = B()
    0
}
