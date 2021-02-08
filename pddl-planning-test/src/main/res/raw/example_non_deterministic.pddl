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
        :effect (feels ?h happy))

   (:action find_human
      :parameters (?h - human)
      :precondition ()
      :effect (is_around ?h)))

(define (problem my_problem)
   (:domain my_domain)
   (:requirements :adl)

   (:objects
      alice bob - human
      neutral happy sad - emotion)

   (:init
      (feels alice neutral)
      (feels bob sad))

   (:goal
      (forall (?h - human)
         (feels ?h happy))))
