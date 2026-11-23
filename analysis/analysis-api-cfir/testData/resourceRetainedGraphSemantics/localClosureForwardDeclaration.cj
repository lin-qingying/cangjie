package test.resourceretained.localclosure

class Box2<T> <: Resource {
    let left: T
    let right: T
    public init(left: T, right: T) { this.left = left; this.right = right }
    public func get(): T { left }
    public func isClosed(): Bool { true }
    public func close(): Unit {}
}

func run<T>(body: () -> T): T { body() }
func acceptInt(value: Int64): Unit { println(value) }
main() {
    let /*@operation_decl*/operation = { /*@outer_left*/left, /*@outer_right*/right =>
        try (/*@resource_a*/a = /*@forward_b*/b, /*@resource_b*/b = /*@initializer_run*/run({ =>
            let /*@mapper_decl*/mapper = { /*@mapper_value*/value: Int64 => value + /*@mapper_capture_left*/left }
            func /*@relay_decl*/relay(/*@relay_value*/value: Int64): Int64 {
                /*@relay_mapper_ref*/mapper(/*@relay_value_ref*/value)
            }
            /*@box_call*/Box2(/*@relay_left_call*/relay(/*@relay_left_ref*/left), /*@relay_right_call*/relay(/*@relay_right_ref*/right))
        })) {
            acceptInt(/*@body_a_ref*/a.get())
        }
        /*@outer_return_left*/left
    }
    let /*@result_decl*/result: Int64 = /*@operation_call*/operation(1, 2)
    println(result)
}
