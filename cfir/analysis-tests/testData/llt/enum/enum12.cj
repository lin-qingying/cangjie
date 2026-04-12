// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

enum B<T> {
    a(Bool) | bNil
}

enum A<T> {
    a(T) | aNil
}

main(): Int64 {
    var i = a<Int64>(1)
    var j = a<Int64>(true)
    let ret1 = match (i) {
        case a(x) =>
            if (x == 1) {
                1
            } else {
                0
            }
        case aNil => 0
    }
    let ret2 = match (j) {
        case a(x) =>
            if (x) {
                1
            } else {
                0
            }
        case bNil => 0
    }
    if (ret1 + ret2 == 2) {
        return 0
    }
    return 1
}
