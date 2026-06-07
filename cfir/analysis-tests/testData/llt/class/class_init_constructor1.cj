// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A {
    public var a: Int64
    public var b: Int64
    public init(n: Int64) {
        <!parse_expected_expr_or_decl_in!>init<!>(n, n)
        a = n
    }
    public init(n: Int64, m: Int64) {
        a = n
        b = m
    }
}

class B <: A {
    public var c: Int64
    public init() {
        super(0)
    }
}
