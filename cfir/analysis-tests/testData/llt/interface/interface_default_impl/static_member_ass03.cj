// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

interface I {
    static func f1(): Unit
    static func f2(): Unit {
        f1()
    }
    static func f3(): Unit {
        f2()
    }
}

interface I2 {
    static func f4(): Unit {
        I.f2() // error
    }
}

main() {
    I.f3() // error
}

/* SCAN
error: static invocation contains unimplemented static function 'f1'
  ==> static_member_ass03.cj:21:9:
   | 
21 |         I.f2() // error
   |         ^^^^ 
   | 
note: indirect use of unimplemented function reference here
  ==> static_member_ass03.cj:12:9:
   | 
12 |         f1()
   |         ^^ 
   | 

error: static invocation contains unimplemented static function 'f1'
  ==> static_member_ass03.cj:26:5:
   | 
26 |     I.f3() // error
   |     ^^^^ 
   | 
note: indirect use of unimplemented function reference here
  ==> static_member_ass03.cj:12:9:
   | 
12 |         f1()
   |         ^^ 
   | 

2 errors generated, 2 errors printed.
*/
