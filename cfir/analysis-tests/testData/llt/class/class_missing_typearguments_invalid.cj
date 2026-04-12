// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

class R<T> {
    public static func funca(a: Int32): Int32 {
        return a
    }
}

let a = R.funca
var b = R.funca

main(): Int64 {
    return Int64(a(1) + b(1) - 2)
}

/* SCAN
error: generic type should be used with type argument
  ==> class_missing_typearguments_invalid.cj:15:9:
   | 
15 | let a = R.funca
   |         ^ 
   | 

error: generic type should be used with type argument
  ==> class_missing_typearguments_invalid.cj:16:9:
   | 
16 | var b = R.funca
   |         ^ 
   | 

2 errors generated, 2 errors printed.
*/
