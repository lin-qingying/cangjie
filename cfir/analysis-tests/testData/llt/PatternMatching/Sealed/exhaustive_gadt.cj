// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

abstract sealed class Expr<T> {}

class EBool <: Expr<Bool> {
    var v: Bool
    init(v: Bool) {
        this.v = v
    }
}

class EInt64 <: Expr<Int64> {
    var v: Int64
    init(v: Int64) {
        this.v = v
    }
}

class EEqual <: Expr<Bool> {
    var v1: Expr<Int64>
    var v2: Expr<Int64>
    init(v1: Expr<Int64>, v2: Expr<Int64>) {
        this.v1 = v1
        this.v2 = v2
    }
}

func eval_bool(e: Expr<Bool>) {
    match (e) {
        case x: EBool => x.v
        case x: EEqual => eval_int64(x.v1) == eval_int64(x.v2)
        case _ => false
    }
}

func eval_int64(e: Expr<Int64>) {
    match (e) {
        case x: EInt64 => x.v
        case _ => 0
    }
}

main() {
    let expr = EEqual(EInt64(0), EInt64(0))
    if (eval_bool(expr)) {
        0
    } else {
        1
    }
}
