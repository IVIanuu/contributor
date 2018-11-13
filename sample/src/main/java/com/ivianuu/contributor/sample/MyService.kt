package com.ivianuu.contributor.sample

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * @author Manuel Wrage (IVIanuu)
 */
class MyService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}