// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f() {
    var b: Int32
    do {
        if (true) {
            continue
        }
        b = 1
    } while (b == 0)
    b
}

main() {
    println(f())
}

/* SCAN
error: variable 'b' is used before initialization
  ==> variable_use_before_init_15.cj:16:14:
   | 
16 |     } while (b == 0)
   |              ^ 
   | 

error: variable 'b' is used before initialization
  ==> variable_use_before_init_15.cj:17:5:
   | 
17 |     b
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
