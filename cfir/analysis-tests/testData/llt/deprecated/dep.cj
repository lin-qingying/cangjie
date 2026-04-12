// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f

func foo(@Deprecated[""] a!: Int64) {
    a
}

main() {
    foo(a: 10)
}

/* SCAN
error: Parameter without default value can not be target of @Deprecated
 ==> dep.cj:9:10:
  | 
9 | func foo(@Deprecated[""] a!: Int64) {
  |          ^^^^^^^^^^^^^^^ 
  | 

error: argument 'message' of @Deprecated must not be empty string
 ==> dep.cj:9:22:
  | 
9 | func foo(@Deprecated[""] a!: Int64) {
  |                      ^^ 
  | 

2 errors generated, 2 errors printed.
*/
