// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct Base12 {
    public var du: Int64 = 1
    public prop a: Int64 {
    }
    public func f(): Int64 {
        a = 2
        return 0
    }
}

main() {
    var x = Base12()
    x.a = 4
    if (x.a == 4) {
        print("${x.a}")
        return 0
    }
    return 1
}

/* SCAN
error: property must have accessors
  ==> record_6.cj:11:5:
   | 
11 |       public prop a: Int64 {
   |  _____^
12 | |     }
   | |_____^
   | 

error: cannot assign to immutable value
  ==> record_6.cj:14:9:
   | 
14 |         a = 2
   |         ^^^^^ 
   | 
note: property 'a' is immutable
  ==> record_6.cj:11:17:
   | 
11 |     public prop a: Int64 {
   |                 ^ 
   | 

error: cannot assign to immutable value
  ==> record_6.cj:21:5:
   | 
21 |     x.a = 4
   |     ^^^^^^^ 
   | 
note: property 'a' is immutable
  ==> record_6.cj:11:17:
   | 
11 |     public prop a: Int64 {
   |                 ^ 
   | 

3 errors generated, 3 errors printed.
*/
