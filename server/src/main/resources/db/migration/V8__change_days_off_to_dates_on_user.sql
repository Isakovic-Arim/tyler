ALTER TABLE days_off
DROP
COLUMN day_of_week;

ALTER TABLE days_off
    ADD day_of_week date;