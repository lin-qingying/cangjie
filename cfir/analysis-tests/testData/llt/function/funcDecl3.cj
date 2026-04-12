// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func f(a: Unit) {}

func f() {}

func f0(a: Int32) {
    var b = a
    a + b
}

func f1(a: Int32) {
    a + 2
}

main() {
    if (f0(3) == 6 && f1(2) == 4) {
        return 0
    }
    return 1
}
