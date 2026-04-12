// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

let x = Type

@Annotation[target: [x]]
public class A {
    const init() {}
}

main() {}

/* SCAN
error: expected 'const' expression
  ==> err_const_arg_00.cj:11:22:
   | 
11 | @Annotation[target: [x]]
   |                      ^ 
   | 

1 error generated, 1 error printed.
*/
