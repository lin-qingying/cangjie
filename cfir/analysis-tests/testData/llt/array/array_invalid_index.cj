// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f



main(): Int64 {
    var c =1
    let a = [2,1,0]
    c = a[1 << 1@#]
    c++
    c--
    return 0
}

/* SCAN
error: expected '#' or '"' in raw string, found ']'
  ==> array_invalid_index.cj:14:19:
   | 
14 |     c = a[1 << 1@#]
   |                  ~^ expected '#' or '"'
   |                  |
   |                  because it is raw string prefix
   | 

1 error generated, 1 error printed.
*/
