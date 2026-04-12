// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

// Should not have ICE

func ( { #
func ( { #

/* SCAN
error: expected '#' or '"' in raw string, found 'new line character'
  ==> func_0.cj:11:11:
   | 
11 | func ( { #
   |          ~^ expected '#' or '"'
   |          |
   |          because it is raw string prefix
   | 

error: expected '#' or '"' in raw string, found 'new line character'
  ==> func_0.cj:12:11:
   | 
12 | func ( { #
   |          ~^ expected '#' or '"'
   |          |
   |          because it is raw string prefix
   | 

2 errors generated, 2 errors printed.
*/
