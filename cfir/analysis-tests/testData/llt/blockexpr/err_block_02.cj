// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {}

var a: Int64 = 1

main() {
    if (a <= if (false) {
        var b: A = A1()
        1i64
    } else {
        1
    }) {
        return 1
    }
    return 0
}

/* SCAN
error: undeclared identifier 'A1'
  ==> err_block_02.cj:15:20:
   | 
15 |         var b: A = A1()
   |                    ^ 
   | 

1 error generated, 1 error printed.
*/
