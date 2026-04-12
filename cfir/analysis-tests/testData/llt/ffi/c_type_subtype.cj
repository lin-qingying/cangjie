// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %diag_json %f -o %output 2>&1 | compare %f

main() {
    CPointerHandle()
    CPointerHandle<CType>()
    return 0
}

// ASSERT: scan-1 sema_generic_type_argument_not_match_constraint
// ASSERT: scan-1 sema_unable_to_infer_generic_func
// ASSERT: scan "Errors": 2
