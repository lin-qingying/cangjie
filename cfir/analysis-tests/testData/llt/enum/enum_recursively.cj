// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

enum E {
    e(A) | e2
}

struct A {
    public A(let a: E) {}
}

struct Expr_ {
    public Expr_(let x: Int32) {}
}

struct BinExpr {
    public BinExpr(let expr_: Expr_, let op0: Expr, let op1: Expr) {}
}

struct UnaryExpr {
    public UnaryExpr(let expr_: Expr_, let op0: Expr) {}
}

enum Expr {
    Binary(BinExpr)
    | Unary(UnaryExpr)
    | Ident
}

main(): Int64 {
    var a = A(e2)
    var b = BinExpr(Expr_(1), Ident, Unary(UnaryExpr(Expr_(2), Ident)))
    let a1 = match (a.a) {
        case e(x) => 1
        case e2 => 0
    }
    let b0 = match (b.op0) {
        case Binary(x) => 1
        case Unary(x) => 1
        case Ident => 0
    }
    let b1 = match (b.op1) {
        case Binary(x) => 1
        case Unary(x) =>
            let i = match (x.op0) {
                case Ident => 0
                case _ => 1
            }
            if (x.expr_.x == 2 && i == 0) {
                return 0
            }
            return 1
        case Ident => 1
    }
    if (a1 != 0 || b0 != 0 || b1 != 0) {
        return 1
    }
    return 0
}
