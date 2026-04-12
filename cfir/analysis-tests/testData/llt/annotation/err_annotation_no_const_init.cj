// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

@Annotation
public class A {}

main() {}

/* SCAN
error: class with '@Annotation' should have 'const' constructor
  ==> err_annotation_no_const_init.cj:10:14:
   | 
10 | public class A {}
   |              ^ 
   | 

1 error generated, 1 error printed.
*/
