// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg
// ERRCHECK: %compiler %noColor %pkg_opt pkg -o %output

/* SCAN
error: undeclared type name 'example_XX'
  ==> pkg/main.cj:11:28:
   | 
11 |     public const init(p0!: example_XX = example_XX.ALL) {}
   |                            ^ 
   | 

error: undeclared identifier 'example_XX'
  ==> pkg/main.cj:11:41:
   | 
11 |     public const init(p0!: example_XX = example_XX.ALL) {}
   |                                         ^ 
   | 

2 errors generated, 2 errors printed.
*/
