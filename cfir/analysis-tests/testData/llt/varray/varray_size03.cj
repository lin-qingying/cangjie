// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    var a = VArray<Int64, $3>(repeat: 0)
    <!NO_MATCHING_OPERATOR_INVOKE!>a<!>.size()
    return 0
}
