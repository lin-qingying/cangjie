// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public enum Expr<T> <: ToString where T <: ToString {
    | Ele(T)
    | Add(Expr<T>, Expr<T>)
    | Sub(Expr<T>, Expr<T>)

    public func toString(): String {
        match (this) {
            case Ele(e) => e.toString()
            case Add(e1, e2) => "ADD ${e1} ${e2}"
            case Sub(e1, e2) => "SUB ${e1} ${e2}"
        }
    }
}

public struct Rcd<T> <: ToString where T <: ToString {
    public func toString(): String {
        "struct"
    }
}
