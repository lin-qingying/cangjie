// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

interface I<T> {}

class A<T> <: I<T> {}

extend<T> A<T> <: I<T> {}

main() {
    return 0
}

/* SCAN
error: interface 'Interface-I<Generics-T>' has been implemented by 'Class-A<Generics-T>', please remove it
  ==> extend_duplicate_interfaces5.cj:13:19:
   | 
13 | extend<T> A<T> <: I<T> {}
   |                   ^^^^ 
   | 

1 error generated, 1 error printed.
*/
