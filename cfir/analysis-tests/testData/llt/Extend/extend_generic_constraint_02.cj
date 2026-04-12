// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A<T> {}

extend<T> A<T> where T <: ToString {
    func f() {
        1
    }
}

extend<T> A<T> {
    func f2() {
        f
        this.f
        this.f()
    }
}

main() {}

/* SCAN
error: undeclared identifier 'f'
  ==> extend_generic_constraint_02.cj:19:9:
   | 
19 |         f
   |         ^ 
   | 

error: 'f' is not a member of class 'A<Generics-T>'
  ==> extend_generic_constraint_02.cj:20:14:
   | 
20 |         this.f
   |              ^ 
   | 

error: 'f' is not a member of class 'A<Generics-T>'
  ==> extend_generic_constraint_02.cj:21:14:
   | 
21 |         this.f()
   |              ^ 
   | 

3 errors generated, 3 errors printed.
*/
