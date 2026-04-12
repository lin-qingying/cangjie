// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %f %cmp_opt -Woff all

@Annotation[target: [GlobalFunction]]
public class APILevel {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

@APILevel[11]
var (a, b) = (3, "")

@APILevel[11]
const c = -1

main() {}

/* SCAN
error: '@APILevel' not applicable to global variable
  ==> globalfunc_on_globalvar.cj:17:1:
   | 
17 | @APILevel[11]
   | ^^^^^^^^^^^^^ 
   | 

error: '@APILevel' not applicable to global variable
  ==> globalfunc_on_globalvar.cj:20:1:
   | 
20 | @APILevel[11]
   | ^^^^^^^^^^^^^ 
   | 

2 errors generated, 2 errors printed.
*/
