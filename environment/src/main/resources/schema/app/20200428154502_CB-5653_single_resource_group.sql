-- // CB-3003 S3Guard DynamoDB table cleanup
-- Migration SQL that makes the change goes here.

ALTER TABLE environment_parameters
    ADD COLUMN IF NOT EXISTS resource_group_name varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE environment_parameters
    DROP COLUMN IF EXISTS resource_group_name;