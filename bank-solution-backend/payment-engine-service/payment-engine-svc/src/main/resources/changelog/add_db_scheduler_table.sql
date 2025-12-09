CREATE TABLE IF NOT EXISTS scheduled_tasks(
    task_name text not null,
    task_instance text not null,
    task_data bytea,
    execution_time timestamp with time zone not null,
    picked boolean not null,
    picked_by text,
    last_success timestamp with time zone,
    last_failure timestamp with time zone,
    consecutive_failures int,
    last_heartbeat timestamp with time zone,
    version bigint not null,
    PRIMARY KEY (task_name, task_instance)
);

CREATE INDEX IF NOT EXISTS execution_time_idx ON scheduled_tasks(execution_time);
CREATE INDEX IF NOT EXISTS last_heartbeat_idx ON scheduled_tasks(last_heartbeat);