package com.octfis.crm

import android.app.Application
import com.octfis.crm.data.remote.ZohoServiceLocator
import com.octfis.crm.service.CallMonitorService

class OctfisApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ZohoServiceLocator.init(this)
        CallMonitorService.start(this)
    }
}