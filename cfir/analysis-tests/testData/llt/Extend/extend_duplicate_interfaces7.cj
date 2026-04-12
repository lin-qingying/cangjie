// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %typecheck %f %noColor 2>&1 | compare %f

interface I<T> {}

class A<T> <: I<Int32> {}

extend<T> A<T> <: I<Int32> {}

main() {
    return 0
}

/* SCAN
error: interface 'Interface-I<Int32>' has been implemented by 'Class-A<Generics-T>', please remove it
  ==> extend_duplicate_interfaces7.cj:13:19:
   | 
13 | extend<T> A<T> <: I<Int32> {}
   |                   ^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
