-- ============================================================
-- V2: JPA 매핑 정합성을 위한 컬럼 타입 조정
--   - sha256   : char(64) -> varchar(64)  (bpchar 패딩 문제 + 매핑 일치)
--   - pub_year : smallint -> integer      (Java Integer 매핑 일치)
-- ============================================================

ALTER TABLE paper_files
    ALTER COLUMN sha256 TYPE VARCHAR(64);

ALTER TABLE papers
    ALTER COLUMN pub_year TYPE INTEGER;
