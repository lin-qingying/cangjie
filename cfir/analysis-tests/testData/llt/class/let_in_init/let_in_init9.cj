// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

main(): Int64 {
    return 0
}

func case01() {
    var a: Coo = Coo()
    var m: Int32 = 1
    return {
        x: () -> Int32 =>
        a.b = x() + 2
        m = 2
        var res = a.b + m;
        res
    }({=> 1})
}

func case0101() {
    var a: Coo = Coo()
    let m: Int32 = 1
    return {
        x: () -> Int32 =>
        a.a = x() + 2
        m = 2
        var res = a.a + m;
        res
    }({=> 1})
}

func case02() {
    let b: Coo = Coo()
    var m: Int32 = 1
    return {
        x: () -> Int32 =>
        b.b = x() + 2
        m = 2
        var res = b.b + m;
        res
    }({=> 1})
}

func case03() {
    var c: Roo = Roo()
    var m: Int32 = 1
    return {
        x: () -> Int32 =>
        c.b = x() + 2
        m = 2
        var res = c.b + m;
        res
    }({=> 1})
}

func case04() {
    let c: Roo = Roo()
    var m: Int32 = 1
    return {
        x: () -> Int32 =>
        c.b = x() + 2
        m = 2
        var res = c.b + m;
        res
    }({=> 1})
}

func case05() {
    let c: Roo = Roo()
    var m: Int32 = 1
    return {
        x: () -> Int32 =>
        c.a = x() + 2
        m = 2
        var res = c.a + m;
        res
    }({=> 1})
}

class Coo {
    public var a: Int32 = 1
    public let b: Int32 = 2
}

struct Roo {
    public var a: Int32 = 1
    public let b: Int32 = 2
}

/* SCAN
error: cannot assign to immutable value
  ==> let_in_init9.cj:18:9:
   | 
18 |         a.b = x() + 2
   |         ^^^^^^^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> let_in_init9.cj:87:16:
   | 
87 |     public let b: Int32 = 2
   |                ^ 
   | 

error: cannot assign to immutable value
  ==> let_in_init9.cj:31:9:
   | 
31 |         m = 2
   |         ^^^^^ 
   | 
note: variable 'm' is immutable
  ==> let_in_init9.cj:27:9:
   | 
27 |     let m: Int32 = 1
   |         ^ 
   | 

error: cannot assign to immutable value
  ==> let_in_init9.cj:42:9:
   | 
42 |         b.b = x() + 2
   |         ^^^^^^^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> let_in_init9.cj:87:16:
   | 
87 |     public let b: Int32 = 2
   |                ^ 
   | 

error: cannot assign to immutable value
  ==> let_in_init9.cj:54:9:
   | 
54 |         c.b = x() + 2
   |         ^^^^^^^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> let_in_init9.cj:92:16:
   | 
92 |     public let b: Int32 = 2
   |                ^ 
   | 

error: cannot assign to immutable value
  ==> let_in_init9.cj:66:9:
   | 
66 |         c.b = x() + 2
   |         ^^^^^^^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> let_in_init9.cj:92:16:
   | 
92 |     public let b: Int32 = 2
   |                ^ 
   | 

error: cannot assign to immutable value
  ==> let_in_init9.cj:78:9:
   | 
78 |         c.a = x() + 2
   |         ^^^^^^^^^^^^^ 
   | 
note: variable 'c' is immutable
  ==> let_in_init9.cj:74:9:
   | 
74 |     let c: Roo = Roo()
   |         ^ 
   | 

6 errors generated, 6 errors printed.
*/
