// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class A {
    public var v = 1
}

type a = A

extend a {
    public func f() {
        this.v + v
    }
}

main() {
    var x = a()
    if (x.f() != 2) {
        return 1
    }
    return 0
}
