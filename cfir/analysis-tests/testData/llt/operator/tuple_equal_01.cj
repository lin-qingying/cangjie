// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f(s: String) {
    1
}

main() {
    f((1, 2) == (1, 2))
}

/* SCAN
error: mismatched types
  ==> tuple_equal_01.cj:14:7:
   | 
14 |     f((1, 2) == (1, 2))
   |       ^^^^^^^^^^^^^^^^ expected 'Struct-String', found 'Bool'
   | 

1 error generated, 1 error printed.
*/
