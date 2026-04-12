// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package asp.service

import std.reflect.{TypeInfo, ClassTypeInfo}
import std.collection.{collectArrayList, first}

/*
反射工具类
 */
class ReflectUtilities {
    public static func getServiceConstructor(typeInfo: TypeInfo) {
        if (let Some(classInfo) <- (typeInfo as ClassTypeInfo)) {
            let list = classInfo.constructors |> collectArrayList
            list.sortBy {
                a, b => if (a.parameters.size > b.parameters.size) {
                    Ordering.LT
                } else if (a.parameters.size < b.parameters.size) {
                    Ordering.GT
                } else {
                    Ordering.EQ
                }
            }
            return (list |> first).getOrThrow {Exception("No constructor found")}
        }
        throw Exception("The '${typeInfo}' not class type.")
    }
}
