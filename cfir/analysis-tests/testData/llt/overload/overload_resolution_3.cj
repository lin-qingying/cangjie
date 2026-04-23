// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

abstract open class Component {}

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
