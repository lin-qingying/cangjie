// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %output

interface I {}

struct R <: I {
    func f<T>() {}
}

struct R2 {
    func f<T>() {}
}

main() {
    let a = R() as I
    match (a) {
        case Some(x) => ()
        case None => return 1
    }
    match (R2() as I) {
        case Some(x) => return 2
        case None => ()
    }
    return 0
}
