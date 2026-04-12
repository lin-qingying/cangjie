// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 |compare %f

func case01() {
    let intList: Array<Int32> = [0, 1, 2, 3, 4]
    var a: Int32 = 0
    let b: Int32 = 2
    for (2 in intList) {
        a = b
    }
    a
}

func case03() {
    let intList: Array<Int32> = [0, 1, 2, 3, 4]
    var a: Int32 = 0
    for (_: Int32 in intList) {
        a++
    }
    a
}

enum TimeUnit {
    Year(Int32)
    | Mounth
}

main() {
    let intList: Array<TimeUnit>
    let a: Array<TimeUnit> = [Year(1), Year(2)]
    for (Year(x) in a) {
    }
    0
}

/* SCAN
error: the pattern in for-in expression must be irrefutable
  ==> forin3.cj:13:5:
   | 
13 |     for (2 in intList) {
   |     ^ 
   | 

error: the pattern in for-in expression must be irrefutable
  ==> forin3.cj:22:5:
   | 
22 |     for (_: Int32 in intList) {
   |     ^ 
   | 

error: the pattern in for-in expression must be irrefutable
  ==> forin3.cj:36:5:
   | 
36 |     for (Year(x) in a) {
   |     ^ 
   | 

3 errors generated, 3 errors printed.
*/
