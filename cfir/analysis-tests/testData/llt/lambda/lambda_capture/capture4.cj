// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

main(): Int64 {
    return 0
}

var a1 = {
    =>
    var a: Int32 = 1
    let b: Int32 = 2
    if (a == 2) {
        return {=> a}
    } else {
        return foo({=> a})
    }
    if (a == 2) {
        return {=> b}
    } else {
        return foo({=> b})
    }
}
var a2 = {
    =>
    var a: Int32 = 1
    if (a == 2) {
        return {=> a}
    } else {
        return foo({=> a})
    }
}

func foo(x: () -> Int32) {
    return x
}

/* SCAN
error: lambda capturing mutable variables needs to be called directly
  ==> capture4.cj:18:16:
   | 
18 |         return {=> a}
   |                ^^^^^^ 
   | 
note: 'a' is mutable
  ==> capture4.cj:18:20:
   | 
18 |         return {=> a}
   |                    ^ 
   | 

error: lambda capturing mutable variables needs to be called directly
  ==> capture4.cj:20:20:
   | 
20 |         return foo({=> a})
   |                    ^^^^^^ 
   | 
note: 'a' is mutable
  ==> capture4.cj:20:24:
   | 
20 |         return foo({=> a})
   |                        ^ 
   | 

error: lambda capturing mutable variables needs to be called directly
  ==> capture4.cj:32:16:
   | 
32 |         return {=> a}
   |                ^^^^^^ 
   | 
note: 'a' is mutable
  ==> capture4.cj:32:20:
   | 
32 |         return {=> a}
   |                    ^ 
   | 

error: lambda capturing mutable variables needs to be called directly
  ==> capture4.cj:34:20:
   | 
34 |         return foo({=> a})
   |                    ^^^^^^ 
   | 
note: 'a' is mutable
  ==> capture4.cj:34:24:
   | 
34 |         return foo({=> a})
   |                        ^ 
   | 

4 errors generated, 4 errors printed.
*/
