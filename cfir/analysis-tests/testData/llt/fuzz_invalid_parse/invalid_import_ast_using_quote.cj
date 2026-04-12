// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %diag_json %f 2>&1 | compare %f

// Was ICE, should quit normally
import ast..

public macro test(input:Tokens):Tokens{
    quote(.)
}

main() {}

// ASSERT: scan parse_expected_character


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
