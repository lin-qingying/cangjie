// ERRCHECK: %compiler %cmp_opt %f -o %output

class C {
    let v = f
}

func f() {
    let c = C()
    c.v
}

main() {}

/* SCAN
error: unable to infer declaration type, please add type annotation
 ==> err_recursive_func_02.cj:4:9:
  | 
4 |     let v = f
  |         ^ 
  | 

1 error generated, 1 error printed.
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
