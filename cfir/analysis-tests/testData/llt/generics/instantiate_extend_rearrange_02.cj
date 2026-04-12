// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// RUN-EXEC: %run %run_opt %output

interface I {
    func test(): Bool {
        match (this) {
            case v: ?Int64 => (v?.test()).getOrThrow()
            case v: Int64 => true
            case _ => false
        }
    }
}

extend Int64 <: I {}

extend<T> Option<T> <: I {}

main() {
    let a: ?Int64 = 1
    if (a.test()) {
        return 0
    }
    return 1
}
