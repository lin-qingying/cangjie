// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct R1 {
    public var i = 0

    public mut func foo(): Unit {
        i += 1
    }
}

func goo() {
    var obj = R1()
    var fn = obj.foo
    obj.foo
    return obj.foo
}

main() {
    return 1
}

/* SCAN
error: mutable function 'foo' cannot be used alone as reference
  ==> record_mut_invalid_11.cj:19:18:
   | 
19 |     var fn = obj.foo
   |                  ^^^ 
   | 
note: 'foo' is a mutable funciton
  ==> record_mut_invalid_11.cj:12:21:
   | 
12 |     public mut func foo(): Unit {
   |                     ^^^ 
   | 

error: mutable function 'foo' cannot be used alone as reference
  ==> record_mut_invalid_11.cj:20:9:
   | 
20 |     obj.foo
   |         ^^^ 
   | 
note: 'foo' is a mutable funciton
  ==> record_mut_invalid_11.cj:12:21:
   | 
12 |     public mut func foo(): Unit {
   |                     ^^^ 
   | 

error: mutable function 'foo' cannot be used alone as reference
  ==> record_mut_invalid_11.cj:21:16:
   | 
21 |     return obj.foo
   |                ^^^ 
   | 
note: 'foo' is a mutable funciton
  ==> record_mut_invalid_11.cj:12:21:
   | 
12 |     public mut func foo(): Unit {
   |                     ^^^ 
   | 

3 errors generated, 3 errors printed.
*/
