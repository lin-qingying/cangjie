// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

enum A<T> {
    A1(T)
}

enum E {
    | E0
    | E1(Int64, (A, Bool))
}

main() {
    let x = E0
    match (x) {
        case E0 => 0
    }
}

/* SCAN
error: generic type should be used with type argument
  ==> enum16.cj:15:18:
   | 
15 |     | E1(Int64, (A, Bool))
   |                  ^ 
   | 

error: non-exhaustive patterns
  ==> enum16.cj:20:12:
   | 
20 |     match (x) {
   |            ^ the selector is of type 'Enum-E'
   | 
   # note: E1(_, _) is not covered

2 errors generated, 2 errors printed.
*/
