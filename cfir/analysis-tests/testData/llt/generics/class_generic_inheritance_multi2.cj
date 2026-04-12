// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class A<T> {
    public var a: T
    public init(n: T) {
        a = n
    }
}

class B<T> <: A<T> {
    public init(a: T) {
        super(a)
    }
}

main(): Int64 {
    var b = B<Int32>(1);
    return 0;
}
