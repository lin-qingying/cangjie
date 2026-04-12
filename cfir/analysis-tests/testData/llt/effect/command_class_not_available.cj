// ERRCHECK: %compiler %enableEH %cmp_opt %f -o %output %diag_json

class Eff {}

main():Int64 {
    try {

    } handle (c: Eff) {

    }
    return 0
}

// ASSERT: scan sema_command_handle_type_error


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
