-- V1 baseline: common column conventions for learnKK (런크크).
--
-- Conventions applied to every table:
--   * id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY  (except natural-key tables)
--   * created_at  timestamptz NOT NULL DEFAULT now()
--   * updated_at  timestamptz                                      (nullable; set on update)
--   * enum-typed columns are stored as varchar guarded by a CHECK constraint (contract #3)
--   * physical naming is snake_case (Hibernate CamelCaseToUnderscoresNamingStrategy)
--
-- This migration intentionally creates no tables; it documents the shared schema contract and
-- anchors the Flyway baseline. Domain tables follow in V2 (auth) and V3 (meeting).

SELECT 1;
