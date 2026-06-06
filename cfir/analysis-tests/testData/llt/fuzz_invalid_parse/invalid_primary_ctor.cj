// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

// Was ICE, should quit normally
struct LA{
  public LA(var <!parse_expected_name!>,<!><!parse_expected_name!>:<!> Int32<!parse_missing_body!>)<!> {}
}

main() {}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
