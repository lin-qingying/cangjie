// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f

interface I<T> {
    func test(a: T) {
        return a
    }
}

class A <: I<Rune> & I<Int32> {}

func test<T>(a: T) where T <: I<Rune> & I<Int32> {
    let f = a.test
    a.test
    return 1
}

main() {
    return test(A())
}

/* SCAN
error: ambiguous match for reference 'test'
  ==> generic_upperbound_reference_02.cj:18:13:
   | 
18 |     let f = a.test
   |             ^ 
   | 

error: ambiguous match for reference 'test'
  ==> generic_upperbound_reference_02.cj:19:5:
   | 
19 |     a.test
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
