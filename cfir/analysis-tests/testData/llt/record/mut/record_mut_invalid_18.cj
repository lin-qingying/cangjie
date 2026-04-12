// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct R1 {
    public var i = 0

    public mut func foo() {
        i += 1
    }
}

class A<T> where T <: R1 {
    func structMut<X>(obj: X): Unit where X <: T {
        obj.foo()
        var r = obj
        let _ = r.foo
    }
}

main() {
    return 0
}

/* SCAN
error: the upper bound 'Struct-R1' of generic parameter 'Generics-T' must be class or interface
  ==> record_mut_invalid_18.cj:17:9:
   | 
17 | class A<T> where T <: R1 {
   |         ^ 
   | 

error: the upper bound 'Generics-T' of generic parameter 'Generics-X' must be class or interface
  ==> record_mut_invalid_18.cj:18:20:
   | 
18 |     func structMut<X>(obj: X): Unit where X <: T {
   |                    ^ 
   | 

error: cannot use mutable function on immutable value
  ==> record_mut_invalid_18.cj:19:9:
   | 
19 |         obj.foo()
   |         ^^^ ~~~ is a mutable function
   |         |
   |         is immutable
   | 
note: 'obj' is a variable declared with 'let'
  ==> record_mut_invalid_18.cj:18:23:
   | 
18 |     func structMut<X>(obj: X): Unit where X <: T {
   |                       ^^^ 
   | 

error: mutable function 'foo' cannot be used alone as reference
  ==> record_mut_invalid_18.cj:21:19:
   | 
21 |         let _ = r.foo
   |                   ^^^ 
   | 
note: 'foo' is a mutable funciton
  ==> record_mut_invalid_18.cj:12:21:
   | 
12 |     public mut func foo() {
   |                     ^^^ 
   | 

4 errors generated, 4 errors printed.
*/
