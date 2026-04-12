// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

interface I<T> {
    func foo(a: T): Int32
}

interface II<T> <: I<T> & I<T> & I<Int32> {
    func foo(a: Int32): Int32
    func foo(a: T): Int32
}

main() {
    return 0
}

/* SCAN
error: interface 'II' inherits or implements duplicate interface 'I'
  ==> interface_generic5.cj:13:1:
   | 
13 | interface II<T> <: I<T> & I<T> & I<Int32> {
   | ^ 
   | 

1 error generated, 1 error printed.
*/
