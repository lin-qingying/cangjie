// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

const v = 11

main() {
    @IfAvailable(level:<!IFAVAILABLE_ARG_NOT_LITERAL!>v<!>, { => println(0) }, {=>println(3)})
}
