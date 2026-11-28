package test.resourceretained.nested

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
        try (/*@resource_b*/b = /*@initializer_run*/run({ =>
            /*@nested_try*/try (/*@resource_p*/p = /*@nested_box_call*/Box2(/*@nested_left*/left, /*@nested_right*/right), /*@resource_q*/q = /*@nested_p_ref*/p) {
                let /*@mapper_decl*/mapper = { /*@mapper_value*/value: Int64 => value + /*@mapper_capture_p*/p.get() }
                acceptInt(/*@mapper_call*/mapper(/*@body_q_ref*/q.get()))
            }
            /*@box_call*/Box2(/*@final_left*/left, /*@final_right*/right)
        }), /*@resource_a*/a = /*@ordered_b_ref*/b) {
            acceptInt(/*@body_a_ref*/a.get())
        }
        /*@outer_return_left*/left
    }
    let /*@result_decl*/result: Int64 = /*@operation_call*/operation(1, 2)
    println(result)
}
