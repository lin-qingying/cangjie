// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func A() {
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>i<!> => i}
    {<!LAMBDA_MUST_HAVE_TYPE_ANNOTATION!>i<!>, j => i + j}
}

main() {
    return 0
}
