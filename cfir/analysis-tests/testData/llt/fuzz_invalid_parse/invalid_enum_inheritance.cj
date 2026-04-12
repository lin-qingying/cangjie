// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

// Was stackoverflow during type substitution. should pass
func test() {
    EE<Int64>.test(1)
}

interface I<T> {
    func test(a: T): Int64 {
        return 0
    }
}

enum E<T> <: E<E<T>> & I<T> {
    EE
}

main() {
    EE.test(1)
}

/* SCAN
error: declaration 'E' cannot inherit itself
  ==> invalid_enum_inheritance.cj:14:14:
   | 
14 | enum E<T> <: E<E<T>> & I<T> {
   |      ~       ^^^^^^^ illegal super declaration here
   |      |
   |      because 'E' cannot inherit itself
   | 

1 error generated, 1 error printed.
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
