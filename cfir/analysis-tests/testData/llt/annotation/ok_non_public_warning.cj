// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// EXEC: %run %run_opt %output %run_args

@Annotation
class A {
    const init() {}
}

@A
class B {}

main() {}

/* SCAN
warning: '@Annotation' modifying non-'public' class is invisible at runtime
  ==> ok_non_public_warning.cj:11:7:
   | 
11 | class A {
   |       ^ 
   | 

1 warning generated, 1 warning printed.
*/
