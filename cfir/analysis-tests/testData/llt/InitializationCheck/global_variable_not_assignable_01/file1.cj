// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: file2.cj
// ERRCHECK: %compiler %cmp_opt %f file2.cj -o %output

class A {
    public func foo() {
        res1 += 1
        res2 += 1
        return
    }
}

/* SCAN
error: cannot assign to immutable value
  ==> file1.cj:12:9:
   | 
12 |         res1 += 1
   |         ^^^^^^^^^ 
   | 
note: variable 'res1' is immutable
  ==> file2.cj:13:7:
   | 
13 | const res1 = 0
   |       ^^^^ 
   | 

error: cannot assign to immutable value
  ==> file1.cj:13:9:
   | 
13 |         res2 += 1
   |         ^^^^^^^^^ 
   | 
note: variable 'res2' is immutable
  ==> file2.cj:14:5:
   | 
14 | let res2 = 0
   |     ^^^^ 
   | 

2 errors generated, 2 errors printed.
*/
