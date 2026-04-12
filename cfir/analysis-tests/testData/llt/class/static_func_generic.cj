// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC-PIPE-0: %run %run_opt %n.%suffix

open class Parent {
    public static func Check<T1, T2>(arg: T1): Int64 {
        0
    }
}

open class Sub<T> <: Parent {
    public static func Check(arg: T): Int64 {
        1
    }
}

main(ina: Array<String>) {
    if (Sub<Int32>.Check(Int32(1)) != 1) {
        return 1
    }
    if (Parent.Check<Parent, Parent>(Parent()) != 0) {
        return 2
    }
    0
}
