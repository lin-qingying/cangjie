// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output
// RUN-EXEC: %run %run_opt %output %run_args

let val = 12

interface I {
    func get(): Int64
}

class A <: I {
    public func get(): Int64 {
        val
    }
}

enum E<T> {
    None | EE(T)

    func test() {
        match (this) {
            case None => 0
            case EE(v: I) => v.get()
            case _ => 0
        }
    }
}

main() {
    let v = EE(A()).test()
    if (v != val) {
        return 1
    }
    return 0
}
