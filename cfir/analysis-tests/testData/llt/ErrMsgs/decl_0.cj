// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I {}

class C<T> where T <: I {}

func foo(x: C<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>U<!>nit>) {}

main() {
    let v: C<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>U<!>nit> = 1
    foo(1)
    ()
}
