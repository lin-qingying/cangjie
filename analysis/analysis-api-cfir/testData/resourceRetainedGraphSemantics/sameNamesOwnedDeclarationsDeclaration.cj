package test.resourceretained.samenames

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
            let /*@first_decl*/first = { /*@first_value*/value: Int64 =>
                func /*@first_step_decl*/step(/*@first_step_value*/value: Int64): Int64 { /*@first_step_value_ref*/value }
                /*@first_step_call*/step(/*@first_value_ref*/value) + /*@first_capture_left*/left
            }
            let /*@second_decl*/second = { /*@second_value*/value: Int64 =>
                func /*@second_step_decl*/step(/*@second_step_value*/value: Int64): Int64 { /*@second_step_value_ref*/value }
                /*@second_step_call*/step(/*@second_value_ref*/value) + /*@second_capture_right*/right
            }
            /*@box_call*/Box2(/*@first_call*/first(/*@first_outer_left*/left), /*@second_call*/second(/*@second_outer_right*/right))
        }), /*@resource_a*/a = /*@ordered_b_ref*/b) {
            acceptInt(/*@body_a_ref*/a.get())
        }
        /*@outer_return_left*/left
    }
    let /*@result_decl*/result: Int64 = /*@operation_call*/operation(1, 2)
    println(result)
}
