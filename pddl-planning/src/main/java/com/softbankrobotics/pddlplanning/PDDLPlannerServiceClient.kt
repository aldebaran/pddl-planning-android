package com.softbankrobotics.pddlplanning

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import com.softbankrobotics.pddlplanning.IPDDLPlannerService.ACTION_SEARCH_PLANS_FROM_PDDL
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val INTENT_SEARCH_PLANS_FROM_PDDL by lazy { Intent(ACTION_SEARCH_PLANS_FROM_PDDL) }

/**
 * Create a PlanSearchFunction implemented by a service identified with the given intent.
 * Note that to target a specific planner service, the caller should specify intent.package.
 * @param context An Android context.
 * @param intent The intent identifying the service which will provide the plan search implementation.
 */
fun createPlanSearchFunctionFromService(
    context: Context,
    intent: Intent = INTENT_SEARCH_PLANS_FROM_PDDL
): PlanSearchFunction {
    val bindingPlannerService = bindPlannerServiceAsync(context, intent)
    return { domain: String, problem: String, log: LogFunction? ->
        val plannerService = bindingPlannerService.await()
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

fun bindPlannerServiceAsync(
    context: Context,
    intent: Intent = INTENT_SEARCH_PLANS_FROM_PDDL
): Deferred<IPDDLPlannerService> {
    // Used to avoid concurrent access internally.
    val lock = Any()

    // The current (or the next) planner service.
    var nextPlannerService: CompletableDeferred<IPDDLPlannerService> = CompletableDeferred()

    // Starts the connection to the service.
    @UiThread
    fun connectToServiceAsync(serviceConnection: ServiceConnection) = synchronized(lock) {
        nextPlannerService = CompletableDeferred()
        val foundService = context.bindService(
            intent, serviceConnection, Context.BIND_AUTO_CREATE
        )
        if (!foundService) {
            context.unbindService(serviceConnection)
            nextPlannerService.completeExceptionally(RemoteException("Planner service was not found"))
        }
        nextPlannerService
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
            connectToServiceAsync(this)
        }
    }

    return connectToServiceAsync(plannerServiceConnection)
}

/**
 * Searches for a solution plan for the given PDDL domain and problem.
 * @throws PDDLTranslationException when the parsing or analysis of the PDDL failed.
 * @throws PDDLPlanningException when the planning failed.
 * @return A list of task that solves the planning problem.
 */
typealias PermissionCheckFunction = () -> Deferred<Unit>

/**
 * The recommended permission to use for services providing plan search functions.
 */
const val SEARCH_PLANS_PERMISSION = "com.softbankrobotics.planning.SEARCH_PLANS"

/**
 * Create a PlanSearchFunction implemented by a service identified with the given intent.
 * Note that to target a specific planner service, the caller should specify intent.package.
 * @param context An Android context.
 * @param intent The intent identifying the service which will provide the plan search implementation.
 */
suspend fun createPlanSearchFunctionFromService(
    checkPermission: PermissionCheckFunction,
    context: Context,
    intent: Intent,
): PlanSearchFunction {
    // Check permission
    withContext(Dispatchers.Main) { checkPermission() }.await()

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

/**
 * In recent Android versions, every exported service must be associated to a permission.
 * Clients must check and grant the right permission before binding the planner service.
 * The permission checker function is a helper to do exactly this.
 * Because it relies on an Android Activity LifecycleOwner,
 * the function has to be setup before the said activity is started.
 */
@UiThread
fun createPlannerPermissionChecker(
    activity: AppCompatActivity,
    packageName: String,
    permission: String = SEARCH_PLANS_PERMISSION
): PermissionCheckFunction {
    // Used to avoid concurrent access internally.
    val lock = Any()

    // Every time the permission checker function is called, the completable is reset.
    var permissionGrant: CompletableDeferred<Unit> = CompletableDeferred()

    // When the permission request has been completed, the current completable is completed.
    val requestPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            synchronized(lock) {
                permissionGrant.complete(Unit)
            }
        } else {
            synchronized(lock) {
                permissionGrant.completeExceptionally(RuntimeException("User denied"))
            }
        }
    }

    return {
        // Let us work on a new completable.
        synchronized(lock) { permissionGrant = CompletableDeferred() }

        when {
            // If the permission is already granted, just go on.
            ContextCompat.checkSelfPermission(
                activity,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                permissionGrant.complete(Unit)
            }

            // If permission was not granted yet and explanations are needed, proceed before requesting.
            shouldShowRequestPermissionRationale(activity, SEARCH_PLANS_PERMISSION) -> {
                val packageManager = activity.packageManager
                val plannerAppInfo = packageManager.getApplicationInfo(packageName, 0)
                val plannerAppName = packageManager.getApplicationLabel(plannerAppInfo)
                AlertDialog.Builder(activity)
                    .setMessage(
                        "This application needs the permission to use " +
                                "the planner provided by the application $plannerAppName to function. " +
                                "No personal information is transmitted in this process."
                    )
                    .setPositiveButton("Continue") { _, _ ->
                        requestPermissionLauncher.launch(SEARCH_PLANS_PERMISSION)
                    }
                    .setNegativeButton("Abort") { _, _ ->
                        permissionGrant.completeExceptionally(RuntimeException("User aborted"))
                    }
                    .show()
            }

            // Else let us request the permission directly.
            else -> {
                requestPermissionLauncher.launch(SEARCH_PLANS_PERMISSION)
            }
        }

        permissionGrant
    }
}