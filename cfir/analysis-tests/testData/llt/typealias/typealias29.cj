// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %diag_json %f -o %n.%suffix 2>&1 | compare %f

interface I {}

extend Int <: I {}

extend<T> Option<T> <: I where T <: I {}

class Cl<U1, U2> where U1 <: I, U2 <: I {}

type A<T1, T2, T3> = Cl<Option<T1>, Option<T1>> // ok

func f<X>(a: A<X, X, X>) { // error
    A<X, X, X>() // error
    A<Int, Int, Int>() // ok
}

main() {}

// ASSERT: scan-2 sema_generic_type_argument_not_match_constraint
// ASSERT: scan-1 typealias_unused_type_parameters
// ASSERT: scan "Errors": 2
