package test.resourceretained.exceptional

var trace: Int64 = 0
func record(value: Int64): Unit { trace = trace * 10 + value }

class Box2<T> <: Resource {
    let left: T
    let right: T
    let label: Int64
    var closed: Bool = false
    public init(left: T, right: T, label: Int64) {
        this.left = left
        this.right = right
        this.label = label
    }
    public func get(): T { left }
    public func isClosed(): Bool { closed }
    public func close(): Unit { closed = true; record(label) }
}

func run<T>(body: () -> T): T { body() }
func acceptInt(value: Int64): Unit { println(value) }

main() {
    let /*@operation_decl*/operation = { /*@outer_left*/left, /*@outer_right*/right =>
        try (/*@resource_b*/b = /*@initializer_run*/run({ =>
            /*@nested_try*/try (/*@resource_inner*/inner = /*@nested_box_call*/Box2(/*@nested_left*/left, /*@nested_right*/right, 3)) {
                if (/*@throw_condition_left*/left < 0) { /*@throw_expr*/throw Exception("negative") }
                acceptInt(/*@body_inner_ref*/inner.get())
                /*@normal_record*/record(1)
            } catch (/*@catch_e*/e: Exception) {
                let /*@caught_decl*/caught = { => /*@catch_capture_e*/e.message }
                if (/*@caught_call*/caught() == "negative") { /*@catch_record*/record(4) } else { record(8) }
            } finally {
                /*@finally_record*/record(9)
            }
            /*@box_call*/Box2(/*@final_left*/left, /*@final_right*/right, 2)
        }), /*@resource_a*/a = /*@ordered_b_ref*/b) {
            acceptInt(/*@body_a_ref*/a.get())
            /*@outer_body_record*/record(5)
        }
        /*@outer_return_left*/left
    }
    let /*@normal_result_decl*/normal: Int64 = /*@normal_operation_call*/operation(1, 2)
    println(trace)
    trace = 0
    let /*@recovered_result_decl*/recovered: Int64 = /*@recovered_operation_call*/operation(-1, 2)
    println(trace)
    println(normal + recovered)
}
