-- Support asked for the reason to show in the admin tool (cancel moved from DELETE to POST .../cancel).
alter table orders add column cancellation_reason varchar(500);
alter table orders add column cancelled_at timestamp with time zone;
