// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main(): Int64 {
    return 0
}

func case03<T>() {
    1
}

func case04() {
    case03
}

/* SCAN
error: type arguments needed for the generic function 'case03'
  ==> call27.cj:18:5:
   | 
18 |     case03
   |     ^^^^^^ cannot infer type arguments for the generic function
   | 

1 error generated, 1 error printed.
*/
