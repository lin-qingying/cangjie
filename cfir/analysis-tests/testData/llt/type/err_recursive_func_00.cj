// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

let f = {=> g}

func g() {
    f
}

main() {}

/* SCAN
error: unable to infer declaration type, please add type annotation
 ==> err_recursive_func_00.cj:9:5:
  | 
9 | let f = {=> g}
  |     ^ 
  | 

1 error generated, 1 error printed.
*/
