// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f<T>(x: T) {
    match (x) {
        case _: Int64 => 0 // reachable
    }
}

main() {
    f(0)
}

/* SCAN
error: non-exhaustive patterns
  ==> non_exhaustive_generic.cj:10:12:
   | 
10 |     match (x) {
   |            ^ the selector is of type 'Generics-T'
   | 
   # note: _ is not covered

1 error generated, 1 error printed.
*/
