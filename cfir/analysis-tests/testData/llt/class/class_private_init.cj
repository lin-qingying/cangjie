// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %noColor %f %error_count_limit_all 2>&1 | compare %f

class A {
    private init() {}
    protected init(a: Int64) {}
    public init(b: Int64, c: Int64) {}
    public func m() {
        var a = A() // ok
        var b = A(1) // ok
        var c = A(1, 1) // ok
    }
    public static func p() {
        var a = A() // ok
        var b = A(1) // ok
        var c = A(1, 1) // ok
    }
}

class B {
    public func m() {
        var a = A()
    }
}

class C <: A {
    public init() {
        super()
    }
    public init(a: Int64) {
        super(a)
    }
    public func k() {
        var a = A()
        var b = A(1) // OK
        var c = A(1, 1) // OK
    }
    public static func j() {
        var a = A()
        var b = A(1) // OK
        var c = A(1, 1) // OK
    }
}

main() {
    var a = A(1)
    var b = A()
    var c = A(1, 1) // OK
    return 0
}

/* SCAN
error: super class 'A' is not inheritable
  ==> class_private_init.cj:31:12:
   | 
31 | class C <: A {
   |            ^ 
   | 

error: missing arguments for parameter list '(Int64, Int64)' in call
  ==> class_private_init.cj:27:18:
   | 
27 |         var a = A()
   |                  ^^ expected 2 arguments, found 0
   | 
note: found candidate
  ==> class_private_init.cj:12:12:
   | 
12 |     public init(b: Int64, c: Int64) {}
   |            ^^^^ 
   | 

error: missing arguments for parameter list '(Int64, Int64)' in call
  ==> class_private_init.cj:33:14:
   | 
33 |         super()
   |              ^^ expected 2 arguments, found 0
   | 
note: found candidate
  ==> class_private_init.cj:12:12:
   | 
12 |     public init(b: Int64, c: Int64) {}
   |            ^^^^ 
   | 

error: missing arguments for parameter list '(Int64, Int64)' in call
  ==> class_private_init.cj:39:18:
   | 
39 |         var a = A()
   |                  ^^ expected 2 arguments, found 0
   | 
note: found candidate
  ==> class_private_init.cj:12:12:
   | 
12 |     public init(b: Int64, c: Int64) {}
   |            ^^^^ 
   | 

error: missing arguments for parameter list '(Int64, Int64)' in call
  ==> class_private_init.cj:44:18:
   | 
44 |         var a = A()
   |                  ^^ expected 2 arguments, found 0
   | 
note: found candidate
  ==> class_private_init.cj:12:12:
   | 
12 |     public init(b: Int64, c: Int64) {}
   |            ^^^^ 
   | 

error: missing arguments for parameter list '(Int64, Int64)' in call
  ==> class_private_init.cj:52:14:
   | 
52 |     var b = A()
   |              ^^ expected 2 arguments, found 0
   | 
note: found candidate
  ==> class_private_init.cj:12:12:
   | 
12 |     public init(b: Int64, c: Int64) {}
   |            ^^^^ 
   | 

6 errors generated, 6 errors printed.
*/
