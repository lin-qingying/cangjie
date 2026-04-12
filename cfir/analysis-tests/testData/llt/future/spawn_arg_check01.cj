// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %n.%suffix

main(): Int64 {
    let context = 6606i64

    let th = spawn (context) {
        return 25
    }
    return 0
}

/* SCAN
error: mismatched types
  ==> spawn_arg_check01.cj:12:21:
   | 
12 |     let th = spawn (context) {
   |                     ^^^^^^^ expected 'Interface-ThreadContext', found 'Int64'
   | 

1 error generated, 1 error printed.
*/
