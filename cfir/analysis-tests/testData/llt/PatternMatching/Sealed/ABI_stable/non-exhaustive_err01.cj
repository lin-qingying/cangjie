// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

public open class A {}

abstract sealed class C <: A {}

public class C1 <: C {}

public class C2 <: C {}

public class C3 <: C {}

func f1(obj: C) {
    match (obj) {
        case v1: C1 => 0
        case v2: C2 => 1
        case v3: C3 => 2
    }
}

main() {}

/* SCAN
error: non-exhaustive patterns
  ==> non-exhaustive_err01.cj:20:12:
   | 
20 |     match (obj) {
   |            ^^^ the selector is of type 'Class-C'
   | 
   # note: _ is not covered

1 error generated, 1 error printed.
*/
