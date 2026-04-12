// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %compile_lib_opt %n.cj 2>&1 | compare %f

main() {
    var b = R(a: 6)
    b.c(7)
    return Int64(b.a - 8)
}

interface I {
    func c(b: Int32): Int32
    func cc(): Int32
}

struct R {
    public var a: Int32 = 8
    public mut func c(b: Int32): Int32 {
        let c = cc
        a = c() + b
        return a
    }
}

extend R <: I {
    public func cc(): Int32 {
        return 1
    }
}

/* SCAN
error: extra argument given for parameter list '()' in call
  ==> extend_mutable_function_invalid_1.cj:10:14:
   | 
10 |     var b = R(a: 6)
   |              ^^^^^^ expected 0 arguments, found 1
   | 

error: undeclared identifier 'cc'
  ==> extend_mutable_function_invalid_1.cj:23:17:
   | 
23 |         let c = cc
   |                 ^ 
   | 

error: 'mut' modifier of 'c' is incompatible with that in interface 'I'
  ==> extend_mutable_function_invalid_1.cj:29:1:
   | 
29 | extend R <: I {
   | ^ 
   | 

3 errors generated, 3 errors printed.
*/
