// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    let x: (Int32, Int32) = if (true) {
        (0, 0)
    } else {
        (0, 0, 0)
    }
}

/* SCAN
error: mismatched types
  ==> tuple9.cj:13:9:
   | 
13 |         (0, 0, 0)
   |         ^^^^^^^^^ expected 'Tuple<Int32, Int32>', found 'Tuple<Int64, Int64, Int64>'
   | 

1 error generated, 1 error printed.
*/
