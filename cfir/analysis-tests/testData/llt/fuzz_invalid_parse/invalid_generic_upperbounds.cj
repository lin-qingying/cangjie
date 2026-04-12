// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

// Was ICE, should quit normally
open class C1{}
class C2 <: C1{}

enum A<X,Y> where X <:A<X,Y> where X <: C1,Y<:C2{
    a
    public func test<Z>(x:Z) where Z <: X&Y{x}
}


main() {
    a<C2,C2>.test<C2>(C2())
    0
}

/* SCAN
error: expected '{', found keyword 'where'
 ==> invalid_generic_upperbounds.cj:7:30:
  | 
7 | enum A<X,Y> where X <:A<X,Y> where X <: C1,Y<:C2{
  |                              ^^^^^ expected '{' here
  | 

1 error generated, 1 error printed.
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
