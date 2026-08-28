-- 삭제된 논문은 휴지통 기록으로 보관하되, 동일 PDF를 새 논문으로 다시 등록할 수 있게 한다.
-- 중복 검사는 애플리케이션에서 삭제되지 않은 논문만 대상으로 수행한다.
ALTER TABLE paper_files DROP CONSTRAINT ux_paper_files_sha256;

CREATE INDEX ix_paper_files_sha256 ON paper_files (sha256);
