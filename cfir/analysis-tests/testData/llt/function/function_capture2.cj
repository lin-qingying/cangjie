// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func f() {
    var x = 1
    let y = 2
    func g() {
        print("${x}")
    }
    let b = g
    g
    g()
    func g2() {
        g()
    }
    let c = g2
    func h() {
        print("${y}")
    }
    let d = h
    let e = {=> print("${x}")}
    let i = {=> x * x}()
    return g
}

let x = 4

func f2() {
    var a = x
    let x: Int64
    func f1() {
        x
    }
    x = 99
    f1()
}

main(): Int64 {
    return 0
}

/* SCAN
error: cannot capture variable 'x' before initialization
  ==> function_capture2.cj:37:9:
   | 
37 |         x
   |         ^ 
   | 

error: function capturing mutable variables needs to be called directly
  ==> function_capture2.cj:15:13:
   | 
15 |     let b = g
   |             ^ 
   | 
note: 'x' is mutable
  ==> function_capture2.cj:13:18:
   | 
13 |         print("${x}")
   |                  ^ 
   | 

error: function capturing mutable variables needs to be called directly
  ==> function_capture2.cj:16:5:
   | 
16 |     g
   |     ^ 
   | 
note: 'x' is mutable
  ==> function_capture2.cj:13:18:
   | 
13 |         print("${x}")
   |                  ^ 
   | 

error: g2 captured a mutable variable transitively, g2 cannot be assigned to a variable
  ==> function_capture2.cj:21:13:
   | 
21 |     let c = g2
   |             ^ 
   | 

error: lambda capturing mutable variables needs to be called directly
  ==> function_capture2.cj:26:13:
   | 
26 |     let e = {=> print("${x}")}
   |             ^^^^^^^^^^^^^^^^^^ 
   | 
note: 'x' is mutable
  ==> function_capture2.cj:26:26:
   | 
26 |     let e = {=> print("${x}")}
   |                          ^ 
   | 

error: function capturing mutable variables needs to be called directly
  ==> function_capture2.cj:28:12:
   | 
28 |     return g
   |            ^ 
   | 
note: 'x' is mutable
  ==> function_capture2.cj:13:18:
   | 
13 |         print("${x}")
   |                  ^ 
   | 

6 errors generated, 6 errors printed.
*/
