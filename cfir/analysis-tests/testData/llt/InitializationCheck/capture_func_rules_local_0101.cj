// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

main(): Int64 {
    return 0
}

var a = {
    =>
    var a: Int32
    let b: Int32
    func foo1(x!: Int32 = a + b) {
        return x
    }
    func foo2() {
        var x = a + b;
        return x
    }
}

/* SCAN
error: cannot capture variable 'a' before initialization
  ==> capture_func_rules_local_0101.cj:17:27:
   | 
17 |     func foo1(x!: Int32 = a + b) {
   |                           ^ 
   | 

error: cannot capture variable 'a' before initialization
  ==> capture_func_rules_local_0101.cj:21:17:
   | 
21 |         var x = a + b;
   |                 ^ 
   | 

2 errors generated, 2 errors printed.
*/
