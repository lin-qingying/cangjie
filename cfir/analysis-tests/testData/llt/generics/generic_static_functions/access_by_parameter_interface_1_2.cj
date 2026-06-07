// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I {
    <!parse_unexpected_declaration_in_scope!>let<!> a = 0
}

func Foo<U>(a:U) where U <: I {
    var b = U.a
    return b
}

class C1 <: I {}

main() {
    return Foo<C1>(C1())
}
