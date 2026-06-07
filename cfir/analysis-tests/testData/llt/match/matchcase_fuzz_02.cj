// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    match (1) {
        case a<!parse_expected_double_arrow_in_case!>@<!>b@@c => 1
    }
    return 0
}
