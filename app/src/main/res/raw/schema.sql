pragma foreign_keys = on;

create table stops (
    stop_id             integer primary key,
    stop_code           integer unique,
    stop_name           text,
    stop_lat            real,
    stop_lon            real,
    stop_url            text,
    wheelchair_boarding integer
);

create index ix_stops_stop_lat on stops (stop_lat);
create index ix_stops_stop_lon on stops (stop_lon);

create table routes (
    route_id         integer primary key,
    agency_id        text,
    route_short_name integer,
    route_long_name  text,
    route_type       integer,
    route_url        text,
    route_color      text,
    route_text_color text
);

create table shapes (
    shape_id          integer,
    shape_pt_lat      real,
    shape_pt_lon      real,
    shape_pt_sequence integer
);

create index ix_shapes_shape_id on shapes (shape_id);

create table trips (
    route_id              integer  references routes (route_id),
    service_id            text,
    trip_id               text     primary key,
    trip_headsign         text,
    direction_id          integer,
    shape_id              integer,
    wheelchair_accessible integer,
    note_fr               text,
    note_en               text
);

create index ix_trips_shape_id on trips (shape_id);

create table stop_times (
    trip_id        text    references trips (trip_id),
    arrival_time   text,
    departure_time text,
    stop_id        integer references stops (stop_id),
    stop_sequence  integer
);

create index ix_stop_times_departure_time on stop_times (departure_time);
create index ix_stop_times_arrival_time   on stop_times (arrival_time);


