// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class A {
    func test() {
        1
    }
    private func test(a: Int64) {
        2
    }
    private func test(b: Rune) {
        3
    }
}

main() {
    let a = A()
    let b = a.test
    if (b() != 1) {
        return 1
    }
    return 0
}
