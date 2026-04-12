// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class Point {
    public var x: Int32 = 0
    public var y: Int32 = 0
    public init(x: Int32, y: Int32) {
        this.x = x
        this.y = y
    }
    public operator func -(oprand!: Point = Point()): Point {
        Point(-oprand.x, -oprand.y)
    }
}

func foo(a: Int32, b!: Int64 = 10): Int64 {
    b + 1
}

main(): Int64 {
    var f = foo
    var d = f(1, 1)
    var e = f(1)
}

/* SCAN
error: optional parameter cannot be used in operator overloading function
  ==> defaultParamter.cj:16:28:
   | 
16 |     public operator func -(oprand!: Point = Point()): Point {
   |                            ^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 

error: missing arguments for parameter list '(Int32, Int32)' in call
  ==> defaultParamter.cj:16:50:
   | 
16 |     public operator func -(oprand!: Point = Point()): Point {
   |                                                  ^^ expected 2 arguments, found 0
   | 
note: found candidate
  ==> defaultParamter.cj:12:12:
   | 
12 |     public init(x: Int32, y: Int32) {
   |            ^^^^ 
   | 

error: missing argument for parameter list '(Int32, Int64)' in call
  ==> defaultParamter.cj:28:14:
   | 
28 |     var e = f(1)
   |              ^^^ expected 2 arguments, found 1
   | 
note: found candidate
  ==> defaultParamter.cj:26:9:
   | 
26 |     var f = foo
   |         ^ 
   | 

error: mismatched types
  ==> defaultParamter.cj:28:5:
   | 
28 |     var e = f(1)
   |     ^^^^^^^^^^^^ expected 'Int64', found 'Unit'
   | 
   # note: definitions and declarations are always of type 'Unit'

4 errors generated, 4 errors printed.
*/
