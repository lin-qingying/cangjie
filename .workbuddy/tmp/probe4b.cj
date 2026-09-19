struct R1 {
    public var i = 0
    public mut func foo1() {
        let f = { => this }
        func goo() {
            this
        }
        return 0
    }
    public mut func foo2() {
        let f = { => this.i = 2 }
        func goo() {
            this.i = 2
        }
        return 0
    }
    public mut func foo3() {
        let f = { => this.i }
        func goo() {
            this.i
        }
        return 0
    }
    public mut func foo4() {
        let f = { => i }
        func goo() {
            i
        }
        return 0
    }
}
main() { return 1 }
