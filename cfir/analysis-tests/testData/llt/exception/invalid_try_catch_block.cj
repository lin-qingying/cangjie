// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func foo(a: Array<Int64>): Int64 {
    1
}

main(): Int64 {
    foo([try {
        1
    } catch (_) {
        f // undefined
        2
    }])
}

/* SCAN
error: undeclared identifier 'f'
  ==> invalid_try_catch_block.cj:17:9:
   | 
17 |         f // undefined
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
