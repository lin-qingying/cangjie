// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %noColor %f -o %output 2>&1 | compare %f

// Test for fuzzing CheckInstTypeCompleteness
interface I0<T> {}

interface I1 <: I0<Int64> & Option<I0> {
    static func foo() {}
}

func test<T>() where T <: I1 {
    T.foo()
}

main() {
    test<I1>()
    return 0
}

/* SCAN
error: generic type should be used with type argument
  ==> generic_call_static_impl10.cj:12:36:
   | 
12 | interface I1 <: I0<Int64> & Option<I0> {
   |                                    ^ 
   | 

error: interface 'I1' can only inherit interface
  ==> generic_call_static_impl10.cj:12:1:
   | 
12 | interface I1 <: I0<Int64> & Option<I0> {
   | ^ 
   | 

2 errors generated, 2 errors printed.
*/
