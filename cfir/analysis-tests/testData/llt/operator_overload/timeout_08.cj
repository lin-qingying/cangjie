// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output
// TIMEOUT: 10

main():Int64 {
    var a = 0B10000_0000_0000_00000000_0000_0000_00000000_0000_0000_00000000_0000_0000_0000u64
    var b = - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -#- - - - - - - - - - - - - - - - - - - - - - - - - - - - -0b1u64

    return 1
}

/* SCAN
error: expected '#' or '"' in raw string, found '-'
  ==> timeout_08.cj:12:147:
   | 
12 |     var b = - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -#- - - - - - - - - - - - - - - - - - - - - - - - - - - - -0b1u64
   |                                                                                                                                                  ~^ expected '#' or '"'
   |                                                                                                                                                  |
   |                                                                                                                                                  because it is raw string prefix
   | 

1 error generated, 1 error printed.
*/
