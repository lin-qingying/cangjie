// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I {}

extend Int <: I {}

extend<T> Option<T> <: I where T <: I {}

class Cl<U1, U2> where U1 <: I, U2 <: I {}

<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>t<!>ype A<T1, T2, T3> = Cl<Option<T1>, Option<T1>> // ok

func f<X>(a: <!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>A<!><X, X, X>) { // error
    <!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>A<!><X, X, X>() // error
    A<Int, Int, Int>() // ok
}

main() {}
