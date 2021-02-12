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
     * Searches for a solution plan for the given PDDL domain and problem.
     * @throws IllegalArgumentException when the parsing or analysis of the PDDL failed.
     * @throws UnsupportedOperationException when the planning failed.
     * @return A list of task that solves the planning problem.
     */
    @nullable List<Task> searchPlan(String domain, String problem);
}
