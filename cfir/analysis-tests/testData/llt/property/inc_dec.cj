// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

struct R {
    var a: Int64 = 0
    mut prop p: Int64 {
        get() {
            a
        }
        set(v) {
            a = v
        }
    }
    mut func inc() {
        p++
    }
    mut func dec() {
        p--
    }
}

main() {
    var r = R()
    r.p++
    if (r.a != 1) {
        return 1
    }
    r.p--
    if (r.a != 0) {
        return 2
    }
    r.inc()
    if (r.a != 1) {
        return 3
    }
    r.dec()
    if (r.a != 0) {
        return 4
    }
    return 0
}
