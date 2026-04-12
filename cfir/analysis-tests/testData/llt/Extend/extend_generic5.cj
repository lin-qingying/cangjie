// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

open class A<T> {}

interface EQ {}

extend Int32 <: EQ {}

extend<T> A<T> where T <: EQ {
    public func soo() {}
    func add(a: T) {}
}

class B<E> <: A<E> { // generic-E does not satisfy the constraint T <: EQ
    func test(a: E) {
        soo()
        add(a)
    }
}

/* SCAN
error: undeclared identifier 'soo'
  ==> extend_generic5.cj:22:9:
   | 
22 |         soo()
   |         ^ 
   | 

error: undeclared identifier 'add'
  ==> extend_generic5.cj:23:9:
   | 
23 |         add(a)
   |         ^ 
   | 

error: 'main' is missing
 ==> extend_generic5.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

3 errors generated, 3 errors printed.
*/
