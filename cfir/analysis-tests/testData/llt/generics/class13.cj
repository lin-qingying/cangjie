// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A<R> {}

class B<R> {
    public let a: R<R>
    public let b: A<R>
}

func test<T>() {
    var a: T<T>
}

main(): Int64 {
    return 0
}

/* SCAN
error: type argument's number does not match type parameter's number
  ==> class13.cj:12:19:
   | 
12 |     public let a: R<R>
   |                   ^^^^ 
   | 

error: type argument's number does not match type parameter's number
  ==> class13.cj:17:12:
   | 
17 |     var a: T<T>
   |            ^^^^ 
   | 

error: the uninitialized member variable 'a' is not initialized in the constructor of class or struct
  ==> class13.cj:12:5:
   | 
12 |     public let a: R<R>
   |     ^ 
   | 

error: the uninitialized member variable 'b' is not initialized in the constructor of class or struct
  ==> class13.cj:13:5:
   | 
13 |     public let b: A<R>
   |     ^ 
   | 

4 errors generated, 4 errors printed.
*/
