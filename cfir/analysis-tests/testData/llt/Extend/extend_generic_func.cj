// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class A {}

extend A {
    public func test<T>(a: T) {
        return a
    }
}

main(): Int64 {
    var a = A()
    if (a.test<Int64>(11) != 11) {
        return 1
    }
    return 0
}
