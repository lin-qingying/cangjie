// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %diag_json %f -o %output

main() {
    let b = synchronized(<!TYPE_MISMATCH!>()<!>) {}
    let c: Bool = synchronized(<!TYPE_MISMATCH!>false<!>) {
        <!TYPE_MISMATCH!>1<!>
    }
    0
}
