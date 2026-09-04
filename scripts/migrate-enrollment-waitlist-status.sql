-- Adds WAITLISTED to the enrollments.status check constraint.
-- Needed because Hibernate ddl-auto=update does not update existing enum check constraints.

ALTER TABLE enrollments
DROP CONSTRAINT IF EXISTS enrollments_status_check;

ALTER TABLE enrollments
ADD CONSTRAINT enrollments_status_check
CHECK (status IN ('PENDING', 'CONFIRMED', 'WAITLISTED', 'CANCELLED', 'COMPLETED'));
