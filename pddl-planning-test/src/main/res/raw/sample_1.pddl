(define (domain exploration) ; how to comment in pddl

    (:requirements :strips :typing)

    (:types event)

    (:constants
        ask_event - event
    )

    (:predicates
        (occurred ?e - event)
    )

    (:action ask
        :parameters ()
        :precondition ()
        :effect (occurred ask_event)
    )
)

(define (problem ask_problem)
    (:domain exploration)
    (:requirements :strips :typing)
    (:objects)
    (:init )
    (:goal (occurred ask_event))
)
