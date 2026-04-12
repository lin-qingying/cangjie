// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

enum Color {
    Red | Blue | Green
}

class Green {}

var Red: Int64 = 2

func test(Green: Int32) {
    var a: Int32 = Green
    a
}

main() {
    var t0 = Green
    var t1 = Red
    var t2 = Blue

    return 0
}

/* SCAN
error: expected member name or constructor call after 'Green' type name
  ==> enum15_1.cj:23:14:
   | 
23 |     var t0 = Green
   |              ^ 
   | 
note: found candidate
  ==> enum15_1.cj:13:1:
   | 
13 | class Green {}
   | ^ 
   | 

1 error generated, 1 error printed.
*/
