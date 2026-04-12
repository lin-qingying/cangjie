// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

interface Interface16 {}

interface Interface17 {}

interface Interface15 <: Interface16 & Interface17 {}

class Class824 <: Interface16 & Interface17 {}

class Class12 <: Interface15 & Interface17 {}

// Class12 <: Interface16 & Interface17 {}

main() {
    let a = if (true) {
        Class824()
    } else {
        Class12()
    }
    0
}

/* SCAN
error: types Class-Class824 and Class-Class12 of the two branches of this 'if' expression mismatch
  ==> join_ordered_problem_1.cj:22:13:
   | 
22 |     let a = if (true) {
   |             ^ 
   | 
   # note: Traces:
The types 'Class-Class12' and 'Class-Class824' do not have the smallest common supertype


1 error generated, 1 error printed.
*/
