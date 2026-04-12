// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class C {}

class D {}

class E {}

interface I<T> {
    func foo(): T
}

interface II<T> <: I<C> & I<D> & I<E> {
    func foo(): C
}

main() {
    return 0
}

/* SCAN
error: return type of 'foo' is not identical or not a subtype of the overridden/redefined/implement function
  ==> interface_generic14.cj:20:10:
   | 
20 |     func foo(): C
   |          ^^^ 
   | 
note: 'Class-C' is not a subtype of 'Class-D', 'Class-E'
  ==> interface_generic14.cj:20:10:
   | 
20 |     func foo(): C
   |          ^^^ 
   | 

1 error generated, 1 error printed.
*/
