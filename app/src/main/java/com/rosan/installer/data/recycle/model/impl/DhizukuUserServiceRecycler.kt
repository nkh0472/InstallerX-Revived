package com.rosan.installer.data.recycle.model.impl

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.annotation.Keep
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuUserServiceArgs
import com.rosan.installer.IDhizukuUserService
import com.rosan.installer.IPrivilegedService
import com.rosan.installer.data.recycle.model.entity.DhizukuPrivilegedService
import com.rosan.installer.data.recycle.repo.Recycler
import com.rosan.installer.data.recycle.repo.recyclable.UserService
import com.rosan.installer.data.recycle.util.requireDhizukuPermissionGranted
import com.rosan.installer.di.init.processModules
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

object DhizukuUserServiceRecycler : Recycler<DhizukuUserServiceRecycler.UserServiceProxy>(),
    KoinComponent {
    class UserServiceProxy(
        private val connection: ServiceConnection,
        val service: IDhizukuUserService
    ) : UserService {
        override val privileged: IPrivilegedService = service.privilegedService

        override fun close() {
            Dhizuku.unbindUserService(connection)
        }
    }

    @SuppressLint("LogNotTimber")
    class DhizukuUserService @Keep constructor(context: Context) : IDhizukuUserService.Stub() {
        init {
            // 在远程服务进程中，必须首先初始化 Dhizuku API。
            // 否则会导致AssertionError
            if (Dhizuku.init(context)) {
                // 在此处记录日志，因为初始化失败将导致所有后续操作失败。
                // 也可以选择抛出一个异常来提前终止。
                Log.d("DhizukuUserService", "Dhizuku.init() succeeded in remote process!")
            } else {
                Log.e("DhizukuUserService", "CRITICAL: Dhizuku.init() failed in remote process!")
            }
            Log.d(
                "DhizukuUserService",
                "Dhizuku.mOwnerComponent: ${Dhizuku.getOwnerComponent()}"
            )
            startKoin {
                modules(processModules)
                androidContext(context)
            }
        }

        private val privileged = DhizukuPrivilegedService()

        override fun getPrivilegedService(): IPrivilegedService = privileged
    }

    private val context by inject<Context>()

    override fun onMake(): UserServiceProxy = runBlocking {
        requireDhizukuPermissionGranted {
            onInnerMake()
        }
    }

    private suspend fun onInnerMake(): UserServiceProxy = callbackFlow {
        val connection = object : ServiceConnection {
            @SuppressLint("LogNotTimber")
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                if (service == null) {
                    // 记录一个明确的错误，并通知调用者连接失败
                    Log.e(
                        "onServiceConnected",
                        "Failed to connect to DhizukuUserService because the remote service failed to start."
                    )
                    close(IllegalStateException("Remote service connection failed."))
                    return
                }
                trySend(UserServiceProxy(this, IDhizukuUserService.Stub.asInterface(service)))
                service.linkToDeath({
                    if (entity?.service == service) recycleForcibly()
                }, 0)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                close()
            }
        }
        Dhizuku.bindUserService(
            DhizukuUserServiceArgs(
                ComponentName(
                    context, DhizukuUserService::class.java
                )
            ), connection
        )
        awaitClose { }
    }.first()
}