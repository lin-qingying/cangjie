// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class A {
    public var x: Int64 = 0
    public operator func [](index: Int64, value!: Int64): Unit {
        this.x = index + value
    }
}

main(): Int64 {
    let a = A();
    a[2] = 5
    if (a.x != 7) {
        return 1;
    }
    return 0;
}
