// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


package collection

public import std.collection.Map
public import std.collection.HashMap

public class Stream<T> <: Iterator<T>{
    public func toMap<K>(keyMapper: (T)->Option<K>): Map<K,T> where K <: Hashable & Equatable<K>{
        toMap<K,T>({<!parse_expected_double_arrow_in_lambda!>HashMap<!><K,T>()},keyMapper,{t=>t})
    }
    public func toMap<K>(initial: ()->Map<K,T>, keyMapper: (T)->Option<K>): Map<K,T> where K <: Equatable<K>{
        toMap<K,T>(initial,keyMapper,{t=>t})
    }
    public func toMap<K,V>(keyMapper: (T)->Option<K>, valueMapper: (T)->Option<V>): Map<K,V> where K <: Hashable & Equatable<K>{
        toMap({<!parse_expected_double_arrow_in_lambda!>HashMap<!><K,V>()},keyMapper, valueMapper)
    }
    public func toMap<K,V>(initial: ()->Map<K,V>, keyMapper: (T)->Option<K>, valueMapper: (T)->Option<V>): Map<K,V> where K <: Equatable<K>{
        collect<Map<K,V>>(initial) {t, m =>
            match((keyMapper(t),valueMapper(t))){
                case (Some(k),Some(v)) => m.add(k,v)
                case _ => None<V>
            }
            return m
        }
    }

}

extend<K,V> HashMap<K,V> <: ExtendHashMap<K,V> where K <: Hashable & Equatable<K>{
    public func stream(): Stream<(K,V)>{
        Stream<(K,V)>(this)
    }
}
