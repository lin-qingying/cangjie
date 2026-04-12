// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %noColor %f 2>&1 | compare %f
struct A {
    private init() {}
    public init(b: Int64, c: Int64) {}
    public func m() {
        var a = A() // ok
        var c = A(1, 1) // ok
    }
    public static func f() {
        var a = A() // ok
        var c = A(1, 1) // ok
    }
}

struct B {
    public func m() {
        var a = A()
        var c = A(1, 1) // OK
    }
    public static func f() {
        var a = A()
        var c = A(1, 1) // OK
    }
}

main() {
    var a = A()
    var c = A(1, 1) // OK
    return 0
}

/* SCAN
error: missing arguments for parameter list '(Int64, Int64)' in call
  ==> record_private_init.cj:23:18:
   | 
23 |         var a = A()
   |                  ^^ expected 2 arguments, found 0
   | 
note: found candidate
  ==> record_private_init.cj:10:12:
   | 
10 |     public init(b: Int64, c: Int64) {}
   |            ^^^^ 
   | 

error: missing arguments for parameter list '(Int64, Int64)' in call
  ==> record_private_init.cj:27:18:
   | 
27 |         var a = A()
   |                  ^^ expected 2 arguments, found 0
   | 
note: found candidate
  ==> record_private_init.cj:10:12:
   | 
10 |     public init(b: Int64, c: Int64) {}
   |            ^^^^ 
   | 

error: missing arguments for parameter list '(Int64, Int64)' in call
  ==> record_private_init.cj:33:14:
   | 
33 |     var a = A()
   |              ^^ expected 2 arguments, found 0
   | 
note: found candidate
  ==> record_private_init.cj:10:12:
   | 
10 |     public init(b: Int64, c: Int64) {}
   |            ^^^^ 
   | 

3 errors generated, 3 errors printed.
*/
