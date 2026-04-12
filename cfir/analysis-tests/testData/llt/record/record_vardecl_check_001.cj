// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

struct R {
    public var a: C = C()
}

struct C {
    public var m: Int64 = 1
}

main(): Int64 {
    let r = R()
    r.a.m = 2
    return 0
}

/* SCAN
error: cannot assign to immutable value
  ==> record_vardecl_check_001.cj:19:5:
   | 
19 |     r.a.m = 2
   |     ^^^^^^^^^ 
   | 
note: variable 'r' is immutable
  ==> record_vardecl_check_001.cj:18:9:
   | 
18 |     let r = R()
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
