---
--- Migration pour le TP2
---

create table feed_info (
    feed_publisher_name text,
    feed_publisher_url  text,
    feed_lang           text,
    feed_start_date     text,
    feed_end_date       text
);

create index ix_trips_service_id on trips (service_id);

create table calendar_dates (
    service_id     text references trips (service_id),
    date           text,
    exception_type integer
);

create index ix_calendar_dates_service_id on calendar_dates (service_id);
create index ix_calendar_dates_date on calendar_dates (date);

drop index ix_stop_times_departure_time;

create index ix_stop_times_trip_id on stop_times (trip_id);
create index ix_stop_times_stop_id on stop_times (stop_id);
