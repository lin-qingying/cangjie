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
        let b: A = if (false) {
            let c = 1
        } else {
            A()
        }
        1i64
    } else {
        1
    }) {
        return 1
    }
    return 0
}

/* SCAN
error: mismatched types
  ==> err_block_03.cj:16:13:
   | 
16 |             let c = 1
   |             ^^^^^^^^^ expected 'Class-A', found 'Unit'
   | 
   # note: definitions and declarations are always of type 'Unit'

1 error generated, 1 error printed.
*/
