(define (domain service_domain)

    (:requirements :adl :negative-preconditions :universal-preconditions)

    (:types
        human
    )

    (:predicates
        (interacting_with ?h - human); Agent is interacting with the robot (mostly engaged, close to the robot).
        (is_leaving ?h - human); User is leaving
        (was_greeted ?h - human); Target has been greeted by the robot.
        (was_goodbyed ?h - human); Target has been goodbyed by the robot.
        (was_checked_in ?h - human); Target has been checked in by the robot.
    )

    (:action greet
        :parameters (?h - human)
        :precondition (interacting_with ?h)
        :effect (was_greeted ?h)
    )

    (:action goodbye
        :parameters (?h - human)
        :precondition (was_greeted ?h); KLUDGE: unwanted extra constraint because precondition cannot be empty.
        :effect (was_goodbyed ?h)
    )

    (:action check_in ; hypothesis: menu could be created automatically
        :parameters (?h - human)
        :precondition (not (was_checked_in ?h))
        :effect (was_checked_in ?h)
    )
)

(define (problem service_problem)
    (:domain service_domain)

    (:requirements :adl :negative-preconditions :universal-preconditions)

    (:objects
        user - human;  Simple case: there is only one human, the user, and we reason around them
    )

    (:init

        ; Users come and go. The robot automatically track, attract and engage them.
        ; Users may be interacting with the robot if they engage the robot back.
        ; They are therefore necessarily colocated.
        ; The robot must check people in, and for this reason,
        ; find out whether the user is an expected guest
        ; (using QR code or e-mail address)
        ; or an unexpected one.
        ; Potentially it could be an employee, or a guest that already checked in, but let's not see that now.
        ; For now, when the user leaves, it is completely forgotten.
        ; People may also want some info on the company, while waiting for the appointment.

        ; The robot is idling.
    )

    (:goal
        (and
            (imply (interacting_with user) (was_greeted user)); Users interacting must be greeted.
            (imply
                (and (was_greeted user) (not (is_leaving user))); Given they are greeted, and are not leaving,
                (was_checked_in user));  we check them in.
            (imply (is_leaving user) (was_goodbyed user)); A leaving user should be goodbyed.
        )
    )
)
