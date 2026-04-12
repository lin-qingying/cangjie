// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

// 函数/闭包/
// 捕获 var 声明的局部变量：捕获了可变变量的函数或lambda只能被调用，不能作为一等公民使 用，包括不能赋值给变量，不能作为实参或返回值使用。
// brief：if和while的逻辑块为lambda表达式

main(): Int64 {
    return 0
}

var b = {
    =>
    var a: Int32 = 1
    let b: Int32 = 2
    var x1 = {=> a}
    var x2 = {=> b}
    var x3 = {=> a}()
    var x4 = {=> b}()
    true
}
var c = {
    =>
    var a: Int32 = 1
    let b: Int32 = 2
    var x1 = {=> a}
    var x2 = {=> b}
    var x3 = {=> a}()
    var x4 = {=> b}()
    true
}()
var x = {
    =>
    var a = 1;
    var b = {=> a}
}()

func foo(x: () -> Int32) {
    return x
}

func goo(x: Int32) {
    return x
}

class Coo {
    public var a: Int32 = 1
    public let b: Int32 = 2
    public static var c: Int32 = 3
    public static let d: Int32 = 4
    public var e: Int32
    public let f: Int32
    public init() {
        e = 5
        f = 6
    }
}

struct Roo {
    public var a: Int32 = 1
    public let b: Int32 = 2
    public static var c: Int32 = 3
    public static let d: Int32 = 4
    public var e: Int32
    public let f: Int32
    public init() {
        e = 5
        f = 6
    }
}

/* SCAN
error: lambda capturing mutable variables needs to be called directly
  ==> lambda_closures_301.cj:21:14:
   | 
21 |     var x1 = {=> a}
   |              ^^^^^^ 
   | 
note: 'a' is mutable
  ==> lambda_closures_301.cj:21:18:
   | 
21 |     var x1 = {=> a}
   |                  ^ 
   | 

error: lambda capturing mutable variables needs to be called directly
  ==> lambda_closures_301.cj:31:14:
   | 
31 |     var x1 = {=> a}
   |              ^^^^^^ 
   | 
note: 'a' is mutable
  ==> lambda_closures_301.cj:31:18:
   | 
31 |     var x1 = {=> a}
   |                  ^ 
   | 

error: lambda capturing mutable variables needs to be called directly
  ==> lambda_closures_301.cj:40:13:
   | 
40 |     var b = {=> a}
   |             ^^^^^^ 
   | 
note: 'a' is mutable
  ==> lambda_closures_301.cj:40:17:
   | 
40 |     var b = {=> a}
   |                 ^ 
   | 

3 errors generated, 3 errors printed.
*/
