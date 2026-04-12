// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %import-cangjie-stdx %cmp_opt %f -o %output
// EXEC: %set_stdx_path %run %run_opt %output %run_args
// ASSERT: scan Some(abc)

internal import stdx.encoding.json.stream.*
internal import std.io.ByteBuffer

class A<T> {
    var x: ?T = None
    init (x: ??T) {
        match (x) {
            case Some(val) => this.x = val
            case _ => ()
        }
    }
}

extend<T> A<T> <: JsonDeserializable<A<T>> where T <: JsonDeserializable<T> {
    public static func fromJson(r: JsonReader): A<T> {
        var x: ??T = None
        while (let Some(v) <- r.peek()) {
            match (v) {
                case JsonToken.BeginObject =>
                    r.startObject()
                    while (r.peek() != JsonToken.EndObject) {
                        let n = r.readName()
                        match (n) {
                            case "x" => x = r.readValue<?T>()
                            case _ => r.skip()
                        }
                    }
                    r.endObject()
                    break
                case _ => throw Exception("Invalid stream")
            }
        }
        A(x)
    }
}

main() {
    let data = "{\"x\": \"abc\"}"
    let stream = ByteBuffer()
    stream.write(data.toArray())
    let reader = JsonReader(stream)
    println(A<String>.fromJson(reader).x)
}
