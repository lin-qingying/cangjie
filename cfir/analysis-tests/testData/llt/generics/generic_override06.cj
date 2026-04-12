// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I<T, R> where T <: R {
    func foo(): R
}

class C<T, R> <: I<T, R> where T <: R {
    var x: T
    init(_x: T) {
        x = _x
    }
    public func foo(): T {
        x
    }
}

main() {
    0
}

/* SCAN
error: the upper bound 'Generics-R' of generic parameter 'Generics-T' must be class or interface
 ==> generic_override06.cj:9:13:
  | 
9 | interface I<T, R> where T <: R {
  |             ^ 
  | 

error: the upper bound 'Generics-R' of generic parameter 'Generics-T' must be class or interface
  ==> generic_override06.cj:13:9:
   | 
13 | class C<T, R> <: I<T, R> where T <: R {
   |         ^ 
   | 

error: return type of 'foo' can only be class/interface type which implements or inherits the interface type 'Generics-R'
  ==> generic_override06.cj:18:5:
   | 
18 |     public func foo(): T {
   |     ^ 
   | 
note: cannot override/implement the following function
  ==> generic_override06.cj:10:5:
   | 
10 |     func foo(): R
   |     ^^^^^^^^^^^^^ 
   | 

3 errors generated, 3 errors printed.
*/
