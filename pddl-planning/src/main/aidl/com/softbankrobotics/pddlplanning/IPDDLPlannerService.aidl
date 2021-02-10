package com.softbankrobotics.pddlplanning;

import com.softbankrobotics.pddlplanning.Task;

/**
 * Interface to bind to your service to expose PDDL planning functions.
 */
interface IPDDLPlannerService {
    /**
     * Recommended intent action to trigger a PDDL planner service.
     */
    const String ACTION_SEARCH_PLANS_FROM_PDDL = "com.softbankrobotics.planning.action.SEARCH_PLANS_FROM_PDDL";

    /**
     * Searches for a plan (a list of tasks) for the given PDDL domain and problem.
     */
    List<Task> searchPlan(String domain, String problem);
}
