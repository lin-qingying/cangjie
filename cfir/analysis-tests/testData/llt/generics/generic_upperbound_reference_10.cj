// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f

class A {
    func foo<T, V>(a: T, b: V): Unit where T <: V, V <: T {}
}

main() {}

/* SCAN
error: generic parameter 'T' is bounded directly recursively with 'Generics-V' which is forbidden
  ==> generic_upperbound_reference_10.cj:10:14:
   | 
10 |     func foo<T, V>(a: T, b: V): Unit where T <: V, V <: T {}
   |              ^ 
   | 

1 error generated, 1 error printed.
*/
