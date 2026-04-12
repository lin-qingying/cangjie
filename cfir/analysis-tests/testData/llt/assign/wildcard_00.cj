// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    let x = _
    let y = _ + 1
    _ = _ + 1
    println(x)
}

/* SCAN
error: unexpected _ wildcard
  ==> wildcard_00.cj:10:13:
   | 
10 |     let x = _
   |             ^ expected expression, found wildcard
   | 

error: unexpected _ wildcard
  ==> wildcard_00.cj:11:13:
   | 
11 |     let y = _ + 1
   |             ^ expected expression, found wildcard
   | 

error: unexpected _ wildcard
  ==> wildcard_00.cj:12:9:
   | 
12 |     _ = _ + 1
   |         ^ expected expression, found wildcard
   | 

3 errors generated, 3 errors printed.
*/
