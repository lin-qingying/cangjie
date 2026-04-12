// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

@Annotation
public class A { const init(_: Int64) {} }

@A[{ =>
    @A[0] let x = 0
    x
}()]
class B {}

main() {}

/* SCAN
error: cannot use custom annotation
  ==> err_custom_annotation_place_var_02.cj:13:15:
   | 
13 |     @A[0] let x = 0
   |     ~~~~~     ^ 
   | 
   # note: custom annotations can only be used on non-local declarations

1 error generated, 1 error printed.
*/
