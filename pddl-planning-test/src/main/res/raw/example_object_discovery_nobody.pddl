(define (domain my_domain)
   (:requirements :adl)

   (:types human emotion)

   (:constants neutral happy sad - emotion)

   (:predicates
      (feels ?h - human ?e - emotion)
      (is_around ?h - human)
      (not_found ?h - human))

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
      someone - human
   )

   (:init)

   (:goal
      (and
         (forall (?h - human)
            (imply (is_around ?h)
                (feels ?h happy)))
         (imply (not (exists(?h - human) (is_around ?h)))
            (is_around someone)))))
