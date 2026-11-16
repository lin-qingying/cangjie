package sample

struct Config {
    const init(value!: Int64, label!: Int64) {}
}

func a(repeat!: Int64) {}

func useConfig() {
    Config(value: 1, label: 2)
    a(repeat: 0)
}
