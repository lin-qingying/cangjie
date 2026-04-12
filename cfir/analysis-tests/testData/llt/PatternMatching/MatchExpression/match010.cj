// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

func test(): Int64 {
    var i : Int32 = 2
    let a = 3
    match (i) {
        case a@t =>
            print(t.toString()+",")
            print(a.toString())
            1
        case _ => 0
    }
    return 0
}

main(): Int64 {
    return test()
}

/* SCAN
error: expected '=>' in case, found '@'
  ==> match010.cj:13:15:
   | 
13 |         case a@t =>
   |         ~~~~  ^ expected '=>' here
   | 

1 error generated, 1 error printed.
*/
