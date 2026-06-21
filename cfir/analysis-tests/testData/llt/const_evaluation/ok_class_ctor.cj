


const G = 6.674e-11

class Planet {
    const Planet(let mass: Float64, let radius: Float64) {}

    const func gravity(m: Float64, r: Float64) {
        G * mass * m / r ** 2
    }
}

main() {
    const myMass = 71.0
    const earth = Planet(5.972e24, 6.378e6)
    println(earth.gravity(myMass, earth.radius))
}
