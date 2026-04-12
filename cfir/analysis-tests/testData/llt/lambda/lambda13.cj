// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    var a = 1
}

class B {}

func test(a: (B) -> Int64) {}

func test(a: (A) -> Int64) {}

func test(a: (A) -> Unit) {}

main() {
    test {r => r.a}
    0
}

/* SCAN
error: ambiguous match for function call 'test'
  ==> lambda13.cj:22:5:
   | 
22 |     test {r => r.a}
   |     ^ 
   | 
note: found candidate
  ==> lambda13.cj:17:6:
   | 
17 | func test(a: (A) -> Int64) {}
   |      ^ 
   | 
note: found candidate
  ==> lambda13.cj:19:6:
   | 
19 | func test(a: (A) -> Unit) {}
   |      ^ 
   | 

1 error generated, 1 error printed.
*/
