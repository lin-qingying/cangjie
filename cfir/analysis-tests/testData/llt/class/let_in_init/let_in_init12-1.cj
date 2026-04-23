// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class C <: A {
    public init() {
        a = 0
        b = 0
    }
    public init(c: Int32) {
        a = 0
        b = 0
    }
}

open class A {
    public var a: Int32 = 1
    <!CLASS_UNINITIALIZED_FIELD!>public let b: Int32<!>
}

main(): Int64 {
    return 0
}
