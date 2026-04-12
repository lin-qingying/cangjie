// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

enum E<T, K> {
    EE(K) | EEE
}

type X<K> = E<Int32, K>

type Y<K> = E<Int32, Int8>

main() {
    let a = X.EE(11)
    var a1: E<Int32, Int8> = X.EE(11)
    let b = Y.EE(12)
    a1 = b // Confirm type equal
    match (a) {
        case EE(v) =>
            if (v != 11) {
                return 1
            }
        case _ => ()
    }
    match (b) {
        case EE(v) =>
            if (v != 12) {
                return 2
            }
        case _ => ()
    }
    let c = Y.EEE
    let d: E<Int32, Int8> = X.EEE
    var e = X<Int8>.EEE
    e = d // Confirm type equal
    e = c
    return 0
}
