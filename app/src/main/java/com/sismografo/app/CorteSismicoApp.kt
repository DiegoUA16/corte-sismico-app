package com.sismografo.app

import android.app.Application

class CorteSismicoApp : Application() {
    val monitor: MonitorSismico by lazy { MonitorSismico(this) }
}
