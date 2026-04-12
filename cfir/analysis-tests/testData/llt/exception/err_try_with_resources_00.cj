// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    try (x = return) {}
}

/* SCAN
error: mismatched types
  ==> err_try_with_resources_00.cj:10:10:
   | 
10 |     try (x = return) {}
   |          ^^^^^^^^^^ expected 'Interface-Resource', found 'Nothing'
   | 
   # note: the resource specification should implement interface 'Resource'

1 error generated, 1 error printed.
*/
