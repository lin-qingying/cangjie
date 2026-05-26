// FILE: helper.cj
package peers.single

func helper(): Int64 {
    return 1
}

// FILE: unrelated.cj
package peers.single

func unrelated(): Int64 {
    return 2
}

// FILE: main.cj
package peers.single

func compute(): Int64 {
    return helper()
}
