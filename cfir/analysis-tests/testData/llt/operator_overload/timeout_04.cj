// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    - - - - - - - - - - - - - - - - - - - - - - - -x
}

/* SCAN
error: undeclared identifier 'x'
  ==> timeout_04.cj:10:52:
   | 
10 |     - - - - - - - - - - - - - - - - - - - - - - - -x
   |                                                    ^ 
   | 

1 error generated, 1 error printed.
*/
