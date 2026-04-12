// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// RUN-EXEC: %run %run_opt %output %run_args

abstract class A <: ToString {}

class B <: A {
    public func toString(): String {
        return "B"
    }
}

main() {
    let a: A = B()
    let v: Any = a
    let str = match (v) {
        case t: ToString => t.toString()
        case _ => ""
    }
    if (str != "B") {
        return 1
    }
    return 0
}
