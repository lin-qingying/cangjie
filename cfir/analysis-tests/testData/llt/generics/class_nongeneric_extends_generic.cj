// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class Base<T> {
    public var member: T
    public init(a: T) {
        member = a
    }
}

class Child <: Base<Int32> {
    public init() {
        super(1)
    }
}

main() {
    var obj1: Base<Int32> = Child()
    var obj2: Child = Child()
    if (obj1.member == 1 && obj2.member == 1) {
        return 0
    }
    return 1
}
