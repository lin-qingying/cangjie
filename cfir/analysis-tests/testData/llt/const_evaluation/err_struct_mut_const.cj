// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct S {
    mut const func f() { 0 }
    const init() {}
}

main() {
    let s = S()
    s.f()
}

/* SCAN
error: 'const' and 'mut' modifiers conflict on function declaration
  ==> err_struct_mut_const.cj:10:9:
   | 
10 |     mut const func f() { 0 }
   |     ~~~ ^^^^^ ~~~~ on function declaration
   |     |   |
   |     |   unexpected modifier
   |     |
   |     because it is conflicted with this
   | 

1 error generated, 1 error printed.
*/
