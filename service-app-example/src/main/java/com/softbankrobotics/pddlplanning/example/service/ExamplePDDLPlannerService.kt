package com.softbankrobotics.pddlplanning.example.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.softbankrobotics.pddlplanning.IPDDLPlannerService
import com.softbankrobotics.pddlplanning.IPDDLPlannerService.ACTION_SEARCH_PLANS_FROM_PDDL
import com.softbankrobotics.pddlplanning.PlanSearchFunction
import com.softbankrobotics.pddlplanning.Task
import kotlinx.coroutines.runBlocking

/**
 * This is an example service that provides a PDDL Planner interface.
 */
class ExamplePDDLPlannerService : Service() {

    private val planSearchFunction: PlanSearchFunction = { domain, problem, logFunction ->
        logFunction?.let { it("Searching plan for PDDL:\n$domain\n$problem") }
        val result = listOf(Task.create("hello", "world"))
        logFunction?.let { it("Plan found:\n$result") }
        result
    }

    private val binder = object : IPDDLPlannerService.Stub() {
        override fun searchPlan(domain: String, problem: String): List<Task> {
            return runBlocking { planSearchFunction(domain, problem) { Log.i(TAG, it) } }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        if (intent.action == ACTION_SEARCH_PLANS_FROM_PDDL) {
            Log.i(TAG, "PDDL Planner service is being bound")
            return binder
        } else {
            Log.e(TAG, "Service received unexpected binding intent: ${intent.action}")
            throw RuntimeException("Service received unexpected binding intent: ${intent.action}")
        }
    }

    companion object {
        const val TAG = "ExamplePDDLPlannerService"
    }
}
