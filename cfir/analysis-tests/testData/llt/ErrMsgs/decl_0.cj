// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

interface I {}

class C<T> where T <: I {}

func foo(x: C<<!sema_generic_type_argument_not_match_constraint!>U<!>nit>) {}

main() {
    let v: C<<!sema_generic_type_argument_not_match_constraint!>U<!>nit> = 1
    foo(1)
    ()
}
