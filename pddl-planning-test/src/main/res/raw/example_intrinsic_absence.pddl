(define (domain my_domain)
   (:requirements :adl)

   (:types human emotion)

   (:constants neutral happy sad - emotion)

   (:predicates
      (feels ?h - human ?e - emotion))

   (:action joke_with
        :parameters (?h - human)
        :precondition ()
        :effect (feels ?h happy)))

(define (problem my_problem)
   (:domain my_domain)
   (:requirements :adl)

   (:objects
      alice - human
   )

   (:init
      (feels alice neutral))

   (:goal
      (forall (?h - human)
         (feels ?h happy))))
