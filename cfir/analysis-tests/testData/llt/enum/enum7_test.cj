// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

enum A {
    a
}

func test() {
    let tmp = A.a
    a
    a
}

main() {
    let b = a
    a
    let c = a
    a
    let d = A.a
    a
    let e = A.a
    a
    a
    let ret1 = match (b) {
        case a => 1
    }
    let ret2 = match (c) {
        case a => 2
    }
    let ret3 = match (d) {
        case a => 3
    }
    let ret4 = match (e) {
        case a => 4
    }
    let ret5 = match (test()) {
        case a => 5
    }
    if (ret1 == 1 && ret2 == 2 && ret3 == 3 && ret4 == 4 && ret5 == 5) {
        return 0
    }
    return 1
}
