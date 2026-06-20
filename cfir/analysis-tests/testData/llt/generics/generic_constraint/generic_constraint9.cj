// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func atest<<!GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY!>T<!>>(param: T) where T <: Array<T> {
    return param
}
