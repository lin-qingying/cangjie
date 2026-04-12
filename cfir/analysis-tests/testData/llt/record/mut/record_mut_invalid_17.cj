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

func structMut<T>(obj: T): Unit where T <: R1 {
    obj.foo()
    var r = R1()
    let _ = r.foo
}

main() {
    return 0
}

/* SCAN
error: the upper bound 'Struct-R1' of generic parameter 'Generics-T' must be class or interface
  ==> record_mut_invalid_17.cj:17:16:
   | 
17 | func structMut<T>(obj: T): Unit where T <: R1 {
   |                ^ 
   | 

error: cannot use mutable function on immutable value
  ==> record_mut_invalid_17.cj:18:5:
   | 
18 |     obj.foo()
   |     ^^^ ~~~ is a mutable function
   |     |
   |     is immutable
   | 
note: 'obj' is a variable declared with 'let'
  ==> record_mut_invalid_17.cj:17:19:
   | 
17 | func structMut<T>(obj: T): Unit where T <: R1 {
   |                   ^^^ 
   | 

error: mutable function 'foo' cannot be used alone as reference
  ==> record_mut_invalid_17.cj:20:15:
   | 
20 |     let _ = r.foo
   |               ^^^ 
   | 
note: 'foo' is a mutable funciton
  ==> record_mut_invalid_17.cj:12:21:
   | 
12 |     public mut func foo() {
   |                     ^^^ 
   | 

3 errors generated, 3 errors printed.
*/
