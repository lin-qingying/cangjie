// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    var a: Float64 = 0.0
    <!TYPE_MISMATCH!>a++<!>
    <!TYPE_MISMATCH!>a--<!>
}
