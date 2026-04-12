// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class A {
    public let a: Int32
    public A(aa: Int32) {
        this.a = aa
    }
}

main() {
    let x: Int32 = 10
    var a = A(x)
    if (a.a != 10) {
        return 1
    }
    return 0
}
