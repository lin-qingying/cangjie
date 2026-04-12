// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %n.cj -o %output
// ASSERT: scan-4 expected decimal digit
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
// ASSERT: regex-not Segementation

main() {
    var a = 1ef64
    var b = 1Ef64
}