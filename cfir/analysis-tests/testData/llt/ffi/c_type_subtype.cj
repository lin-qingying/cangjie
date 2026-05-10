// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %diag_json %f -o %output 2>&1 | compare %f

main() {
    <!UNABLE_TO_INFER_GENERIC_FUNC!>CPointerHandle()<!>
    <!GENERIC_ARGUMENT_NO_MATCH!>CPointerHandle<CType>()<!>
    return 0
}
