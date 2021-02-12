package com.softbankrobotics.pddlplanning

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import com.softbankrobotics.pddlplanning.IPDDLPlannerService.ACTION_SEARCH_PLANS_FROM_PDDL
import kotlinx.coroutines.CompletableDeferred

val INTENT_SEACH_PLANS_FROM_PDDL by lazy { Intent(ACTION_SEARCH_PLANS_FROM_PDDL) }

/**
 * Create a PlanSearchFunction implemented by a service identified with the given intent.
 * Note that to target a specific planner service, the caller should specify intent.package.
 * @param context An Android context.
 * @param intent The intent identifying the service which will provide the plan search implementation.
 */
suspend fun createPlanSearchFunctionFromService(
    context: Context,
    intent: Intent = INTENT_SEACH_PLANS_FROM_PDDL
): PlanSearchFunction {

    // Used to avoid concurrent access internally.
    val lock = Any()

    // The current (or the next) planner service.
    var nextPlannerService: CompletableDeferred<IPDDLPlannerService> = CompletableDeferred()

    // Starts the connection to the service.
    fun connectToService(serviceConnection: ServiceConnection) = synchronized(lock) {
        nextPlannerService = CompletableDeferred()
        val foundService = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        if (!foundService) {
            nextPlannerService.completeExceptionally(
                RemoteException("Planner service was not found")
            )
        }
    }

    // Or service connection.
    val plannerServiceConnection = object : ServiceConnection {

        // Called when the connection with the service is established
        override fun onServiceConnected(className: ComponentName, service: IBinder): Unit =
            synchronized(lock) {
                val newPlannerService = IPDDLPlannerService.Stub.asInterface(service)
                nextPlannerService.complete(newPlannerService)
            }

        // Called when the connection with the service disconnects unexpectedly
        override fun onServiceDisconnected(className: ComponentName): Unit = synchronized(lock) {
            connectToService(this)
        }
    }

    connectToService(plannerServiceConnection)
    suspend fun waitForPlannerService(): IPDDLPlannerService {
        return synchronized(lock) { nextPlannerService }.await()
    }

    return { domain: String, problem: String, log: LogFunction? ->
        val plannerService = waitForPlannerService()
        if (log != null)
            log("Planner service is ready, searching for a plan...")
        val start = System.nanoTime()
        try {
            plannerService.searchPlan(domain, problem)
        } catch (e: IllegalArgumentException) {
            throw PDDLTranslationException(e.message ?: "unknown error in input PDDL")
        } catch (e: UnsupportedOperationException) {
            throw PDDLPlanningException(e.message ?: "unknown error in PDDL planning")
        } finally {
            if (log != null) {
                val durationMs = (System.nanoTime() - start) / 1_000_000
                log("Plan search took $durationMs ms")
            }
        }
    }
}