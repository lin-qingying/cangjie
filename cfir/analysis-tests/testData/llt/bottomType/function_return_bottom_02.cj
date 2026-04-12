// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (DEBUG_OPTION)EXEC: %compiler %debugCHIROpt %cmp_opt %f -o %output 2>&1 | compare %f




main(): Int64 {
    case01()
    return 0
}

func case01() {
    while (break) {
        print("yes")
    }
}

/* SCAN
error: 'break' or 'continue' must be used inside a loop
  ==> function_return_bottom_02.cj:18:12:
   | 
18 |     while (break) {
   |            ^ 
   | 

1 error generated, 1 error printed.
*/
