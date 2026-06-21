

abstract <!REDUNDANT_MODIFIER!>open<!> class Component {}

class Button <: Component {
    Button(let pushed: Bool) {}
}

class Box <: Component {
    init(contentGen: (Bool) -> Array<Component>) {
        content = contentGen(false)
    }
    init(contentGen: (Int) -> Array<Component>) {
        content = contentGen(0)
    }
    init(contents: Array<Component>) {
        content = contents
    }
    let content: Array<Component>
}

main() {
    let b = Box {
        flag => [Button(flag),
            Box {
            flag => [Button(flag),
                Box {
                flag => [Button(flag),
                    Box {
                    flag => [Button(flag),
                        Box {
                        flag => [Button(flag),
                            Box {
                            flag => [Button(flag),
                                Box {
                                flag => [Button(flag),
                                    Box {
                                    flag => [Button(flag),
                                        Box {
                                        flag => [Button(flag),
                                            Box {
                                            flag => [Button(flag),
                                                Box {
                                                flag => [Button(flag),
                                                    Box {
                                                    flag => [Button(flag),
                                                        Box {
                                                        flag => [Button(flag),
                                                            Box {
                                                            flag => [Button(flag),
                                                                Box {
                                                                flag => [Button(flag),
                                                                    Box {
                                                                    flag => [Button(flag),
                                                                        Box {
                                                                        flag => [Button(flag),
                                                                            Box {
                                                                            flag => [Button(flag),
                                                                                Box {
                                                                                flag => [Button(flag),
                                                                                    Box {
                                                                                    flag => [Button(flag),
                                                                                        Box {
                                                                                        flag => [Button(flag),
                                                                                            Box {
                                                                                            flag => [Button(flag),
                                                                                                Box {
                                                                                                flag => [Button(flag),
                                                                                                    Box {
                                                                                                    flag => [Button(
                                                                                                        flag),
                                                                                                        Box {
                                                                                                        flag => [Button(
                                                                                                            flag),
                                                                                                            Box {
                                                                                                            flag => [Button(
                                                                                                                flag),
                                                                                                                Box {
                                                                                                                flag => [Button(
                                                                                                                    flag),
                                                                                                                    Box {
                                                                                                                    flag => [Button(
                                                                                                                        flag),
                                                                                                                        Box {
                                                                                                                        
                                                                                                                    flag => [Button(
                                                                                                                            flag)]
                                                                                                                    }]
                                                                                                                }]
                                                                                                            }]
                                                                                                        }]
                                                                                                    }]
                                                                                                }]
                                                                                            }]
                                                                                        }]
                                                                                    }]
                                                                                }]
                                                                            }]
                                                                        }]
                                                                    }]
                                                                }]
                                                            }]
                                                        }]
                                                    }]
                                                }]
                                            }]
                                        }]
                                    }]
                                }]
                            }]
                        }]
                    }]
                }]
            }]
        }]
    }
    b
    ()
}
