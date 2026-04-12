// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK:%compiler %cmp_opt %n.cj -o %output

main(): Int64 {
    while ({
            =>
            break;
            false
        }()) {
    }
    return 0;
}

/* SCAN
error: 'break' or 'continue' must be used inside a loop
  ==> bottomType04.cj:12:13:
   | 
12 |             break;
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
