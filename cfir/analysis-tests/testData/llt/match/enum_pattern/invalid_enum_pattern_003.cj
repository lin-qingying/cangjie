// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    let a: Array<UInt8> = Array<UInt8>(1, repeat: 0)
    a[match (0) { case x => 1; case _ => 0 }] *= match (0) { case x =><!parse_case_body_cannot_be_empty!> <!> }
}
