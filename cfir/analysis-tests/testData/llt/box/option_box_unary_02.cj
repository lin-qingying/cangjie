// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

class A {
    operator func -() {
        1
    }
}

main() {
    var count = 0
    let a: ?ToString = -(Some(A()) ?? A())
    if (let Some(v) <- a) {
        if (v.toString() != "1") {
            return 1
        }
        count++
    }
    let b: ?ToString = -(Some(11) ?? 0)
    if (let Some(v) <- b) {
        if (v.toString() != "-11") {
            return 2
        }
        count++
    }
    let c: ?ToString = !1
    if (let Some(v) <- c) {
        if (v.toString() != "-2") {
            return 3
        }
        count++
    }
    if (count != 3) {
        return 4
    }
    return 0
}
