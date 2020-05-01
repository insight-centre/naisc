package org.insightcentre.uld.naisc.rest

class DatasetNotFoundException : Exception {
    constructor() {}

    constructor(s: String) : super(s) {}

    constructor(s: String, throwable: Throwable) : super(s, throwable) {}

    constructor(throwable: Throwable) : super(throwable) {}
}
