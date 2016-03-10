---
--- k meilleurs trajets entre deux points
---

select *,

    -- concaténation des paires de coordonnées pour la forme à dessiner sur la carte
    (select group_concat(shape_pt_lat || ',' || shape_pt_lon, ';') from shapes
        where A_prime_B_prime_trips.shape_id = shapes.shape_id
        group by shape_id
        order by shape_pt_sequence)

from stops as A_prime -- arrêts à A'

join stop_times as A_prime_times on A_prime.stop_id = A_prime_times.stop_id -- temps aux arrêtes de A'

join trips as A_prime_B_prime_trips on A_prime_times.trip_id = A_prime_B_prime_trips.trip_id -- chemins de A' à B'

join stops as B_prime -- arrêtes à B'

join stop_times as B_prime_times on B_prime.stop_id = B_prime_times.stop_id -- temps aux arrêtes à B'
    and A_prime_times.trip_id = B_prime_times.trip_id -- chemin de A' à B'
    and A_prime_times.stop_sequence < B_prime_times.stop_sequence -- A' est avant B'

where

-- marche jusqu'à l'arrêt (5 km/h)
time(A_prime_times.arrival_time) >= time(strftime('%s', 'now', 'localtime') + 1.37 * 51883.246273604 * (abs(A_prime.stop_lat - :A_lat) + abs(A_prime.stop_lon - :A_lon)), 'unixepoch') -- d(A) < d(A')

-- filtre les points A' et B'
and 51883.246273604 * (abs(A_prime.stop_lat - :A_lat) + abs(A_prime.stop_lon - :A_lon)) <= :max_walking_distance
and 51883.246273604 * (abs(:B_lat - B_prime.stop_lat) + abs(:B_lon - B_prime.stop_lon)) <= :max_walking_distance

-- distance maximale de marche
and 51883.246273604 * (abs(A_prime.stop_lat - :A_lat) + abs(A_prime.stop_lon - :A_lon) + abs(:B_lat - B_prime.stop_lat) + abs(:B_lon - B_prime.stop_lon)) <= :max_walking_distance -- d(A, A') + d(B', B)

-- minimise le temps d'arrivée t(B') + 1.37 * d(B', B)
order by time(B_prime_times.arrival_time) + 1.37 * 51883.246273604 * (abs (:B_lat - B_prime.stop_lat) + abs(:B_lon - B_prime.stop_lon)) -- minimise t(B') + t(B)

limit :k;

