// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE) EXEC-PIPE-1: %compiler %f 2>&1 %cmp_opt | compare %f
var b = VArray<Int64, $3>({i => i})

func getb(n: String) {
    println(n)
    b
}

class A {
    var a = VArray<Int64, $3>({i => i})
    func foo() {
        a
    }
}

main() {
    let a = A()
    a.foo()[0] = 18

    getb("left")[1] = 0
}

/* SCAN
error: cannot assign to immutable value
  ==> assign.cj:24:5:
   | 
24 |     a.foo()[0] = 18
   |     ^^^^^^^^^^^^^^^ 
   | 
note: function call returns immutable value
  ==> assign.cj:24:5:
   | 
24 |     a.foo()[0] = 18
   |     ^^^^^^^ 
   | 

error: cannot assign to immutable value
  ==> assign.cj:26:5:
   | 
26 |     getb("left")[1] = 0
   |     ^^^^^^^^^^^^^^^^^^^ 
   | 
note: function call returns immutable value
  ==> assign.cj:26:5:
   | 
26 |     getb("left")[1] = 0
   |     ^^^^^^^^^^^^ 
   | 

2 errors generated, 2 errors printed.
*/
