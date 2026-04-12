// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    try {} catch (_: Nothing) {}
}

/* SCAN
error: the exception catch type must be class and extends from core.Exception or core.Error
  ==> err_catch_00.cj:10:22:
   | 
10 |     try {} catch (_: Nothing) {}
   |                      ^ 
   | 

1 error generated, 1 error printed.
*/
