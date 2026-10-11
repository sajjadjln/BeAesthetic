package app.beaesthetic

import android.app.Application
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import app.beaesthetic.reminders.ReconcileWorker
import kotlinx.coroutines.launch

class App : Application() {

    lateinit var container: AppContainer
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(PersianLocale.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifier.createChannels()
        ReconcileWorker.enqueue(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(OpenObserver())
    }

    /** Each time the app comes to the foreground: note it, then plan again (the absence rule reads this). */
    private inner class OpenObserver : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            container.scope.launch {
                container.ensureReady()
                container.db.settings().setLastOpenedAt(container.clock.now())
                container.events.appOpen()
                container.events.prune()
                container.scheduler.reconcile("app-open")
            }
        }
    }
}
