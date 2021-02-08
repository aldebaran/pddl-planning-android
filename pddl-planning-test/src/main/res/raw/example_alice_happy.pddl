(define (domain my_domain)
   (:requirements :adl)

   (:types human emotion)

   (:constants neutral happy sad - emotion)

   (:predicates
      (feels ?h - human ?e - emotion)
      (is_around ?h - human))

   (:action joke_with
        :parameters (?h - human)
        :precondition (is_around ?h)
        :effect (feels ?h happy)))

(define (problem my_problem)
   (:domain my_domain)
   (:requirements :adl)

   (:objects
      alice bob - human
      neutral happy sad - emotion)

   (:init
      (is_around alice) (feels alice happy)
      (is_around bob) (feels bob sad))

   (:goal
      (forall (?h - human)
         (feels ?h happy))))
